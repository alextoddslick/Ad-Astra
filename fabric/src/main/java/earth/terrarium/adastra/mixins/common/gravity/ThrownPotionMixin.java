package earth.terrarium.adastra.mixins.common.gravity;

import earth.terrarium.adastra.api.systems.GravityApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractThrownPotion.class)
public abstract class ThrownPotionMixin extends Entity {

    public ThrownPotionMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // 1.21.1: getGravity now returns double instead of float
    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    public void adastra$getGravity(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue((double) (0.05f * GravityApi.API.getGravity(this)));
    }
}
