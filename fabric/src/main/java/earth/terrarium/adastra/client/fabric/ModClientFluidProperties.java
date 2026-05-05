package earth.terrarium.adastra.client.fabric;

import com.teamresourceful.resourcefullib.client.fluid.data.ClientFluidProperties;
import com.teamresourceful.resourcefullib.client.fluid.registry.ResourcefulClientFluidRegistry;
import earth.terrarium.adastra.AdAstra;
import net.minecraft.resources.ResourceLocation;

public final class ModClientFluidProperties {

    public static final ResourcefulClientFluidRegistry CLIENT_FLUID_PROPERTIES = new ResourcefulClientFluidRegistry(AdAstra.MOD_ID);

    public static void init() {
        register("oxygen", 0xffdae6f0);
        register("hydrogen", 0xff89CFF0);
        register("oil", 0xff373A36);
        register("fuel", 0xffE5292B);
        register("cryo_fuel", 0xff6cfffa);
    }

    private static void register(String id, int tintColor) {
        CLIENT_FLUID_PROPERTIES.register(id, ClientFluidProperties.builder()
            .still(ResourceLocation.withDefaultNamespace("block/water_still"))
            .flowing(ResourceLocation.withDefaultNamespace("block/water_flow"))
            .overlay(ResourceLocation.withDefaultNamespace("block/water_overlay"))
            .screenOverlay(ResourceLocation.withDefaultNamespace("textures/misc/underwater.png"))
            .tintColor(tintColor));
    }
}
