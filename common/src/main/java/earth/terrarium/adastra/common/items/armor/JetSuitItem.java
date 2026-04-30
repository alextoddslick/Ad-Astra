package earth.terrarium.adastra.common.items.armor;

import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.planets.AdAstraData;
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

    private void applySpaceBrake(Player player) {
        Vec3 v = player.getDeltaMovement();
        if (v.lengthSqr() < 1.0e-4) return;
        // Instant zero when touching a block. The caller already gates on
        // touchingBlock, so reaching here means the player is grounded or
        // pressed against a surface — no reason to drift.
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private void checkAtmosphereLeave(Player player) {
        if (AdAstraConfig.jetSuitAtmosphereLeave < 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> currentDim = serverLevel.dimension();
        Planet currentPlanet = AdAstraData.planets().get(currentDim);
        if (currentPlanet == null) return;

        // On a planet surface: fly to orbit at jetSuitAtmosphereLeave height
        if (!currentPlanet.isSpace() && player.getY() >= AdAstraConfig.jetSuitAtmosphereLeave) {
            ResourceKey<Level> orbitDim = currentPlanet.orbitIfPresent();
            ServerLevel orbitLevel = serverLevel.getServer().getLevel(orbitDim);
            if (orbitLevel == null) return;
            ModUtils.land(serverPlayer, orbitLevel, new Vec3(player.getX(), AdAstraConfig.atmosphereLeave, player.getZ()));
        }

        // In Earth orbit: fly to moon at 10,000 blocks
        if (currentDim.equals(Planet.EARTH_ORBIT) && player.getY() >= 10000) {
            ServerLevel moonLevel = serverLevel.getServer().getLevel(Planet.MOON);
            if (moonLevel == null) return;
            ModUtils.land(serverPlayer, moonLevel, new Vec3(player.getX(), AdAstraConfig.atmosphereLeave, player.getZ()));
        }
    }

    protected void upwardsFlight(Player player) {
        double acceleration = sigmoidAcceleration(player.tickCount, 5.0, 1.0, 2.0);
        acceleration /= 35.0f;
        double yBoost = Math.max(0.002, acceleration);

        if (PlanetApi.API.isSpace(player.level())) {
            // In space, while jump is held, gradually bleed off lateral momentum
            // (~20% per tick) instead of zeroing it instantly — feels like real
            // attitude thrusters rather than an emergency stop. Y gets the boost
            // additively. After ~10 ticks (0.5s) the player is going essentially
            // straight up.
            Vec3 v = player.getDeltaMovement();
            final double lateralDamp = 0.80;
            player.setDeltaMovement(v.x * lateralDamp, v.y + yBoost, v.z * lateralDamp);
        } else {
            // Atmospheric: gravity + drag handle horizontal naturally; just push Y.
            player.push(new Vec3(0, yBoost, 0));
        }
        player.fallDistance = Math.max(player.fallDistance / 1.5f, 0.0f);
        player.hurtMarked = true;
    }

    protected void fullFlight(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 current = player.getDeltaMovement();

        if (PlanetApi.API.isSpace(player.level())) {
            // Space mode: redirect existing momentum toward look direction (turn assist),
            // plus apply forward thrust. Without this the player just adds vectors and
            // can't actually steer — looking somewhere else doesn't change where you go.
            double speed = current.length();
            double turnRate = 0.18;       // 18% of momentum redirected per tick
            double thrust = 0.085;        // higher than the atmospheric 0.055
            double maxSpeed = 1.8;

            Vec3 redirected = current.scale(1.0 - turnRate).add(look.scale(speed * turnRate));
            Vec3 newVel = redirected.add(look.scale(thrust));
            if (newVel.length() > maxSpeed) newVel = newVel.normalize().scale(maxSpeed);
            player.setDeltaMovement(newVel);
        } else {
            // Atmospheric: original behaviour. Drag/gravity provide the natural deceleration.
            if (current.length() > 1.5) return;
            player.push(look.scale(0.055));
        }

        player.fallDistance = Math.max(player.fallDistance / 1.5f, 0.0f);
        player.hurtMarked = true;
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