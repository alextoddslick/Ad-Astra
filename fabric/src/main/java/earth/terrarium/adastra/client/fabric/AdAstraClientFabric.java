package earth.terrarium.adastra.client.fabric;

import earth.terrarium.adastra.client.AdAstraClient;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import earth.terrarium.adastra.client.renderers.special.ModSpecialRenderers;
import earth.terrarium.adastra.client.models.entities.mobs.*;
import earth.terrarium.adastra.client.models.entities.vehicles.LanderModel;
import earth.terrarium.adastra.client.models.entities.vehicles.RocketModel;
import earth.terrarium.adastra.client.renderers.entities.mobs.*;
import earth.terrarium.adastra.client.renderers.entities.vehicles.LanderRenderer;
import earth.terrarium.adastra.client.renderers.entities.vehicles.RocketRenderer;
import earth.terrarium.adastra.client.renderers.entities.vehicles.RoverRenderer;
import earth.terrarium.adastra.client.utils.DimensionRenderingUtils;
import earth.terrarium.adastra.common.registry.ModBlocks;
import earth.terrarium.adastra.common.registry.ModEntityTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AdAstraClientFabric {

    static final Map<Identifier, ExtraModelKey<BlockStateModel>> EXTRA_MODELS = new HashMap<>();

    public static void init() {
        ModSpecialRenderers.register(SpecialModelRenderers.ID_MAPPER);
        registerExtraModels();
        AdAstraClient.init();
        onAddReloadListener();
        ClientTickEvents.START_CLIENT_TICK.register(AdAstraClient::clientTick);
        KeyBindingHelper.registerKeyBinding(AdAstraClient.KEY_TOGGLE_SUIT_FLIGHT);
        KeyBindingHelper.registerKeyBinding(AdAstraClient.KEY_OPEN_RADIO);
        AdAstraClient.onRegisterParticles((particle, provider) -> ParticleFactoryRegistry.getInstance().register(particle, provider::create));
        AdAstraClient.onRegisterEntityLayers((location, definition) -> EntityModelLayerRegistry.registerModelLayer(location, definition::get));
        AdAstraClient.onRegisterHud(hud -> HudRenderCallback.EVENT.register((graphics, tickCounter) -> hud.renderHud(graphics, tickCounter.getGameTimeDeltaPartialTick(false))));
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            var poseStack = ctx.matrices();
            if (poseStack != null) {
                AdAstraClient.renderOverlays(poseStack, camera);
            }
        });
        registerEntityRenderers();

        BlockRenderLayerMap.putBlock(ModBlocks.SOLAR_PANEL.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.WATER_PUMP.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ENERGIZER.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ETRIONIC_BLAST_FURNACE.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.VENT.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.STEEL_DOOR.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.STEEL_TRAPDOOR.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.AERONOS_LADDER.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.STROPHAR_LADDER.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.GLACIAN_TRAPDOOR.get(), ChunkSectionLayer.CUTOUT);
        ModBlocks.GLOBES.stream().forEach(block -> BlockRenderLayerMap.putBlock(block.get(), ChunkSectionLayer.CUTOUT));
        ModBlocks.SLIDING_DOORS.stream().forEach(block -> BlockRenderLayerMap.putBlock(block.get(), ChunkSectionLayer.CUTOUT));
        ModBlocks.INDUSTRIAL_LAMPS.stream().forEach(block -> BlockRenderLayerMap.putBlock(block.get(), ChunkSectionLayer.CUTOUT));
        ModBlocks.SMALL_INDUSTRIAL_LAMPS.stream().forEach(block -> BlockRenderLayerMap.putBlock(block.get(), ChunkSectionLayer.CUTOUT));
    }

    @SuppressWarnings("unchecked")
    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(ModEntityTypes.AIR_VORTEX.get(), NoopRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.ROVER.get(), RoverRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.TIER_1_ROCKET.get(), c -> new RocketRenderer(c, RocketModel.TIER_1_LAYER, RocketRenderer.TIER_1_TEXTURE));
        EntityRendererRegistry.register(ModEntityTypes.TIER_2_ROCKET.get(), c -> new RocketRenderer(c, RocketModel.TIER_2_LAYER, RocketRenderer.TIER_2_TEXTURE));
        EntityRendererRegistry.register(ModEntityTypes.TIER_3_ROCKET.get(), c -> new RocketRenderer(c, RocketModel.TIER_3_LAYER, RocketRenderer.TIER_3_TEXTURE));
        EntityRendererRegistry.register(ModEntityTypes.TIER_4_ROCKET.get(), c -> new RocketRenderer(c, RocketModel.TIER_4_LAYER, RocketRenderer.TIER_4_TEXTURE));
        EntityRendererRegistry.register(ModEntityTypes.LANDER.get(), c -> new LanderRenderer(c, LanderModel.LAYER));
        EntityRendererRegistry.register(ModEntityTypes.LUNARIAN.get(), LunarianRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.CORRUPTED_LUNARIAN.get(), CorruptedLunarianRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.STAR_CRAWLER.get(), StarCrawlerRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.MARTIAN_RAPTOR.get(), MartianRaptorRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.PYGRO.get(), PygroRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.ZOMBIFIED_PYGRO.get(), ZombifiedPygroRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.PYGRO_BRUTE.get(), PygroBruteRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.MOGLER.get(), MoglerRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.ZOMBIFIED_MOGLER.get(), ZombifiedMoglerRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.SULFUR_CREEPER.get(), SulfurCreeperRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.LUNARIAN_WANDERING_TRADER.get(), LunarianWanderingTraderRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.GLACIAN_RAM.get(), GlacianRamRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.ICE_SPIT.get(), ThrownItemRenderer::new);
    }

    public static void onAddReloadListener() {
        AdAstraClient.onAddReloadListener((id, listener) -> ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id;
            }

            @Override
            public @NotNull CompletableFuture<Void> reload(@NotNull PreparableReloadListener.SharedState sharedState, @NotNull Executor prepareExecutor, PreparableReloadListener.@NotNull PreparationBarrier synchronizer, @NotNull Executor applyExecutor) {
                return listener.reload(sharedState, prepareExecutor, synchronizer, applyExecutor);
            }
        }));
    }

    private static void registerExtraModels() {
        ModelLoadingPlugin.register(context -> {
            AdAstraClient.onRegisterModels(id -> {
                ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(() -> id.toString());
                context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id));
                EXTRA_MODELS.put(id, key);
            });
        });
    }

    public static void registerDimensionEffects(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        // TODO: 1.21.11 - DimensionRenderingRegistry was removed from Fabric API.
    }
}
