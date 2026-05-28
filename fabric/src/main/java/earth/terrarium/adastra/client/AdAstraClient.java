package earth.terrarium.adastra.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import earth.terrarium.adastra.client.dimension.AdAstraPlanetRenderers;
import earth.terrarium.adastra.client.models.armor.SpaceSuitModel;
import earth.terrarium.adastra.client.models.blocks.GlobeCubeModel;
import earth.terrarium.adastra.client.models.blocks.GravityNormalizerToeModel;
import earth.terrarium.adastra.client.models.blocks.GravityNormalizerTopModel;
import earth.terrarium.adastra.client.models.blocks.OxygenDistributorTopModel;
import earth.terrarium.adastra.client.models.entities.mobs.*;
import earth.terrarium.adastra.client.models.entities.vehicles.LanderModel;
import earth.terrarium.adastra.client.models.entities.vehicles.RocketModel;
import earth.terrarium.adastra.client.models.entities.vehicles.RoverModel;
import earth.terrarium.adastra.client.particle.LargeFlameParticle;
import earth.terrarium.adastra.client.particle.OxygenBubbleParticle;
import earth.terrarium.adastra.client.radio.audio.RadioHandler;
import earth.terrarium.adastra.client.renderers.blocks.*;
import earth.terrarium.adastra.client.renderers.entities.mobs.*;
import earth.terrarium.adastra.client.renderers.entities.vehicles.LanderRenderer;
import earth.terrarium.adastra.client.renderers.entities.vehicles.RocketRenderer;
import earth.terrarium.adastra.client.renderers.entities.vehicles.RoverRenderer;
import earth.terrarium.adastra.client.renderers.world.OverlayRenderer;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.client.screens.machines.*;
import earth.terrarium.adastra.client.screens.player.OverlayScreen;
import earth.terrarium.adastra.client.screens.vehicles.LanderScreen;
import earth.terrarium.adastra.client.screens.vehicles.RocketScreen;
import earth.terrarium.adastra.client.screens.vehicles.RoverScreen;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.items.EtrionicCapacitorItem;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundSyncKeybindPacket;
import earth.terrarium.adastra.common.registry.*;
import earth.terrarium.adastra.common.tags.ModItemTags;
import earth.terrarium.adastra.common.utils.KeybindManager;
import earth.terrarium.adastra.common.entities.vehicles.Vehicle;
import earth.terrarium.adastra.common.utils.radio.RadioHolder;
// TODO: CSL migration - botarium ClientHooks provided registerBlockEntityRenderers, registerEntityRenderer,
// registerItemProperty, setRenderLayer. These registration methods need platform-specific replacements.
// import earth.terrarium.botarium.client.ClientHooks;
import earth.terrarium.adastra.client.utils.ClientRegistrationHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
// TODO: 1.21.11 - ItemColor no longer exists. Item tinting is now data-driven via ItemTintSource.
// The onAddItemColors method signature needs to be reworked for the new system.
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AdAstraClient {

    public static final OverlayRenderer OXYGEN_OVERLAY_RENDERER = new OverlayRenderer(0x4099ccff, () -> AdAstraConfigClient.showOxygenDistributorArea, ModBlocks.OXYGEN_DISTRIBUTOR);
    public static final OverlayRenderer GRAVITY_OVERLAY_RENDERER = new OverlayRenderer(0x40DE2F14, () -> AdAstraConfigClient.showGravityNormalizerArea, ModBlocks.GRAVITY_NORMALIZER);

    private static boolean forcedThirdPerson = false;
    private static CameraType previousCameraType = null;

    public static final KeyMapping.Category AD_ASTRA_KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "key_category"));

    public static final KeyMapping KEY_TOGGLE_SUIT_FLIGHT = new KeyMapping(
        ConstantComponents.TOGGLE_SUIT_FLIGHT_KEY.getString(),
        InputConstants.KEY_V,
        AD_ASTRA_KEY_CATEGORY);

    public static final KeyMapping KEY_OPEN_RADIO = new KeyMapping(
        ConstantComponents.OPEN_RADIO_KEY.getString(),
        InputConstants.KEY_R,
        AD_ASTRA_KEY_CATEGORY);

    public static void init() {
        AdAstra.CONFIGURATOR.register(AdAstraConfigClient.class);
        earth.terrarium.adastra.client.registry.ModClientFluidProperties.init();
        registerScreens();
        registerBlockEntityRenderers();
        registerItemProperties();
        registerRenderLayers();
        registerArmor();

        AdAstra.setRegistryAccess(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) return RegistryAccess.EMPTY;
            return connection.registryAccess();
        });
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenus.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        MenuScreens.register(ModMenus.COMPRESSOR.get(), CompressorScreen::new);
        MenuScreens.register(ModMenus.ETRIONIC_BLAST_FURNACE.get(), EtrionicBlastFurnaceScreen::new);
        MenuScreens.register(ModMenus.OXYGEN_LOADER.get(), OxygenLoaderScreen::new);
        MenuScreens.register(ModMenus.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        MenuScreens.register(ModMenus.WATER_PUMP.get(), WaterPumpScreen::new);
        MenuScreens.register(ModMenus.SOLAR_PANEL.get(), SolarPanelScreen::new);
        MenuScreens.register(ModMenus.OXYGEN_DISTRIBUTOR.get(), OxygenDistributorScreen::new);
        MenuScreens.register(ModMenus.GRAVITY_NORMALIZER.get(), GravityNormalizerScreen::new);
        MenuScreens.register(ModMenus.CRYO_FREEZER.get(), CryoFreezerScreen::new);
        MenuScreens.register(ModMenus.NASA_WORKBENCH.get(), NasaWorkbenchScreen::new);

        MenuScreens.register(ModMenus.ROCKET.get(), RocketScreen::new);
        MenuScreens.register(ModMenus.ROVER.get(), RoverScreen::new);
        MenuScreens.register(ModMenus.LANDER.get(), LanderScreen::new);

        MenuScreens.register(ModMenus.PLANETS.get(), PlanetsScreen::new);
    }

    private static void registerBlockEntityRenderers() {
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.ENERGIZER.get(), c -> new EnergizerBlockEntityRenderer(c));
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.GLOBE.get(), c -> new GlobeBlockEntityRenderer(c));
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.OXYGEN_DISTRIBUTOR.get(), c -> new OxygenDistributorBlockEntityRenderer(c));
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.GRAVITY_NORMALIZER.get(), c -> new GravityNormalizerBlockEntityRenderer());
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.FLAG.get(), c -> new FlagBlockEntityRenderer());
        ClientRegistrationHooks.registerBlockEntityRenderers(ModBlockEntityTypes.SLIDING_DOOR.get(), c -> new SlidingDoorBlockEntityRenderer());
    }

    // Entity renderers are registered in platform-specific code (Fabric/NeoForge)
    // because EntityRenderers.register() is not accessible from the common module.

    @SuppressWarnings("unchecked")
    public static void registerArmor() {
        ClientPlatformUtils.ArmorFactory spaceSuitFactory = (root, slot, stack, parentModel) ->
            new SpaceSuitModel(root, slot, stack, (HumanoidModel<HumanoidRenderState>) parentModel);
        ClientPlatformUtils.registerArmor(SpaceSuitModel.SPACE_SUIT_TEXTURE, SpaceSuitModel.SPACE_SUIT_LAYER, spaceSuitFactory,
            ModItems.SPACE_HELMET.get(), ModItems.SPACE_SUIT.get(),
            ModItems.SPACE_PANTS.get(), ModItems.SPACE_BOOTS.get());
        ClientPlatformUtils.registerArmor(SpaceSuitModel.NETHERITE_SPACE_SUIT_TEXTURE, SpaceSuitModel.NETHERITE_SPACE_SUIT_LAYER, spaceSuitFactory,
            ModItems.NETHERITE_SPACE_HELMET.get(), ModItems.NETHERITE_SPACE_SUIT.get(),
            ModItems.NETHERITE_SPACE_PANTS.get(), ModItems.NETHERITE_SPACE_BOOTS.get());
        ClientPlatformUtils.registerArmor(SpaceSuitModel.JET_SUIT_TEXTURE, SpaceSuitModel.JET_SUIT_LAYER, spaceSuitFactory,
            ModItems.JET_SUIT_HELMET.get(), ModItems.JET_SUIT.get(),
            ModItems.JET_SUIT_PANTS.get(), ModItems.JET_SUIT_BOOTS.get());
    }

    public static void onRegisterEntityLayers(ClientPlatformUtils.LayerDefinitionRegistry consumer) {
        consumer.register(RoverModel.LAYER, RoverModel::createBodyLayer);
        RocketModel.register(consumer);
        consumer.register(LanderModel.LAYER, LanderModel::createBodyLayer);
        SpaceSuitModel.register(consumer);

        // Block entity model layers
        consumer.register(GlobeCubeModel.LAYER, GlobeCubeModel::createBodyLayer);
        consumer.register(OxygenDistributorTopModel.LAYER, OxygenDistributorTopModel::createBodyLayer);
        consumer.register(GravityNormalizerTopModel.LAYER, GravityNormalizerTopModel::createBodyLayer);
        consumer.register(GravityNormalizerToeModel.LAYER, GravityNormalizerToeModel::createBodyLayer);

        consumer.register(LunarianModel.LAYER_LOCATION, LunarianModel::createBodyLayer);
        consumer.register(CorruptedLunarianModel.LAYER_LOCATION, CorruptedLunarianModel::createBodyLayer);
        consumer.register(StarCrawlerModel.LAYER_LOCATION, StarCrawlerModel::createBodyLayer);
        consumer.register(MartianRaptorModel.LAYER_LOCATION, MartianRaptorModel::createBodyLayer);
        consumer.register(PygroModel.LAYER_LOCATION, PygroModel::createBodyLayer);
        consumer.register(PygroBruteModel.LAYER_LOCATION, PygroBruteModel::createBodyLayer);
        consumer.register(ZombifiedPygroModel.LAYER_LOCATION, ZombifiedPygroModel::createBodyLayer);
        consumer.register(MoglerModel.LAYER_LOCATION, MoglerModel::createBodyLayer);
        consumer.register(SulfurCreeperModel.LAYER_LOCATION, SulfurCreeperModel::createBodyLayer);
        consumer.register(GlacianRamModel.LAYER_LOCATION, GlacianRamModel::createBodyLayer);
    }

    private static void registerItemProperties() {
        ClientRegistrationHooks.registerItemProperty(ModItems.ETRIONIC_CAPACITOR.get(), Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "toggled"), (stack, level, entity, i) -> EtrionicCapacitorItem.active(stack) ? 0 : 1);
    }

    public static void registerRenderLayers() {
        // In 1.21.11, block render layers are determined by block model JSON, not set programmatically.
        // These calls are no-ops but kept for documentation of which blocks need cutout render type.
        ClientRegistrationHooks.setRenderLayer(ModBlocks.VENT.get(), "cutout");
        ClientRegistrationHooks.setRenderLayer(ModBlocks.STEEL_DOOR.get(), "cutout");
        ClientRegistrationHooks.setRenderLayer(ModBlocks.STEEL_TRAPDOOR.get(), "cutout");
        ClientRegistrationHooks.setRenderLayer(ModBlocks.AERONOS_LADDER.get(), "cutout");
        ClientRegistrationHooks.setRenderLayer(ModBlocks.STROPHAR_LADDER.get(), "cutout");
        ClientRegistrationHooks.setRenderLayer(ModBlocks.GLACIAN_TRAPDOOR.get(), "cutout");
    }

    public static void onRegisterParticles(BiConsumer<ParticleType<SimpleParticleType>, ClientPlatformUtils.SpriteParticleRegistration<SimpleParticleType>> consumer) {
        consumer.accept(ModParticleTypes.ACID_RAIN.get(), SplashParticle.Provider::new);
        consumer.accept(ModParticleTypes.LARGE_FLAME.get(), LargeFlameParticle.Provider::new);
        consumer.accept(ModParticleTypes.LARGE_SMOKE.get(), LargeFlameParticle.Provider::new);
        consumer.accept(ModParticleTypes.OXYGEN_BUBBLE.get(), OxygenBubbleParticle.Provider::new);
    }

    public static void onRegisterModels(Consumer<Identifier> consumer) {
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_flipped".formatted(ModBlocks.AIRLOCK.getId().getPath())));
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_flipped".formatted(ModBlocks.REINFORCED_DOOR.getId().getPath())));

        // Per-variant flipped models for the simple sliding doors (iron / steel / desh / ostrum / calorite).
        // These models live as JSONs but aren't reachable from any blockstate, so they must be registered
        // as extra-baked models or `ClientPlatformUtils.getModel(...)` returns null and the renderer
        // falls back to a rotated copy of the front-facing model — which is what was producing the
        // washed-out / wrong-color second door panel reported by the user.
        ModBlocks.SIMPLE_SLIDING_DOORS.stream().forEach(block ->
            consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_flipped".formatted(block.getId().getPath()))));

        // Globe cube models (per-planet, with correct textures baked in)
        ModBlocks.GLOBES.stream().forEach(block ->
            consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_cube".formatted(block.getId().getPath()))));

        // Oxygen distributor spinning top
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/oxygen_distributor_top"));

        // Gravity normalizer spinning top and toe pieces
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/gravity_normalizer_top"));
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/gravity_normalizer_toe"));
    }

    /**
     * Functional interface for custom item renderers, replacing the removed BlockEntityWithoutLevelRenderer.
     */
    @FunctionalInterface
    public interface CustomItemRenderer {
        void renderByItem(ItemStack stack, ItemDisplayContext displayContext, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay);
    }

    public static void onRegisterItemRenderers(BiConsumer<Item, CustomItemRenderer> consumer) {
        ModItems.GLOBES.stream().forEach(item -> {
            var renderer = new GlobeBlockEntityRenderer.ItemRenderer();
            consumer.accept(item.get(), renderer::renderByItem);
        });
        var oxygenRenderer = new OxygenDistributorBlockEntityRenderer.ItemRenderer();
        consumer.accept(ModItems.OXYGEN_DISTRIBUTOR.get(), oxygenRenderer::renderByItem);
        var gravityRenderer = new GravityNormalizerBlockEntityRenderer.ItemRenderer();
        consumer.accept(ModItems.GRAVITY_NORMALIZER.get(), gravityRenderer::renderByItem);
        // TODO: 1.21.11 - BuiltinItemRendererRegistry removed. Rover and rocket item
        // rendering needs minecraft:special model type or alternative approach.
        // Items use flat placeholder textures in creative menu for now.
    }

    public static void onRegisterHud(Consumer<ClientPlatformUtils.RenderHud> consumer) {
        consumer.accept(OverlayScreen::render);
    }

    // TODO: 1.21.11 - ItemColor no longer exists. Item tinting is now data-driven via ItemTintSource.
    // This method needs to be reworked to use the new data-driven tinting system.
    // For now, using a local functional interface to maintain the same signature.
    @FunctionalInterface
    public interface ItemColorFunction {
        int getColor(ItemStack stack, int tintIndex);
    }

    public static void onAddItemColors(BiConsumer<ItemColorFunction, ItemLike[]> consumer) {
        // Default color must include full alpha (0xFF000000) or items will be invisible.
        // DyedItemColor.getOrDefault() only calls ARGB32.opaque() when the dye component exists,
        // but passes through the raw default value when it doesn't, so alpha=0 (0x00FFFFFF) causes
        // the ItemRenderer to render fully transparent items.
        consumer.accept((stack, i) -> i > 0 ? -1 : DyedItemColor.getOrDefault(stack, 0xFFFFFFFF), new ItemLike[]{ModItems.SPACE_HELMET.get(), ModItems.SPACE_SUIT.get(), ModItems.SPACE_PANTS.get(), ModItems.SPACE_BOOTS.get()});
        consumer.accept((stack, i) -> i > 0 ? -1 : DyedItemColor.getOrDefault(stack, 0xFFFFFFFF), new ItemLike[]{ModItems.NETHERITE_SPACE_HELMET.get(), ModItems.NETHERITE_SPACE_SUIT.get(), ModItems.NETHERITE_SPACE_PANTS.get(), ModItems.NETHERITE_SPACE_BOOTS.get()});
        consumer.accept((stack, i) -> i > 0 ? -1 : DyedItemColor.getOrDefault(stack, 0xFFFFFFFF), new ItemLike[]{ModItems.JET_SUIT_HELMET.get(), ModItems.JET_SUIT.get(), ModItems.JET_SUIT_PANTS.get(), ModItems.JET_SUIT_BOOTS.get()});
    }

    public static void renderOverlays(PoseStack stack, Camera camera) {
        OXYGEN_OVERLAY_RENDERER.render(stack, camera);
        GRAVITY_OVERLAY_RENDERER.render(stack, camera);
    }

    public static void onAddReloadListener(BiConsumer<Identifier, PreparableReloadListener> consumer) {
        consumer.accept(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planet_renderers"), new AdAstraPlanetRenderers());
    }

    public static void clientTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (KEY_OPEN_RADIO.consumeClick() && player.getVehicle() instanceof RadioHolder) {
            RadioHandler.open(null);
        }

        // Auto third-person camera for vehicles that request it (lander, rocket)
        if (player.getVehicle() instanceof Vehicle vehicle && vehicle.zoomOutCameraInThirdPerson()) {
            if (!forcedThirdPerson && minecraft.options.getCameraType().isFirstPerson()) {
                previousCameraType = minecraft.options.getCameraType();
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                forcedThirdPerson = true;
            }
        } else if (forcedThirdPerson) {
            if (previousCameraType != null) {
                minecraft.options.setCameraType(previousCameraType);
            }
            forcedThirdPerson = false;
            previousCameraType = null;
        }

        boolean wearingJetSuit = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItemTags.JET_SUITS);
        boolean ridingVehicle = player.getVehicle() instanceof Vehicle;

        if (wearingJetSuit) {
            if (KEY_TOGGLE_SUIT_FLIGHT.consumeClick()) {
                AdAstraConfigClient.jetSuitEnabled = !AdAstraConfigClient.jetSuitEnabled;
                Minecraft.getInstance().execute(() -> AdAstra.CONFIGURATOR.saveConfig(AdAstraConfigClient.class));
                player.sendSystemMessage(AdAstraConfigClient.jetSuitEnabled ? ConstantComponents.SUIT_FLIGHT_ENABLED : ConstantComponents.SUIT_FLIGHT_DISABLED);
            }
        }

        if (wearingJetSuit || ridingVehicle) {
            Options options = minecraft.options;

            KeybindManager.set(player,
                options.keyJump.isDown(),
                options.keySprint.isDown(),
                wearingJetSuit && AdAstraConfigClient.jetSuitEnabled);

            NetworkHandler.CHANNEL.sendToServer(new ServerboundSyncKeybindPacket(
                options.keyJump.isDown(),
                options.keySprint.isDown(),
                wearingJetSuit && AdAstraConfigClient.jetSuitEnabled
            ));

            // Client-side immediate visual feedback for jet flight. The server-side
            // velocity is still authoritative (applied in JetSuitItem.inventoryTick),
            // but the ~50–100 ms round-trip latency between key press → server-tick
            // boost → motion-packet-back made the suit feel like it "stuttered"
            // before lifting off. Spawning particles here at clientTick (20 Hz)
            // gives the player instant confirmation that the jet is firing.
            // Conditions inside spawnParticles still gate on suitFlightEnabled +
            // jumpDown + canFly, so no particles for normal vanilla jumping.
            if (wearingJetSuit) {
                ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                if (chest.getItem() instanceof JetSuitItem suit) {
                    suit.spawnParticles(player.level(), player, chest);
                }
            }
        } else if (KeybindManager.hasAnyKeyDown(player)) {
            // Clear stale keybind state on dismount to prevent jet suit launch
            KeybindManager.set(player, false, false, false);
            NetworkHandler.CHANNEL.sendToServer(new ServerboundSyncKeybindPacket(false, false, false));
        }
    }
}