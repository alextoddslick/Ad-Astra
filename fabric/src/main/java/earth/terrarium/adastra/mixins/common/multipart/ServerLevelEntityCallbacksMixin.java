package earth.terrarium.adastra.mixins.common.multipart;

import earth.terrarium.adastra.common.entities.multipart.MultipartEntity;
import earth.terrarium.adastra.common.entities.multipart.MultipartPartEntity;
import earth.terrarium.adastra.common.entities.multipart.MultipartPartsHolder;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.minecraft.server.level.ServerLevel$EntityCallbacks"})
public class ServerLevelEntityCallbacksMixin {


    // 1.21.1: Use TAIL instead of targeting updateDynamicGameEventListener for resilience
    @Inject(
        method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void adastra$onTrackingStart(Entity entity, CallbackInfo ci) {
        if (entity instanceof MultipartEntity multipartEntity) {
            MultipartPartsHolder holder = (MultipartPartsHolder) entity.level();
            for (MultipartPartEntity<?> part : multipartEntity.getParts()) {
                Entity partEntity = ((Entity) part);
                holder.adastra$getParts().put(partEntity.getId(), partEntity);
            }
        }
    }

    // 1.21.1: Use TAIL instead of targeting updateDynamicGameEventListener for resilience
    @Inject(
        method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void adastra$onTrackingStop(Entity entity, CallbackInfo ci) {
        if (entity instanceof MultipartEntity multipartEntity) {
            MultipartPartsHolder holder = (MultipartPartsHolder) entity.level();
            for (MultipartPartEntity<?> part : multipartEntity.getParts()) {
                Entity partEntity = ((Entity) part);
                holder.adastra$getParts().remove(partEntity.getId());
            }
        }
    }
}
