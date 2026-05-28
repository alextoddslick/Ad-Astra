package earth.terrarium.adastra.client.dimension;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * In 1.21.11, DimensionSpecialEffects has been removed. Dimension rendering is now handled
 * via DimensionType.Skybox enum (NONE, OVERWORLD, END) and the new SkyRenderer, CloudRenderer,
 * and WeatherEffectRenderer classes.
 *
 * This class retains the static helper methods and renderer references that are still used
 * by other parts of Ad Astra. The custom sky/cloud/weather rendering hooks must be
 * reimplemented using mixins into SkyRenderer, CloudRenderer, and WeatherEffectRenderer.
 *
 * TODO: 1.21.11 - Reimplement custom dimension rendering:
 * - Custom sky rendering: Mixin into SkyRenderer or provide a custom SkyRenderer via LevelRenderer
 * - Custom cloud rendering: Mixin into CloudRenderer
 * - Custom weather rendering: Mixin into WeatherEffectRenderer
 * - Fog color customization: Mixin into FogRenderer (now at net.minecraft.client.renderer.fog.FogRenderer)
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ModDimensionSpecialEffects {

    private final PlanetRenderer renderer;
    private final ModSkyRenderer skyRenderer;

    public ModDimensionSpecialEffects(PlanetRenderer renderer) {
        this.renderer = renderer;
        this.skyRenderer = new ModSkyRenderer(renderer);
    }

    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        if (renderer.hasFog()) {
            return fogColor.multiply(
                brightness * 0.94 + 0.06,
                brightness * 0.94 + 0.06,
                brightness * 0.91 + 0.09);
        }
        return Vec3.ZERO;
    }

    public boolean isFoggyAt(int x, int y) {
        return renderer.hasThickFog();
    }

    @Nullable
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        // Prevent the FogRenderer from rendering the sunrise if the sun isn't setting in the west.
        if (renderer.sunriseAngle() != 0) return null;
        return getSunriseColor(timeOfDay, partialTicks, renderer.sunriseColor());
    }

    @Nullable
    public static float[] getSunriseColor(float timeOfDay, float partialTicks, int sunColor) {
        float timeCos = Mth.cos(timeOfDay * (float) (Math.PI * 2));
        if (timeCos >= -0.4f && timeCos <= 0.4f) {
            float time = timeCos / 0.4f * 0.5f + 0.5f;
            float alpha = 1 - (1 - Mth.sin(time * (float) Math.PI)) * 0.99F;
            alpha *= alpha;
            var rgba = new float[4];
            rgba[0] = time * 0.3f + ARGB.red(sunColor) / 255f * 0.7f;
            rgba[1] = time * time * 0.7f + ARGB.green(sunColor) / 255f * 0.5f;
            rgba[2] = ARGB.blue(sunColor) / 255f * 0.6f;
            rgba[3] = alpha;
            return rgba;
        } else {
            return null;
        }
    }

    public PlanetRenderer renderer() {
        return renderer;
    }

    public ModSkyRenderer skyRenderer() {
        return skyRenderer;
    }
}
