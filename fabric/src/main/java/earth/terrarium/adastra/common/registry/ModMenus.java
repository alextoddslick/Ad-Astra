package earth.terrarium.adastra.common.registry;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.blockentities.machines.*;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.menus.base.BaseContainerMenu;
import earth.terrarium.adastra.common.menus.machines.*;
import earth.terrarium.adastra.common.menus.vehicles.LanderMenu;
import earth.terrarium.adastra.common.menus.vehicles.RocketMenu;
import earth.terrarium.adastra.common.menus.vehicles.RoverMenu;
import com.teamresourceful.resourcefullib.common.menu.MenuContentHelper;
import earth.terrarium.adastra.common.menus.base.BlockPosContent;
import earth.terrarium.adastra.common.menus.base.EntityIdContent;
import earth.terrarium.adastra.common.menus.base.PlanetsMenuContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class ModMenus {

    public static final ResourcefulRegistry<MenuType<?>> MENUS = ResourcefulRegistries.create(BuiltInRegistries.MENU, AdAstra.MOD_ID);

    public static final RegistryEntry<MenuType<CoalGeneratorMenu>> COAL_GENERATOR = MENUS.register("coal_generator_menu", () -> createMenuType(CoalGeneratorMenu::new, CoalGeneratorBlockEntity.class));
    public static final RegistryEntry<MenuType<CompressorMenu>> COMPRESSOR = MENUS.register("compressor_menu", () -> createMenuType(CompressorMenu::new, CompressorBlockEntity.class));
    public static final RegistryEntry<MenuType<EtrionicBlastFurnaceMenu>> ETRIONIC_BLAST_FURNACE = MENUS.register("etrionic_blast_furnace_menu", () -> createMenuType(EtrionicBlastFurnaceMenu::new, EtrionicBlastFurnaceBlockEntity.class));
    public static final RegistryEntry<MenuType<OxygenLoaderMenu>> OXYGEN_LOADER = MENUS.register("oxygen_loader_menu", () -> createMenuType(OxygenLoaderMenu::new, OxygenLoaderBlockEntity.class));
    public static final RegistryEntry<MenuType<FuelRefineryMenu>> FUEL_REFINERY = MENUS.register("fuel_refinery_menu", () -> createMenuType(FuelRefineryMenu::new, FuelRefineryBlockEntity.class));
    public static final RegistryEntry<MenuType<WaterPumpMenu>> WATER_PUMP = MENUS.register("water_pump_menu", () -> createMenuType(WaterPumpMenu::new, WaterPumpBlockEntity.class));
    public static final RegistryEntry<MenuType<SolarPanelMenu>> SOLAR_PANEL = MENUS.register("solar_panel_menu", () -> createMenuType(SolarPanelMenu::new, SolarPanelBlockEntity.class));
    public static final RegistryEntry<MenuType<OxygenDistributorMenu>> OXYGEN_DISTRIBUTOR = MENUS.register("oxygen_distributor_menu", () -> createMenuType(OxygenDistributorMenu::new, OxygenDistributorBlockEntity.class));
    public static final RegistryEntry<MenuType<GravityNormalizerMenu>> GRAVITY_NORMALIZER = MENUS.register("gravity_normalizer_menu", () -> createMenuType(GravityNormalizerMenu::new, GravityNormalizerBlockEntity.class));
    public static final RegistryEntry<MenuType<CryoFreezerMenu>> CRYO_FREEZER = MENUS.register("cryo_freezer_menu", () -> createMenuType(CryoFreezerMenu::new, CryoFreezerBlockEntity.class));
    public static final RegistryEntry<MenuType<NasaWorkbenchMenu>> NASA_WORKBENCH = MENUS.register("nasa_workbench_menu", () -> createMenuType(NasaWorkbenchMenu::new, NasaWorkbenchBlockEntity.class));

    public static final RegistryEntry<MenuType<RoverMenu>> ROVER = MENUS.register("rover_menu", () -> MenuContentHelper.create(
        (id, inventory, content) -> new RoverMenu(id, inventory, content.map(EntityIdContent::entityId).orElse(-1)),
        EntityIdContent.SERIALIZER
    ));
    public static final RegistryEntry<MenuType<RocketMenu>> ROCKET = MENUS.register("rocket_menu", () -> MenuContentHelper.create(
        (id, inventory, content) -> new RocketMenu(id, inventory, content.map(EntityIdContent::entityId).orElse(-1)),
        EntityIdContent.SERIALIZER
    ));
    public static final RegistryEntry<MenuType<LanderMenu>> LANDER = MENUS.register("lander_menu", () -> MenuContentHelper.create(
        (id, inventory, content) -> new LanderMenu(id, inventory, content.map(EntityIdContent::entityId).orElse(-1)),
        EntityIdContent.SERIALIZER
    ));

    public static final RegistryEntry<MenuType<PlanetsMenu>> PLANETS = MENUS.register("planets_menu", () -> MenuContentHelper.create(
        (id, inventory, content) -> content
            .map(c -> new PlanetsMenu(id, inventory, c.disabledPlanets(), c.spaceStations(), c.spawnLocations()))
            .orElse(new PlanetsMenu(id, inventory, java.util.Set.of(), java.util.Map.of(), java.util.Set.of())),
        PlanetsMenuContent.SERIALIZER
    ));

    private static <T extends BaseContainerMenu<E>, E extends BlockEntity> MenuType<T> createMenuType(Factory<T, E> factory, Class<E> clazz) {
        return MenuContentHelper.create(
            (id, inventory, content) -> factory.create(
                id,
                inventory,
                content.map(c -> BaseContainerMenu.getBlockEntityFromBuf(inventory.player.level(), c.pos(), clazz)).orElse(null)
            ),
            BlockPosContent.SERIALIZER
        );
    }

    public interface Factory<T extends BaseContainerMenu<E>, E extends BlockEntity> {

        T create(int syncId, Inventory inventory, E blockEntity);
    }
}
