package earth.terrarium.adastra.client.fabric;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.AdAstraClient;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import earth.terrarium.adastra.client.renderers.special.ModSpecialRenderers;
import earth.terrarium.adastra.client.screens.player.OverlayScreen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
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
import earth.terrarium.adastra.mixins.client.SpecialModelRenderersAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
// TODO 26.1.2: fabric-key-binding-api-v1 and fabric-rendering-v1.world were dropped
// from the fabric-api meta in 26.1. Key bindings and BEFORE_TRANSLUCENT overlay are
// stubbed out below; restore via mixins or new fabric modules when available.
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
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

    public static final Map<Identifier, ExtraModelKey<BlockStateModel>> EXTRA_MODELS = new HashMap<>();

    public static void init() {
        // 26.1.2: SpecialModelRenderers.ID_MAPPER is package-private; reach it through an accessor mixin.
        ModSpecialRenderers.register(SpecialModelRenderersAccessor.getIdMapper());
        registerExtraModels();
        AdAstraClient.init();
        onAddReloadListener();
        ClientTickEvents.START_CLIENT_TICK.register(AdAstraClient::clientTick);
        // TODO 26.1.2: KeyBindingHelper.registerKeyBinding(...) — fabric-key-binding-api-v1 missing.
        AdAstraClient.onRegisterParticles((particle, provider) -> ParticleProviderRegistry.getInstance().register(particle, provider::create));
        // 26.1.2: EntityModelLayerRegistry was renamed/relocated to ModelLayerRegistry.
        // The fabric-rendering-v1 module still exposes it; we adapt our internal Supplier-based
        // contract to its TexturedLayerDefinitionProvider (which is functionally identical).
        AdAstraClient.onRegisterEntityLayers((location, definition) ->
            ModelLayerRegistry.registerModelLayer(location, definition::get));
        // 26.1.2: HudRenderCallback was replaced by HudElementRegistry. Insert our
        // overlay just before the chat layer so it draws on top of the gameplay HUD
        // (rocket countdown, lander brake/distance prompt, oxygen bar, battery bar)
        // but underneath chat messages.
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "overlay"),
            (graphics, tickDelta) -> OverlayScreen.render(graphics, tickDelta.getGameTimeDeltaPartialTick(false))
        );
        // TODO 26.1.2: WorldRenderEvents.BEFORE_TRANSLUCENT — fabric-rendering-v1.world missing.
        // AdAstraClient.renderOverlays(...) call temporarily stubbed.
        registerEntityRenderers();

        // TODO 26.1.2: BlockRenderLayerMap was removed from fabric-rendering-v1. The vanilla
        // block JSON should declare {"render_type": "cutout"} per-state. Calls stubbed.
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

    public static final Map<ResourceKey<Level>, ModDimensionSpecialEffects> DIMENSION_RENDERERS = new HashMap<>();

    public static void registerDimensionEffects(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        // 1.21.11 - DimensionRenderingRegistry was removed from Fabric API. We hold the data here
        // and the SkyRendererMixin consumes it to draw planet sky renderables on top of vanilla sun/moon/stars.
        DIMENSION_RENDERERS.clear();
        DIMENSION_RENDERERS.putAll(renderers);
    }
}
