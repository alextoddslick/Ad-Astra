package earth.terrarium.adastra.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import earth.terrarium.adastra.common.tags.ModBiomeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// In 1.21.11, cloud rendering moved to CloudRenderer and weather rendering moved to WeatherEffectRenderer.
// The old renderClouds, renderSnowAndRain, and tickRain methods no longer exist in LevelRenderer.
// Weather/cloud customization for acid rain and Venus clouds now requires mixins into
// WeatherEffectRenderer and CloudRenderer respectively.
// See WeatherEffectRendererMixin and CloudRendererMixin for the new implementations.
@Mixin(value = LevelRenderer.class, priority = 2000)
public abstract class LevelRendererMixin {

    @Unique
    @SuppressWarnings("unused")
    private boolean adastra$hasAcidRain() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.level().getBiome(player.blockPosition()).is(ModBiomeTags.HAS_ACID_RAIN);
    }
}
