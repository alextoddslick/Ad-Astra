package earth.terrarium.adastra.common.registry;


import earth.terrarium.adastra.AdAstra;
import com.teamresourceful.resourcefullib.common.fluid.data.FluidData;
import com.teamresourceful.resourcefullib.common.fluid.data.FluidProperties;
import com.teamresourceful.resourcefullib.common.fluid.registry.ResourcefulFluidRegistry;
import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistryType;

public final class ModFluidProperties {

    public static final ResourcefulFluidRegistry FLUID_PROPERTIES = ResourcefulRegistries.create(ResourcefulRegistryType.FLUID, AdAstra.MOD_ID);

    public static final RegistryEntry<FluidData> OXYGEN = FLUID_PROPERTIES.register("oxygen", FluidProperties.builder()
        .viscosity(0)
        .density(-1)
        .canConvertToSource(false));

    public static final RegistryEntry<FluidData> HYDROGEN = FLUID_PROPERTIES.register("hydrogen", FluidProperties.builder()
        .viscosity(0)
        .density(-1)
        .canConvertToSource(false));

    public static final RegistryEntry<FluidData> OIL = FLUID_PROPERTIES.register("oil", FluidProperties.builder()
        .viscosity(2000)
        .density(2000)
        .canConvertToSource(false));

    public static final RegistryEntry<FluidData> FUEL = FLUID_PROPERTIES.register("fuel", FluidProperties.builder()
        .viscosity(1500)
        .density(1500)
        .canConvertToSource(false));

    public static final RegistryEntry<FluidData> CRYO_FUEL = FLUID_PROPERTIES.register("cryo_fuel", FluidProperties.builder()
        .viscosity(71)
        .density(71)
        .temperature(-196)
        .canConvertToSource(false));
}
