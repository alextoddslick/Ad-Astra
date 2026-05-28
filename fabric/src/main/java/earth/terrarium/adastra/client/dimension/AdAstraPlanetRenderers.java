package earth.terrarium.adastra.client.dimension;

import com.mojang.serialization.JsonOps;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class AdAstraPlanetRenderers extends SimpleJsonResourceReloadListener<PlanetRenderer> {

    public AdAstraPlanetRenderers() {
        super(PlanetRenderer.CODEC, FileToIdConverter.json("planet_renderers"));
    }

    @Override
    protected void apply(Map<Identifier, PlanetRenderer> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceKey<Level>, ModDimensionSpecialEffects> effects = new HashMap<>();
        object.forEach((key, renderer) -> {
            effects.put(renderer.dimension(), new ModDimensionSpecialEffects(renderer));
        });
        ClientPlatformUtils.registerPlanetRenderers(effects);
    }
}
