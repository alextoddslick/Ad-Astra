package earth.terrarium.adastra.common.entities.vehicles;

import earth.terrarium.adastra.common.entities.multipart.MultipartPartEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.function.BiFunction;

public class VehiclePart extends Entity implements MultipartPartEntity<Vehicle> {

    private final Vehicle vehicle;
    private final EntityDimensions size;
    private final Vector3f offset;
    private final BiFunction<Player, InteractionHand, InteractionResult> interactionHandler;

    public VehiclePart(Vehicle vehicle, float width, float height, Vector3f offset, BiFunction<Player, InteractionHand, InteractionResult> interactionHandler) {
        super(vehicle.getType(), vehicle.level());
        this.vehicle = vehicle;
        this.interactionHandler = interactionHandler;
        this.offset = offset;
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean isPickable() {
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return this.vehicle.getPickResult();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.vehicle.hurtServer(level, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return entity == this || this.vehicle == entity;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 vec3) {
        return this.interactionHandler.apply(player, hand);
    }

    public void tickPart() {
        this.setOldPosAndRot();
        Matrix3f rotationMatrix = new Matrix3f();
        rotationMatrix.rotateY(this.vehicle.getYRot() * 0.017453292F);
        Vector3f offset = this.offset.mulTranspose(rotationMatrix, new Vector3f());
        this.setPos(this.vehicle.getX() + offset.x(), this.vehicle.getY() + offset.y(), this.vehicle.getZ() + offset.z());
    }

    @Override
    public Vehicle getParent() {
        return this.vehicle;
    }
}
