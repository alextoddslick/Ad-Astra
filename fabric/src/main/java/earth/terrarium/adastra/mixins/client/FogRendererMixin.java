package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.client.utils.ClientStormData;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pulls the world fog planes in during a planet storm so you can't see far — the storms are
 * thick enough to obscure the horizon. Scales with {@link ClientStormData#intensity()} on top
 * of the dimension's static {@code has_thick_fog} (which only tints fog colour, not distance).
 * Only the terrain fog (environmental + renderDistance planes) is tightened; sky/cloud fog is
 * left alone so the giant Jupiter in the sky still shows through.
 */
@Mixin(value = FogRenderer.class, priority = 2000)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), require = 1)
    private void adastra$applyStormFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                                       float gammaValue, ClientLevel level,
                                       CallbackInfoReturnable<FogData> cir) {
        float intensity = ClientStormData.effectiveStormIntensity();
        if (intensity <= 0) return;
        FogData fog = cir.getReturnValue();
        if (fog == null) return;

        // At full intensity, compress the fog gradient to ~22% of its normal distance — the gas
        // is thick. (Storm particles add further visual obstruction on top of this.)
        float factor = Mth.lerp(intensity, 1.0f, 0.22f);
        fog.environmentalStart *= factor;
        fog.environmentalEnd *= factor;
        fog.renderDistanceStart *= factor;
        fog.renderDistanceEnd *= factor;
        // Keep end strictly past start so the fog math stays well-formed.
        fog.environmentalEnd = Math.max(fog.environmentalEnd, fog.environmentalStart + 1.0f);
        fog.renderDistanceEnd = Math.max(fog.renderDistanceEnd, fog.renderDistanceStart + 1.0f);
    }
}
