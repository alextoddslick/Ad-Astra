package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.client.renderers.ti69.Ti69Renderer;
import earth.terrarium.adastra.common.registry.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int combinedLight, float equippedProgress, float swingProgress, HumanoidArm side) {}

    @Inject(method = "renderArmWithItem", at = @At(value = "HEAD"), cancellable = true)
    private void adastra$renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, SubmitNodeCollector collector, int combinedLight, CallbackInfo ci) {
        if (stack.is(ModItems.TI_69.get())) {
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidArm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            // TODO: Ti69Renderer needs updating for SubmitNodeCollector
            // Ti69Renderer.renderTi69(poseStack, collector, combinedLight, equippedProgress, humanoidArm, swingProgress, this::renderPlayerArm);
            ci.cancel();
        }
    }
}
