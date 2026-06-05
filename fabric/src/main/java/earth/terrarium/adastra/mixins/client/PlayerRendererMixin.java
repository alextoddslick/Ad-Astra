package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.client.models.armor.SpaceSuitModel;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the Ad Astra space-suit sleeve over the first-person arm.
 *
 * <p>In 26.1, {@code PlayerRenderer} was renamed to {@link AvatarRenderer} and the first-person hand
 * is drawn by its private {@code renderHand(PoseStack, SubmitNodeCollector, int, Identifier, ModelPart, boolean)}
 * (called from {@code renderRightHand}/{@code renderLeftHand}). That method receives the already-posed
 * arm {@link ModelPart}, so — exactly like the pre-port mixin did against the old {@code renderHand} —
 * we cancel vanilla's bare-arm draw for suit wearers and submit the matching suit arm part instead,
 * posed identically.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void adastra$renderSuitHand(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skinTexture, ModelPart arm, boolean slim, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack chest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof SpaceSuitItem)) return;

        ModelLayerLocation layer = SpaceSuitModel.getLayerLocation(chest);
        Identifier texture = SpaceSuitModel.getTextureLocation(chest);
        if (layer == null || texture == null) return;

        // Take over the bare-arm render for suit wearers.
        ci.cancel();

        ModelPart root = minecraft.getEntityModels().bakeLayer(layer);
        SpaceSuitModel suit = new SpaceSuitModel(root, EquipmentSlot.CHEST, chest, null);

        // The arm param is this renderer's own PlayerModel arm; match it to pick the correct side.
        PlayerModel avatarModel = (PlayerModel) ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel();
        boolean right = arm == avatarModel.rightArm;
        ModelPart suitArm = right ? suit.rightArm : suit.leftArm;

        // Mirror exactly what vanilla AvatarRenderer#renderHand does to the bare arm before submitting:
        // reset to the layer's initial (rest) pose — the first-person position comes from the pose stack,
        // not the arm's local transform — then visible + the slight outward zRot vanilla applies
        // (+0.1 right / -0.1 left). The vanilla arm is NOT yet reset at HEAD, so replicating the reset
        // (rather than copying the arm's stale transform) is what keeps the sleeve aligned. ModelPart#copyFrom
        // was removed in 26.1 anyway.
        suitArm.resetPose();
        suitArm.visible = true;
        suitArm.zRot = right ? 0.1f : -0.1f;

        collector.submitModelPart(suitArm, poseStack, RenderTypes.armorTranslucent(texture), light, OverlayTexture.NO_OVERLAY, null);
    }
}
