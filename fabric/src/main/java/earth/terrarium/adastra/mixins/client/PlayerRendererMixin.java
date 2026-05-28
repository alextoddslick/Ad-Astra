package earth.terrarium.adastra.mixins.client;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * In 1.21.11, PlayerRenderer was renamed to AvatarRenderer, and the rendering API changed significantly:
 * - LivingEntityRenderer now takes 3 type parameters (Entity, RenderState, Model)
 * - renderHand signature changed to (PoseStack, SubmitNodeCollector, int, Identifier, ModelPart, boolean)
 * - The player entity is no longer available in renderHand (only render state is used)
 * - MultiBufferSource is replaced by SubmitNodeCollector in many rendering methods
 * - RenderType moved to net.minecraft.client.renderer.rendertype.RenderType
 * - FastColor.ARGB32 is now net.minecraft.util.ARGB
 *
 * TODO: 1.21.11 - Reimplement space suit hand rendering using the new AvatarRenderer API.
 * The old approach of extending LivingEntityRenderer and shadowing setupRotations/setModelProperties
 * no longer works. Space suit hand rendering should use render layers or a different hook point.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
}
