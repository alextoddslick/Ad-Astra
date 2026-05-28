package earth.terrarium.adastra.common.registry;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.blockentities.GlobeBlockEntity;
import earth.terrarium.adastra.common.blockentities.RadioBlockEntity;
import earth.terrarium.adastra.common.blockentities.SlidingDoorBlockEntity;
import earth.terrarium.adastra.common.blockentities.flag.FlagBlockEntity;
import earth.terrarium.adastra.common.blockentities.machines.*;
import earth.terrarium.adastra.common.blockentities.pipes.CableBlockEntity;
import earth.terrarium.adastra.common.blockentities.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public class ModBlockEntityTypes {

    public static final ResourcefulRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = ResourcefulRegistries.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AdAstra.MOD_ID);

    public static final RegistryEntry<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR = BLOCK_ENTITY_TYPES.register(
        "coal_generator",
        () -> newBlockEntityType(
            CoalGeneratorBlockEntity::new,
            ModBlocks.COAL_GENERATOR.get()));

    public static final RegistryEntry<BlockEntityType<CompressorBlockEntity>> COMPRESSOR = BLOCK_ENTITY_TYPES.register(
        "compressor",
        () -> newBlockEntityType(
            CompressorBlockEntity::new,
            ModBlocks.COMPRESSOR.get()));

    public static final RegistryEntry<BlockEntityType<EtrionicBlastFurnaceBlockEntity>> ETRIONIC_BLAST_FURNACE = BLOCK_ENTITY_TYPES.register(
        "etreonic_blast_furnace",
        () -> newBlockEntityType(
            EtrionicBlastFurnaceBlockEntity::new,
            ModBlocks.ETRIONIC_BLAST_FURNACE.get()));

    public static final RegistryEntry<BlockEntityType<OxygenLoaderBlockEntity>> OXYGEN_LOADER = BLOCK_ENTITY_TYPES.register(
        "oxygen_loader",
        () -> newBlockEntityType(
            OxygenLoaderBlockEntity::new,
            ModBlocks.OXYGEN_LOADER.get()));

    public static final RegistryEntry<BlockEntityType<FuelRefineryBlockEntity>> FUEL_REFINERY = BLOCK_ENTITY_TYPES.register(
        "fuel_refinery",
        () -> newBlockEntityType(
            FuelRefineryBlockEntity::new,
            ModBlocks.FUEL_REFINERY.get()));

    public static final RegistryEntry<BlockEntityType<WaterPumpBlockEntity>> WATER_PUMP = BLOCK_ENTITY_TYPES.register(
        "water_pump",
        () -> newBlockEntityType(
            WaterPumpBlockEntity::new,
            ModBlocks.WATER_PUMP.get()));

    public static final RegistryEntry<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL = BLOCK_ENTITY_TYPES.register(
        "solar_panel",
        () -> newBlockEntityType(
            SolarPanelBlockEntity::new,
            ModBlocks.SOLAR_PANEL.get()));

    public static final RegistryEntry<BlockEntityType<OxygenDistributorBlockEntity>> OXYGEN_DISTRIBUTOR = BLOCK_ENTITY_TYPES.register(
        "oxygen_distributor",
        () -> newBlockEntityType(
            OxygenDistributorBlockEntity::new,
            ModBlocks.OXYGEN_DISTRIBUTOR.get()));

    public static final RegistryEntry<BlockEntityType<GravityNormalizerBlockEntity>> GRAVITY_NORMALIZER = BLOCK_ENTITY_TYPES.register(
        "gravity_normalizer",
        () -> newBlockEntityType(
            GravityNormalizerBlockEntity::new,
            ModBlocks.GRAVITY_NORMALIZER.get()));

    public static final RegistryEntry<BlockEntityType<EnergizerBlockEntity>> ENERGIZER = BLOCK_ENTITY_TYPES.register(
        "energizer",
        () -> newBlockEntityType(
            EnergizerBlockEntity::new,
            ModBlocks.ENERGIZER.get()));

    public static final RegistryEntry<BlockEntityType<CryoFreezerBlockEntity>> CRYO_FREEZER = BLOCK_ENTITY_TYPES.register(
        "cryo_freezer",
        () -> newBlockEntityType(
            CryoFreezerBlockEntity::new,
            ModBlocks.CRYO_FREEZER.get()));

    public static final RegistryEntry<BlockEntityType<DetectorBlockEntity>> Detector = BLOCK_ENTITY_TYPES.register(
        "detector",
        () -> newBlockEntityType(
            DetectorBlockEntity::new,
            ModBlocks.OXYGEN_SENSOR.get()));

    public static final RegistryEntry<BlockEntityType<NasaWorkbenchBlockEntity>> NASA_WORKBENCH = BLOCK_ENTITY_TYPES.register(
        "nasa_workbench",
        () -> newBlockEntityType(
            NasaWorkbenchBlockEntity::new,
            ModBlocks.NASA_WORKBENCH.get()));

    public static final RegistryEntry<BlockEntityType<GlobeBlockEntity>> GLOBE = BLOCK_ENTITY_TYPES.register(
        "globe",
        () -> createBlockEntityType(
            GlobeBlockEntity::new,
            ModBlocks.GLOBES));

    public static final RegistryEntry<BlockEntityType<FlagBlockEntity>> FLAG = BLOCK_ENTITY_TYPES.register(
        "flag",
        () -> createBlockEntityType(
            FlagBlockEntity::new,
            ModBlocks.FLAGS));

    public static final RegistryEntry<BlockEntityType<SlidingDoorBlockEntity>> SLIDING_DOOR = BLOCK_ENTITY_TYPES.register(
        "sliding_door",
        () -> createBlockEntityType(
            SlidingDoorBlockEntity::new,
            ModBlocks.SLIDING_DOORS));

    public static final RegistryEntry<BlockEntityType<CableBlockEntity>> CABLE = BLOCK_ENTITY_TYPES.register(
        "cable",
        () -> createBlockEntityType(
            CableBlockEntity::new,
            ModBlocks.CABLES));

    public static final RegistryEntry<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE = BLOCK_ENTITY_TYPES.register(
        "fluid_pipe",
        () -> createBlockEntityType(
            FluidPipeBlockEntity::new,
            ModBlocks.FLUID_PIPES));

    public static final RegistryEntry<BlockEntityType<RadioBlockEntity>> RADIO = BLOCK_ENTITY_TYPES.register(
        "radio",
        () -> newBlockEntityType(
            RadioBlockEntity::new,
            ModBlocks.RADIO.get()));

    public static <E extends BlockEntity> BlockEntityType<E> createBlockEntityType(BiFunction<BlockPos, BlockState, E> factory, ResourcefulRegistry<Block> registry) {
        Block[] blocks = registry.stream()
                .map(RegistryEntry::get)
                .toArray(Block[]::new);
        return newBlockEntityType(factory, blocks);
    }

    /**
     * Creates a new BlockEntityType by reflectively invoking the private constructor.
     * In 1.26.2, BlockEntityType.BlockEntitySupplier was made package-private, so we
     * use a dynamic Proxy to bridge our BiFunction factory to the package-private SAM.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends BlockEntity> BlockEntityType<E> newBlockEntityType(BiFunction<BlockPos, BlockState, E> factory, Block... blocks) {
        try {
            Class<?> supplierClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object proxy = Proxy.newProxyInstance(
                supplierClass.getClassLoader(),
                new Class<?>[] { supplierClass },
                (p, method, args) -> {
                    if ("create".equals(method.getName()) && args != null && args.length == 2) {
                        return factory.apply((BlockPos) args[0], (BlockState) args[1]);
                    }
                    // Fall through to default Object methods
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(factory, args);
                    }
                    return null;
                }
            );
            Constructor<BlockEntityType> constructor = BlockEntityType.class.getDeclaredConstructor(
                supplierClass, Set.class
            );
            constructor.setAccessible(true);
            return (BlockEntityType<E>) constructor.newInstance(proxy, Set.of(blocks));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create BlockEntityType", e);
        }
    }
}
