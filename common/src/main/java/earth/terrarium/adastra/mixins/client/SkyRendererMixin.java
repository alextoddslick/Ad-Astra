package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.client.dimension.PlanetSkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders Ad Astra planet/sun/moon discs after vanilla draws its skybox.
 *
 * Two injection sites cover both vanilla code paths:
 *
 * 1. {@code renderSunMoonAndStars} — only fires when {@code DimensionType.skybox == OVERWORLD}.
 *    Used as a safety net for dimensions that opt into an overworld-style sky.
 *
 * 2. {@code renderEndSky} — fires when {@code DimensionType.skybox == END}, which is what all
 *    Ad Astra orbit / airless-surface dimensions use (so vanilla draws the End starfield + dark
 *    sky for free). Without this second injection, planet discs were never drawn in orbit.
 *
 * Both call into {@link PlanetSkyRenderer#render}, which early-returns when the current
 * dimension has no Ad Astra planet renderer registered — overworld / nether / vanilla End
 * are unaffected.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(
        method = "renderSunMoonAndStars",
        at = @At("TAIL"))
    private void adastra$renderPlanetDiscsOverworld(PoseStack poseStack, float sunAngle, float moonAngle,
                                                    float starAngle, MoonPhase moonPhase,
                                                    float rainBrightness, float starBrightness,
                                                    CallbackInfo ci) {
        // sunAngle is radians; legacy PlanetSkyRenderer expects a 0..1 day fraction.
        float timeOfDayFraction = sunAngle / (float) (Math.PI * 2.0);
        PlanetSkyRenderer.render(poseStack, timeOfDayFraction);
    }

    /**
     * Fires after vanilla draws the End-style starfield and dark sky. Ad Astra orbit dimensions
     * use {@code "skybox": "end"} so they get the End starfield for free; this hook then layers
     * the planet/sun/moon discs on top.
     *
     * No PoseStack is provided by vanilla — we create a fresh one in absolute sky-space, matching
     * the camera-aligned frame the End sky pipeline runs in (the End-sky has no time-of-day
     * rotation either, so no rotation needs unwinding).
     */
    @Inject(
        method = "renderEndSky",
        at = @At("TAIL"))
    private void adastra$renderPlanetDiscsEnd(CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        // Day fraction derived from the level's time of day. Orbit dims tick day-time the same
        // way overworld does, so this gives the planets a slow rotation to match the (not-yet-
        // visible) sun position on the parent body's day cycle.
        float timeOfDayFraction = (level.getDayTime() % 24000L) / 24000.0f;
        PlanetSkyRenderer.render(new PoseStack(), timeOfDayFraction);
    }
}
