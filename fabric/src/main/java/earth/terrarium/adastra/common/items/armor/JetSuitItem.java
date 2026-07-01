package earth.terrarium.adastra.common.items.armor;

import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.constants.PlanetConstants;
import earth.terrarium.adastra.common.planets.AdAstraData;
import earth.terrarium.adastra.common.utils.UpgradeUtils;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.KeybindManager;
import earth.terrarium.adastra.common.utils.ModUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import net.minecraft.resources.ResourceKey;
import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class JetSuitItem extends SpaceSuitItem implements EnergyProvider.Item {

    private final long energyCapacity;

    public JetSuitItem(ArmorMaterial material, ArmorType type, int tankSize, int energy, Properties properties) {
        super(material, type, tankSize, properties);
        this.energyCapacity = energy;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> consumer, @NotNull TooltipFlag isAdvanced) {
        var fluidContainer = getFluidContainer(stack);
        long fluidAmount = fluidContainer.get(0).getAmount();
        long fluidCapacity = fluidContainer.get(0).getLimit(fluidContainer.get(0).getResource());
        consumer.accept(TooltipUtils.getFluidComponent(
            fluidAmount, fluidCapacity, ModFluids.OXYGEN.get()));
        var energy = getEnergyStorage(stack);
        consumer.accept(TooltipUtils.getEnergyComponent(energy.getStoredAmount(), energyCapacity));
        consumer.accept(TooltipUtils.getMaxEnergyInComponent(energy.getCapacity()));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.JET_SUIT_INFO);
    }

    public SimpleValueStorage getEnergyStorage(ItemStack holder) {
        return EnergyUtils.getItemEnergyStorage(holder, energyCapacity);
    }

    @Override
    public ValueStorage getEnergy(ItemStack stack, ItemContext context) {
        return getEnergyStorage(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) return;
        if (!hasFullJetSuitSet(player)) return;

        // Check atmosphere leave regardless of flight state
        checkAtmosphereLeave(player);

        tickFlight(player, stack);
    }

    /**
     * Core jet-suit flight, restored to the 1.20.1 model. In 1.20.1 Item#inventoryTick ran on
     * BOTH sides, so the client applied its own flight velocity every tick — that's what made
     * the suit responsive. 26.x made inventoryTick server-only, which left flight dependent on
     * a per-tick server velocity sync (rubber-banding). This method is therefore called from
     * both the server inventoryTick above AND AdAstraClient.clientTick for the local player;
     * energy consumption still only happens server-side (consume() no-ops on the client).
     */
    public void tickFlight(Player player, ItemStack stack) {
        if (!hasFullJetSuitSet(player)) return;
        if (player.getAbilities().flying) return;
        if (player.isPassenger()) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;
        if (!KeybindManager.suitFlightEnabled(player)) return;

        // Space brake: in space, hold sneak while *touching a block* (ground / wall / ceiling)
        // to instantly kill velocity. Mid-air sneak does nothing — that matches user expectation
        // ("hold shift while pressed against a block to stop").
        if (PlanetApi.API.isSpace(player.level()) && player.isShiftKeyDown() && canFly(player, stack)) {
            boolean touchingBlock = player.onGround()
                || player.horizontalCollision
                || player.verticalCollision;
            if (touchingBlock) {
                applySpaceBrake(player);
                consume(player, stack, 30);
            }
        }

        // Jet Boots "boost mode" hover: on a planet, HOLD SNEAK while airborne to hover in place.
        // This is deliberate — when you're neither boosting (jump) nor hovering (sneak), full planet
        // gravity applies, so on heavy worlds like Jupiter you fall normally instead of slow-falling.
        if (!KeybindManager.jumpDown(player)
            && player.isShiftKeyDown()
            && UpgradeUtils.hasBoostMode(player.getItemBySlot(EquipmentSlot.FEET))
            && !player.onGround()
            && !PlanetApi.API.isSpace(player.level())
            && canFly(player, stack)) {
            hover(player);
            consume(player, stack, 20);
            return;
        }

        if (!KeybindManager.jumpDown(player)) return;
        if (!canFly(player, stack)) return;

        if (KeybindManager.sprintDown(player)) {
            fullFlight(player);
            consume(player, stack, 100);
        } else {
            upwardsFlight(player);
            consume(player, stack, 50);
        }
    }

    /**
     * Jet Boots hover (boost upgrade): hold altitude while the player sneaks in mid-air. Releasing sneak
     * drops them back to full planet gravity (the caller gates this on isShiftKeyDown). Horizontal
     * momentum is lightly damped so the player settles into a controllable hover.
     */
    private void hover(Player player) {
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * 0.92, 0.0, v.z * 0.92);
        player.fallDistance = 0.0f;
    }

    private void applySpaceBrake(Player player) {
        Vec3 v = player.getDeltaMovement();
        if (v.lengthSqr() < 1.0e-4) return;
        // Instant zero when touching a block. The caller already gates on
        // touchingBlock, so reaching here means the player is grounded or
        // pressed against a surface — no reason to drift.
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void checkAtmosphereLeave(Player player) {
        if (AdAstraConfig.jetSuitAtmosphereLeave < 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> currentDim = serverLevel.dimension();
        Planet currentPlanet = AdAstraData.planets().get(currentDim);
        if (currentPlanet == null) return;

        // On a planet surface: fly to orbit at jetSuitAtmosphereLeave height.
        // For Earth → earth_orbit specifically, arrive at y=-100 (the space-station Y);
        // other planets keep the legacy "top of build height" clamp via land(...).
        if (!currentPlanet.isSpace() && player.getY() >= AdAstraConfig.jetSuitAtmosphereLeave) {
            ResourceKey<Level> orbitDim = currentPlanet.orbitIfPresent();
            ServerLevel orbitLevel = serverLevel.getServer().getLevel(orbitDim);
            if (orbitLevel == null) return;
            if (orbitDim.equals(Planet.EARTH_ORBIT)) {
                ModUtils.landAt(serverPlayer, orbitLevel, new Vec3(player.getX(), -100, player.getZ()));
            } else {
                ModUtils.land(serverPlayer, orbitLevel, new Vec3(player.getX(), AdAstraConfig.atmosphereLeave, player.getZ()));
            }
        }

        // In Earth orbit: fly to moon at 10,000 blocks
        if (currentDim.equals(Planet.EARTH_ORBIT) && player.getY() >= 10000) {
            ServerLevel moonLevel = serverLevel.getServer().getLevel(Planet.MOON);
            if (moonLevel == null) return;
            ModUtils.land(serverPlayer, moonLevel, new Vec3(player.getX(), AdAstraConfig.atmosphereLeave, player.getZ()));
        }
    }

    /** Extra thrust factor from the Jet Boots "boost mode" upgrade on high-gravity worlds. */
    private static final float BOOST_FACTOR = 2.5f;
    private static final float BOOST_GRAVITY_THRESHOLD = 15.0f; // m/s²

    /**
     * Jet Boots boost-mode upgrade: when the player wears boost boots AND the world's gravity is
     * above {@value #BOOST_GRAVITY_THRESHOLD} m/s² (e.g. Jupiter's 24.79), the suit thrusts much
     * harder so flight stays viable against the heavy pull. Returns 1.0 (no change) otherwise.
     */
    private float boostMultiplier(Player player) {
        if (!UpgradeUtils.hasBoostMode(player.getItemBySlot(EquipmentSlot.FEET))) return 1.0f;
        float rawGravity = GravityApi.API.getGravity(player) * PlanetConstants.EARTH_GRAVITY;
        return rawGravity > BOOST_GRAVITY_THRESHOLD ? BOOST_FACTOR : 1.0f;
    }

    // Flight physics below are the original 1.20.1 implementation (verbatim math),
    // with only the boost-mode multiplier layered on for heavy-gravity worlds.

    protected void upwardsFlight(Player player) {
        double acceleration = sigmoidAcceleration(player.tickCount, 5.0, 1.0, 2.0);
        acceleration /= 25.0f;
        double yBoost = Math.max(0.0025, acceleration) * boostMultiplier(player);
        player.setDeltaMovement(player.getDeltaMovement().add(0, yBoost, 0));
        player.fallDistance = Math.max(player.fallDistance / 1.5f, 0.0f);
    }

    protected void fullFlight(Player player) {
        if (player.getDeltaMovement().length() > 2.0) return;
        Vec3 movement = player.getLookAngle().normalize().scale(0.075 * boostMultiplier(player));
        player.setDeltaMovement(player.getDeltaMovement().add(movement));
        player.fallDistance = Math.max(player.fallDistance / 1.5f, 0.0f);
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
    }

    private boolean canFly(Player player, ItemStack stack) {
        return player.isCreative() || getEnergyStorage(stack).getStoredAmount() > 0;
    }

    private void consume(Player player, ItemStack stack, int amount) {
        if (player.isCreative() || player.isSpectator() || player.level().isClientSide()) return;
        var container = getEnergyStorage(stack);
        if (container == null) return;
        container.extract(amount, false);
        EnergyUtils.saveItemEnergyStorage(stack, container);
    }

    protected boolean isFullFlightEnabled(Player player) {
        return KeybindManager.suitFlightEnabled(player) && KeybindManager.jumpDown(player) && KeybindManager.sprintDown(player);
    }

    public static double sigmoidAcceleration(double t, double peakTime, double peakAcceleration, double initialAcceleration) {
        return ((2 * peakAcceleration) / (1 + Math.exp(-t / peakTime)) - peakAcceleration) + initialAcceleration;
    }

    public void spawnParticles(Level level, LivingEntity entity, HumanoidModel<?> model, ItemStack stack) {
        if (!(entity instanceof Player player)) return;
        if (!canFly(player, stack)) return;
        if (!hasFullJetSuitSet(player)) return;
        if (!KeybindManager.suitFlightEnabled(player)) return;
        if (!KeybindManager.jumpDown(player) || (!KeybindManager.jumpDown(player) && !KeybindManager.sprintDown(player)))
            return;

        spawnParticles(level, entity, model.rightArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, -0.45);
        spawnParticles(level, entity, model.leftArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, 0.45);
        spawnParticles(level, entity, model.rightLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, -0.1);
        spawnParticles(level, entity, model.leftLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, 0.1);
    }

    /**
     * Model-less particle spawn for client-side immediate feedback. Uses fixed limb angles
     * (≈ idle pose) since we don't have a HumanoidModel reference outside the armor renderer.
     *
     * <p>Called from {@code AdAstraClient.clientTick} as soon as the jump key is held — the
     * server still authoritatively applies the velocity, but the player gets instant visual
     * confirmation that the jet is firing instead of the ~50–100 ms "stutter" while the
     * keybind packet + velocity sync round-trip completes.
     */
    public void spawnParticles(Level level, Player player, ItemStack stack) {
        if (!canFly(player, stack)) return;
        if (!hasFullJetSuitSet(player)) return;
        if (!KeybindManager.suitFlightEnabled(player)) return;
        if (!KeybindManager.jumpDown(player)) return;

        spawnParticles(level, player, 0.05, player.isFallFlying() ? 0.0 : 0.8, -0.45);
        spawnParticles(level, player, 0.05, player.isFallFlying() ? 0.0 : 0.8, 0.45);
        spawnParticles(level, player, 0.05, player.isFallFlying() ? 0.1 : 0.0, -0.1);
        spawnParticles(level, player, 0.05, player.isFallFlying() ? 0.1 : 0.0, 0.1);
    }

    // Spawns particles at the limbs of the player
    private void spawnParticles(Level level, LivingEntity entity, double pitch, double yOffset, double zOffset) {
        double yRot = entity.yBodyRot;
        double forwardOffsetX = Math.cos(yRot * Math.PI / 180) * zOffset;
        double forwardOffsetZ = Math.sin(yRot * Math.PI / 180) * zOffset;
        double sideOffsetX = Math.cos((yRot - 90) * Math.PI / 180) * pitch;
        double sideOffsetZ = Math.sin((yRot - 90) * Math.PI / 180) * pitch;

        level.addParticle(ParticleTypes.FLAME, true, false,
            entity.getX() + forwardOffsetX + sideOffsetX,
            entity.getY() + yOffset,
            entity.getZ() + sideOffsetZ + forwardOffsetZ,
            0, 0, 0);
    }

    @SuppressWarnings("unused") // NeoForge
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (entity.level().isClientSide()) return true;
        if (this.type != ArmorType.CHESTPLATE) return true;
        int nextFlightTick = flightTicks + 1;
        if (nextFlightTick % 10 != 0) return true;

        if (nextFlightTick % 20 == 0) {
            stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
        }

        entity.gameEvent(GameEvent.ELYTRA_GLIDE);

        return true;
    }

    @SuppressWarnings("unused") // NeoForge
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return entity instanceof Player player && canFly(player, stack) && isFullFlightEnabled(player);
    }
}