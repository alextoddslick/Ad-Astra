package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.common.tags.ModItemTags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// In 1.21.11, HumanoidModel.setupAnim takes a HumanoidRenderState instead of direct entity access.
// The Vehicle riding check and HELD_OVER_HEAD item check need to use render state data.
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void adastra$setupAnimTail(HumanoidRenderState state, CallbackInfo ci) {
        var model = ((HumanoidModel<?>) (Object) this);

        // Check if main hand or off hand items should be held over head
        if (state.rightHandItemStack != null && state.rightHandItemStack.is(ModItemTags.HELD_OVER_HEAD)) {
            model.rightArm.xRot = -2.8f;
            model.leftArm.xRot = model.rightArm.xRot;
        } else if (state.leftHandItemStack != null && state.leftHandItemStack.is(ModItemTags.HELD_OVER_HEAD)) {
            model.leftArm.xRot = -2.8f;
            model.rightArm.xRot = model.leftArm.xRot;
        }
    }
}
