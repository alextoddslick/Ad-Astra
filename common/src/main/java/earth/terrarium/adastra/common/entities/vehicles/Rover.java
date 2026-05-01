package earth.terrarium.adastra.common.entities.vehicles;

import earth.terrarium.adastra.client.radio.audio.RadioHandler;
import earth.terrarium.adastra.common.menus.vehicles.RoverMenu;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ClientboundPlayStationPacket;
import earth.terrarium.adastra.common.registry.ModDamageSources;
import earth.terrarium.adastra.common.registry.ModItems;
import earth.terrarium.adastra.common.tags.ModFluidTags;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.radio.RadioHolder;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.fluid.util.FluidStorageData;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Rover extends Vehicle implements PlayerRideable, RadioHolder {

    private static final long BUCKET = 81000L;
    private static final float MAX_SPEED_KM = 50.0f;
    private static final float ACCELERATION_RATE = 0.02f;
    /**
     * Hard cap on absolute Y velocity in blocks/tick. Vehicle.tickGravity()
     * runs unconditionally, but Vehicle.tick() only calls move() when there
     * is a controlling passenger. While riderless, gravity therefore
     * accumulates in deltaMovement.y without ever being dissipated by a
     * vertical collision. Combined with doEntityCollisionTick() — which
     * uses getDeltaMovement().length() as the launch power for nearby
     * entities — a just-dismounted player still inside the inflated AABB
     * could be flung upward at hundreds of blocks/tick. Clamping y here
     * (and using horizontalDistance for collision power below) breaks that
     * loop.
     */
    private static final double Y_VELOCITY_CAP = 2.0;

    public static final EntityDataAccessor<Long> FUEL = SynchedEntityData.defineId(Rover.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<String> FUEL_TYPE = SynchedEntityData.defineId(Rover.class, EntityDataSerializers.STRING);

    private final SimpleFluidStorage fluidContainer = new SimpleFluidStorage(1, 3000L * BUCKET / 1000L);

    private float speed;
    private float angle;

    public float wheelXRot;
    public float wheelYRot;

    private String radioUrl = "";

    /**
     * Tracks entities already pushed/damaged by doEntityCollisionTick this
     * tick. Cleared at the start of each invocation. Defense in depth: even
     * if some upstream pathway calls doEntityCollisionTick more than once per
     * tick, an individual entity gets at most one push + damage application
     * per tick. Without this, a player stuck in the inflated AABB could
     * accumulate stacked +Y boosts and reach hundreds of blocks/tick.
     */
    private final Set<UUID> ranOverThisTick = new HashSet<>();

    public Rover(EntityType<?> type, Level level) {
        super(type, level);

        addPart(0.6f, 0.7f, new Vector3f(0.6f, 1f, 0.5f), (player, hand) -> {
            if (player.getVehicle() instanceof Rover) {
                if (player.level().isClientSide()) {
                    RadioHandler.open(null);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        addPart(1.1f, 0.7f, new Vector3f(0.15f, 0.8f, -1.7f), (player, hand) -> {
            if (!level().isClientSide()) {
                this.openCustomInventoryScreen(player);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUEL, 0L);
        builder.define(FUEL_TYPE, "air");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        speed = input.getFloatOr("Speed", 0f);
        angle = input.getFloatOr("Angle", 0f);
        input.read("FluidData", FluidStorageData.CODEC).ifPresent(fluidContainer::readSnapshot);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Speed", speed);
        output.putFloat("Angle", angle);
        output.store("FluidData", FluidStorageData.CODEC, fluidContainer.createSnapshot());
    }

    public SimpleFluidStorage fluidContainer() {
        return fluidContainer;
    }

    @Override
    public ItemStack getDropStack() {
        ItemStack stack = ModItems.ROVER.get().getDefaultInstance();
        FluidResource resource = fluidContainer.getResource(0);
        long amount = fluidContainer.getAmount(0);
        if (!resource.isBlank() && amount > 0) {
            SimpleFluidStorage itemStorage = FluidUtils.getItemFluidStorage(stack, 1, fluidContainer.get(0).getLimit(FluidResource.BLANK));
            FluidUtils.insertFluid(itemStorage.get(0), resource, amount, false);
            FluidUtils.saveItemFluidStorage(stack, itemStorage);
        }
        return stack;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    private void clampRotation(Entity entityToUpdate) {
        entityToUpdate.setYBodyRot(getYRot());
        float degrees = Mth.wrapDegrees(entityToUpdate.getYRot() - getYRot());
        float lookAngle = Mth.clamp(degrees, -105.0f, 105.0f);
        entityToUpdate.yRotO += lookAngle - degrees;
        entityToUpdate.setYRot(entityToUpdate.getYRot() + lookAngle - degrees);
        entityToUpdate.setYHeadRot(entityToUpdate.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity entityToUpdate) {
        clampRotation(entityToUpdate);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float zOffset = getControllingPassenger() == passenger ? 1.75f : -1.75f;
        Vec3 position = new Vec3(-0.5, 0, zOffset).yRot(-getYRot() * (float) (Math.PI / 180) - (float) (Math.PI / 2));
        if (level().isClientSide()) {
            RadioHandler.stop();
        }
        return new Vec3(getX() + position.x, getY(), getZ() + position.z);
    }

    @Override
    public boolean isSafeToDismount(Player player) {
        return speed() < 0.1f;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;

        float zOffset = getControllingPassenger() == passenger ? -0.6f : 0.4f;
        // 1.21+ passenger anchor is computed differently — the previous 0.95f
        // value put the player ~1 block above the seat; -0.05f put them too
        // low (sunken into the seat). 0.45f sits the rider on the seat surface.
        float yOffset = this.isRemoved() ? 0.01f : 0.45f;
        Vec3 position = new Vec3(-0.5, 0, zOffset).yRot(-getYRot() * (float) (Math.PI / 180) - (float) (Math.PI / 2));

        clampRotation(passenger);
        passenger.setYRot(passenger.getYRot() + angle);
        passenger.setYHeadRot(passenger.getYHeadRot() + angle);
        callback.accept(passenger, getX() + position.x, getY() + yOffset, getZ() + position.z);
    }

    @Override
    public void tick() {
        super.tick();
        handleVehicleMovementTick();
        // Vehicle.tick() only calls move() when a controlling passenger is
        // present. Without this, gravity from tickGravity() accumulates in
        // deltaMovement.y while the rover is parked or after a dismount.
        // Apply movement here when riderless so gravity is dissipated by
        // ground collisions.
        if (getControllingPassenger() == null) {
            move(MoverType.SELF, getDeltaMovement());
            tickFriction();
        }
        doEntityCollisionTick();
        // Final defensive Y clamp: any pathway (gravity accumulation, vanilla
        // push, mod interactions) that managed to put |y| above Y_VELOCITY_CAP
        // gets clipped here so the next tick can't feed runaway values into
        // collision math or motion.
        Vec3 dm = getDeltaMovement();
        if (Math.abs(dm.y) > Y_VELOCITY_CAP) {
            setDeltaMovement(dm.x, Mth.clamp(dm.y, -Y_VELOCITY_CAP, Y_VELOCITY_CAP), dm.z);
        }
        if (!level().isClientSide()) {
            FluidUtils.moveItemToContainer(inventory, fluidContainer, 0, 1, 0);
            FluidUtils.moveContainerToItem(inventory, fluidContainer, 0, 1, 0);

            FluidResource fluidResource = fluidContainer.getResource(0);
            entityData.set(FUEL, fluidContainer.getAmount(0));
            entityData.set(FUEL_TYPE, BuiltInRegistries.FLUID.getKey(fluidResource.getType()).toString());
        }
    }

    private void handleVehicleMovementTick() {
        boolean noPassenger = getControllingPassenger() == null;
        float xxa = -xxa(); // right/left
        float zza = zza(); // forward/backward

        if (!onGround()) {
            xxa *= 0.2f;
            zza *= 0.2f;
        }

        if (!hasEnoughFuel()) {
            xxa = 0;
            zza = 0;
        }

        // acceleration
        if (zza != 0) {
            speed += ACCELERATION_RATE * zza;
        } else {
            speed *= noPassenger ? 0.98f : 0.96f;
        }

        if (noPassenger && speed < 0.1f && speed > -0.1f) {
            speed *= 0.9f;
        }

        // clamp speed
        float maxBlocksPerTick = MAX_SPEED_KM / 20.0f / 3.6f;
        speed = Mth.clamp(speed, -maxBlocksPerTick / 2, maxBlocksPerTick);

        // turning
        if (xxa != 0 && (speed > 0.05f || speed < -0.05f)) {
            angle += xxa * Math.signum(speed) * Math.abs(speed);
        } else {
            angle *= noPassenger ? 0.95f : 0.75f;
        }

        // clamp turning
        angle = Mth.clamp(angle, -3, 3);

        // handle turning
        setYRot(getYRot() + angle);

        // handle speed
        float yRot = getYRot() * (float) (Math.PI / 180);
        // Reset Y on ground — Vehicle.tickGravity() always subtracts gravity
        // but Vehicle.tick() only calls move() when there's a controlling
        // passenger, so without this reset the y component drifts unbounded.
        // Always cap |y| as a final safety net against runaway accumulation.
        double rawY = getDeltaMovement().y;
        double y = onGround() ? Math.max(0.0, rawY) : Mth.clamp(rawY, -Y_VELOCITY_CAP, Y_VELOCITY_CAP);
        setDeltaMovement(
            Mth.sin(-yRot) * speed,
            y,
            Mth.cos(yRot) * speed
        );

        if (zza > 0) consumeFuel();
    }


    // run over entities, launching and damaging them
    private void doEntityCollisionTick() {
        // Reset the per-tick dedupe set on every invocation. If something
        // calls this method N times per tick (which would be a bug), each
        // entity still only gets one push+damage on the *first* call; the
        // remaining calls find them in the set and skip. The set is cleared
        // here rather than at end-of-tick so we don't depend on tick ordering.
        ranOverThisTick.clear();
        if (level().isClientSide()) return;
        // Use horizontalDistance — not length() — so any leftover y velocity
        // (e.g. accumulated gravity while riderless) can't fuel a runaway
        // upward launch of nearby entities. The rover only "runs over"
        // things by moving horizontally anyway.
        double horizontalSpeed = getDeltaMovement().horizontalDistance();
        if (horizontalSpeed <= 0.15) return;
        AABB aabb = getBoundingBox().inflate(1.001);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, aabb, entity -> !getPassengers().contains(entity));
        if (entities.isEmpty()) return;

        double power = Math.min(horizontalSpeed * 0.4, 0.6); // hard cap on launch power
        // Survivable damage cap: previous formula (power*0.5*100) gave up to
        // 30 damage — a one-shot kill on a 20-HP player at the lightest
        // brush. New formula scales linearly with power up to a max of 6
        // damage (3 hearts), so even worst-case a healthy player survives
        // a touch and can react.
        float damage = (float) Math.min(power * 8.0, 6.0);
        // Vertical-push cap: previous code added the full `power` (up to 0.6)
        // to Y velocity. Combined with any other vertical force that lands
        // on the entity in the same tick, this could launch a player into
        // the stratosphere. Cap the Y component at 0.15 — enough to feel
        // a knock, not enough to kill via fall damage.
        double yPush = Math.min(power, 0.15);
        var yRot = getYRot() * (float) (Math.PI / 180);
        for (var entity : entities) {
            if (!ranOverThisTick.add(entity.getUUID())) continue;
            entity.setDeltaMovement(entity.getDeltaMovement().add(Mth.sin(-yRot) * 0.1, yPush, Mth.cos(yRot) * 0.1));
            entity.hurt(ModDamageSources.ranOver(level(), this, getControllingPassenger()), damage);
        }
    }

    public float speed() {
        return speed;
    }

    public float angle() {
        return angle;
    }

    @Override
    public @NotNull String getRadioUrl() {
        return radioUrl;
    }

    @Override
    public void setRadioUrl(@NotNull String url) {
        this.radioUrl = url;

        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) {
                NetworkHandler.CHANNEL.sendToPlayer(new ClientboundPlayStationPacket(url), player);
            }
        }
    }

    public void consumeFuel() {
        if (level().isClientSide() || tickCount % 5 != 0) return;
        FluidResource resource = fluidContainer.getResource(0);
        if (!resource.isBlank()) {
            fluidContainer.extract(resource, BUCKET / 1000L, false);
        }
    }

    public boolean hasEnoughFuel() {
        if (level().isClientSide()) {
            return entityData.get(FUEL) > 0;
        }
        return fluidContainer.getAmount(0) > 0;
    }

    public FluidResource fluidResource() {
        return FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(entityData.get(FUEL_TYPE))));
    }

    public long fluidAmount() {
        return entityData.get(FUEL);
    }

    public long fluidCapacity() {
        return fluidContainer.getLimit(0, FluidResource.BLANK);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RoverMenu(containerId, inventory, this);
    }

    @Override
    public int getInventorySize() {
        return 18;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ROVER.get());
    }
}
