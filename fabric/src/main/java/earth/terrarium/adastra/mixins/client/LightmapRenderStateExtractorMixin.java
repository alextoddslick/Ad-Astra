package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.client.utils.ClientStormData;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Darkens the world during a planet storm — the storm clouds block out the sun. Scales the
 * computed lightmap brightness down by up to 60% at full {@link ClientStormData#intensity()}
 * (so the world dims like a heavy overcast but never goes pitch black). Uses the same brightness
 * field vanilla uses for the Darkness mob effect, so it dims terrain geometry, not just the screen.
 */
@Mixin(value = LightmapRenderStateExtractor.class, priority = 2000)
public abstract class LightmapRenderStateExtractorMixin {

    @Inject(method = "extract", at = @At("TAIL"), require = 1)
    private void adastra$applyStormDarkening(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        float intensity = ClientStormData.effectiveStormIntensity();
        if (intensity <= 0) return;
        state.brightness *= (1.0f - 0.6f * intensity);
    }
}
