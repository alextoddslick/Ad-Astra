package earth.terrarium.adastra;

import com.mojang.logging.LogUtils;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.PlanetData;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ClientboundSyncLocalPlanetDataPacket;
import earth.terrarium.adastra.common.network.packets.ClientboundSyncPlanetsPacket;
import earth.terrarium.adastra.common.planets.AdAstraData;
import earth.terrarium.adastra.common.registry.*;
import earth.terrarium.adastra.common.utils.radio.StationLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AdAstra {

    public static final String MOD_ID = "ad_astra";
    public static final Configurator CONFIGURATOR = new Configurator(MOD_ID);
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Supplier<RegistryAccess> registryAccessSupplier;

    public static void init() {
        CONFIGURATOR.register(AdAstraConfig.class);

        NetworkHandler.init();
        StationLoader.init();

        ModFluids.FLUIDS.init();
        ModFluidProperties.FLUID_PROPERTIES.init();
        ModBlocks.BLOCKS.init();
        ModArmorMaterials.init();
        ModItems.ITEMS.init();
        ModCreativeTab.TABS.init();
        ModEntityTypes.ENTITY_TYPES.init();
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.init();
        ModMenus.MENUS.init();
        ModRecipeTypes.RECIPE_TYPES.init();
        ModRecipeSerializers.RECIPE_SERIALIZERS.init();
        ModParticleTypes.PARTICLE_TYPES.init();
        // Painting variants are data-driven in 1.21.1
        ModSoundEvents.SOUND_EVENTS.init();
        ModStructures.STRUCTURE_TYPES.init();
        ModStructures.STRUCTURE_PROCESSORS.init();
        ModFeatures.FEATURES.init();
        ModWorldCarvers.WORLD_CARVERS.init();
        ModBiomeSources.BIOME_SOURCES.init();
        ModDensityFunctionTypes.DENSITY_FUNCTION_TYPES.init();
    }

    public static void postInit() {
        // In 1.21.11, CauldronInteraction.DYED_ITEM was removed (dyedItemIteration is private).
        // Dyed item cauldron interactions are now handled automatically via the DyedItemColor component.
        ModEntityTypes.registerSpawnPlacements();
    }

    public static void onAddReloadListener(BiConsumer<Identifier, PreparableReloadListener> registry) {
        registry.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets"), new AdAstraData());
    }

    public static void onDatapackSync(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSyncPlanetsPacket(AdAstraData.planets()), player);
        earth.terrarium.adastra.common.handlers.PlanetStormHandler.syncToPlayer(player);
    }

    public static void onServerTick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            if (player.tickCount % 5 == 0) {
                boolean oxygen = OxygenApi.API.hasOxygen(player);
                short temperature = TemperatureApi.API.getTemperature(player);
                float gravity = GravityApi.API.getGravity(player);
                NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSyncLocalPlanetDataPacket(new PlanetData(oxygen, temperature, gravity)), player);
            }
        });
        earth.terrarium.adastra.common.events.VelocityDebugTicker.onServerTick(server);
        earth.terrarium.adastra.common.events.AtmosphereLeaveTicker.onServerTick(server);
        earth.terrarium.adastra.common.handlers.PlanetStormHandler.onServerTick(server);
    }

    public static void onServerStarted(MinecraftServer server) {
        setRegistryAccess(server::registryAccess);
    }

    public static void setRegistryAccess(Supplier<RegistryAccess> supplier) {
        registryAccessSupplier = supplier;
    }

    public static RegistryAccess getRegistryAccess() {
        return registryAccessSupplier.get();
    }
}
