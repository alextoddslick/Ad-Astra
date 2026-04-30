package earth.terrarium.adastra.client.neoforge;

import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class ClientPlatformUtilsImpl {

    public static final Map<Item, ArmorRenderer> ARMOR_RENDERERS = new HashMap<>();
    public static final Map<ResourceKey<Level>, ModDimensionSpecialEffects> DIMENSION_RENDERERS = new HashMap<>();

    public static BlockStateModel getModel(ModelManager dispatcher, Identifier id) {
        // TODO: 1.21.11 - BakedModel is now BlockStateModel. ModelManager.getModel(Identifier) may not exist.
        // Need to find the new NeoForge/vanilla API for model lookup.
        return null; // Placeholder - needs API update
    }

    public static void registerArmor(Identifier texture, ModelLayerLocation layer, ClientPlatformUtils.ArmorFactory factory, Item... items) {
        for (Item item : items) {
            ARMOR_RENDERERS.put(item, new ArmorRenderer(texture, layer, factory));
        }
    }

    public static void registerPlanetRenderers(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        DIMENSION_RENDERERS.clear();
        DIMENSION_RENDERERS.putAll(renderers);
    }

    public static Map<ResourceKey<Level>, ModDimensionSpecialEffects> getPlanetRenderers() {
        return DIMENSION_RENDERERS;
    }

    public record ArmorRenderer(Identifier texture, ModelLayerLocation layer,
                                ClientPlatformUtils.ArmorFactory factory) {}
}
