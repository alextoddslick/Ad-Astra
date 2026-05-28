package earth.terrarium.adastra.client.registry;

import earth.terrarium.adastra.AdAstra;
import com.teamresourceful.resourcefullib.client.fluid.data.ClientFluidProperties;
import com.teamresourceful.resourcefullib.client.fluid.registry.ResourcefulClientFluidRegistry;
import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import net.minecraft.resources.Identifier;

public final class ModClientFluidProperties {

    public static final ResourcefulClientFluidRegistry CLIENT_FLUID_PROPERTIES = new ResourcefulClientFluidRegistry(AdAstra.MOD_ID);

    private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier WATER_FLOW = Identifier.withDefaultNamespace("block/water_flow");
    private static final Identifier WATER_OVERLAY = Identifier.withDefaultNamespace("block/water_overlay");

    public static final RegistryEntry<ClientFluidProperties> OXYGEN = CLIENT_FLUID_PROPERTIES.register("oxygen",
        ClientFluidProperties.builder()
            .still(WATER_STILL)
            .flowing(WATER_FLOW)
            .overlay(WATER_OVERLAY)
            .tintColor(0x9090C8FF));

    public static final RegistryEntry<ClientFluidProperties> HYDROGEN = CLIENT_FLUID_PROPERTIES.register("hydrogen",
        ClientFluidProperties.builder()
            .still(WATER_STILL)
            .flowing(WATER_FLOW)
            .overlay(WATER_OVERLAY)
            .tintColor(0x90E0E0FF));

    public static final RegistryEntry<ClientFluidProperties> OIL = CLIENT_FLUID_PROPERTIES.register("oil",
        ClientFluidProperties.builder()
            .still(WATER_STILL)
            .flowing(WATER_FLOW)
            .overlay(WATER_OVERLAY)
            .tintColor(0xFF1A1A1A));

    public static final RegistryEntry<ClientFluidProperties> FUEL = CLIENT_FLUID_PROPERTIES.register("fuel",
        ClientFluidProperties.builder()
            .still(WATER_STILL)
            .flowing(WATER_FLOW)
            .overlay(WATER_OVERLAY)
            .tintColor(0xFFD4AA00));

    public static final RegistryEntry<ClientFluidProperties> CRYO_FUEL = CLIENT_FLUID_PROPERTIES.register("cryo_fuel",
        ClientFluidProperties.builder()
            .still(WATER_STILL)
            .flowing(WATER_FLOW)
            .overlay(WATER_OVERLAY)
            .tintColor(0xFF4DC8FF));

    public static void init() {
        CLIENT_FLUID_PROPERTIES.init();
    }
}
