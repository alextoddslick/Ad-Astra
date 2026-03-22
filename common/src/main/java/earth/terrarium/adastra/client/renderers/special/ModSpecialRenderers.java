package earth.terrarium.adastra.client.renderers.special;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

public final class ModSpecialRenderers {

    /**
     * Register special model renderers. Must be called from the platform module
     * which has access to SpecialModelRenderers.ID_MAPPER.
     */
    public static void register(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> mapper) {
        mapper.put(
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "rocket"),
            RocketSpecialRenderer.Unbaked.MAP_CODEC
        );
        mapper.put(
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "rover"),
            RoverSpecialRenderer.Unbaked.MAP_CODEC
        );
    }
}
