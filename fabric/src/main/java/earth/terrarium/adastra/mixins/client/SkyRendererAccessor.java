package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes private members of vanilla {@link SkyRenderer} so Ad Astra can drive vanilla's
 * star buffer when rendering its own custom space skies.
 *
 * Only the star pass is needed — Ad Astra draws its own planet/sun/moon discs separately
 * and bypasses the End sky / sun / moon vanilla calls (so we get no End-purple void texture
 * and no End-tied audio in space dimensions).
 */
@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {

    @Invoker("renderStars")
    void adastra$renderStars(float starBrightness, PoseStack poseStack);
}
