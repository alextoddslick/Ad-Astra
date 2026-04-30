package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.client.dimension.PlanetSkyRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders Ad Astra planet/sun/moon discs after vanilla draws sun + moon + stars,
 * so planets sit *over* the star field. Early-returns inside {@link PlanetSkyRenderer#render}
 * for any dimension that has no registered planet renderer (overworld, nether, end, modded).
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(
        method = "renderSunMoonAndStars",
        at = @At("TAIL"))
    private void adastra$renderPlanetDiscs(PoseStack poseStack, float sunAngle, float moonAngle,
                                           float starAngle, MoonPhase moonPhase,
                                           float rainBrightness, float starBrightness,
                                           CallbackInfo ci) {
        // Pass sunAngle as the time-of-day proxy for renderable rotation; legacy used getTimeOfDay()
        // which is the same value (radians vs the 0..1 fraction) — sunAngle here is in radians,
        // and PlanetSkyRenderer multiplies by 360 internally for the legacy convention. Convert.
        float timeOfDayFraction = sunAngle / (float) (Math.PI * 2.0);
        PlanetSkyRenderer.render(poseStack, timeOfDayFraction);
    }
}
