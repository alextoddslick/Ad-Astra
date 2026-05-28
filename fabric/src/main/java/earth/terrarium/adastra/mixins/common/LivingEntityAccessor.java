package earth.terrarium.adastra.mixins.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor
    boolean isJumping();

    @Invoker
    Vec3 invokeHandleRelativeFrictionAndCalculateMovement(Vec3 travelVector, float friction);
}
