package earth.terrarium.adastra.common.entities.vehicles;

import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.client.utils.SoundUtils;
import earth.terrarium.adastra.common.menus.vehicles.LanderMenu;
import earth.terrarium.adastra.common.registry.ModParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Lander extends Vehicle {

    private float speed;
    private float angle;
    public boolean startedRocketSound;

    public Lander(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        speed = input.getFloatOr("Speed", 0f);
        angle = input.getFloatOr("Angle", 0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Speed", speed);
        output.putFloat("Angle", angle);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0, 2.5f, 0);
    }

    @Override
    public boolean hideRider() {
        return true;
    }

    @Override
    public boolean zoomOutCameraInThirdPerson() {
        return true;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return super.getDismountLocationForPassenger(passenger)
            .add(passenger.getLookAngle().multiply(1, 0, 1)
                .normalize()
                .subtract(0, 2, 0));
    }

    @Override
    public boolean isSafeToDismount(Player player) {
        return onGround();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        passenger.setYRot(getYRot());
        passenger.setYHeadRot(getYHeadRot());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYRot(passenger.getYRot() + angle);
        passenger.setYHeadRot(passenger.getYHeadRot() + angle);
    }

    @Override
    public void tick() {
        // Capture pre-tick vertical velocity so we can detect a hard impact
        // that occurs during this tick (touchdown after a fall).
        double prevDeltaY = getDeltaMovement().y;
        boolean wasInAir = !onGround();

        super.tick();

        if (!onGround()) {
            if (getControllingPassenger() != null) {
                // Player is steering — original flight behaviour.
                flightTick();
            } else {
                // Riderless mid-air (e.g. player dismounted) — make sure the
                // lander actually falls instead of hovering. Vehicle.tick()
                // only calls move() when there's a controlling passenger,
                // so we need to apply movement ourselves here. Gravity has
                // already been applied by Vehicle.tickGravity().
                speed = (float) getDeltaMovement().y;
                move(MoverType.SELF, getDeltaMovement());
                tickFriction();
            }
        } else {
            angle = 0;
        }

        // Detect ground impact (was airborne last tick, now on ground).
        // If the lander hit hard, blow up the ground; otherwise let it sit.
        if (wasInAir && onGround() && !level().isClientSide()) {
            double impactSpeed = Math.abs(prevDeltaY);
            if (impactSpeed >= HARD_IMPACT_THRESHOLD) {
                explode(IMPACT_EXPLOSION_RADIUS);
            }
        }
    }

    /**
     * Vertical speed (blocks/tick) at or above which a touchdown causes a ground-shattering explosion.
     * Piloted descent is clamped to ~1.1 blocks/tick by {@link #flightTick()}, so anything above 1.5
     * indicates an unpiloted free-fall (e.g. the player dismounted mid-air).
     */
    private static final double HARD_IMPACT_THRESHOLD = 1.5;
    /** Explosion radius used for hard impacts (smaller than {@link #explode()}'s 10-block fall-distance blast). */
    private static final float IMPACT_EXPLOSION_RADIUS = 3.0f;

    private void flightTick() {
        var delta = getDeltaMovement();
        float xxa = -xxa(); // right/left

        if (xxa != 0) {
            angle += xxa * 1;
        } else {
            angle *= 0.9f;
        }

        if (passengerHasSpaceDown() && delta.y < -0.05) {
            speed += 0.01f;
            fallDistance *= 0.9f;
            spawnLanderParticles();
            if (level().isClientSide() && !startedRocketSound) {
                startedRocketSound = true;
                SoundUtils.playLanderSound(this);
            }
        } else if (speed > -1.1) {
            speed -= 0.01f;
        }

        // clamp turning
        angle = Mth.clamp(angle, -3, 3);

        setYRot(getYRot() + angle);

        setDeltaMovement(
            delta.x(),

            speed,
            delta.z()
        );

        if (isInWater()) {
            setDeltaMovement(delta.x(), Math.min(0.06, delta.y() + 0.15), delta.z());
            speed *= 0.9f;
        }
    }

    public void explode() {
        explode(10);
    }

    public void explode(float radius) {
        if (level().isClientSide()) return;
        level().explode(
            this,
            getX(), getY(), getZ(),
            radius,
            OxygenApi.API.hasOxygen(this.level()),
            Level.ExplosionInteraction.TNT);
        discard();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (level().isClientSide()) return false;
        if (fallDistance > 40 && onGround()) {
            explode();
            return true;
        }
        return false;
    }

    public void spawnLanderParticles() {
        if (!level().isClientSide()) return;
        for (int i = 0; i < 10; i++) {
            level().addParticle(ModParticleTypes.LARGE_FLAME.get(),
                getX(), getY() - 0.2, getZ(),
                Mth.nextDouble(level().random, -0.05, 0.05),
                Mth.nextDouble(level().random, -0.05, 0.05),
                Mth.nextDouble(level().random, -0.05, 0.05));
        }

        for (int i = 0; i < 10; i++) {
            level().addParticle(ModParticleTypes.LARGE_SMOKE.get(),
                getX(), getY() - 0.2, getZ(),
                Mth.nextDouble(level().random, -0.05, 0.05),
                Mth.nextDouble(level().random, -0.05, 0.05),
                Mth.nextDouble(level().random, -0.05, 0.05));
        }
    }

    @Override
    public ItemStack getDropStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public int getInventorySize() {
        return 11;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LanderMenu(containerId, inventory, this);
    }
}
