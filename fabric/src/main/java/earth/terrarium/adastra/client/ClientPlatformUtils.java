package earth.terrarium.adastra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import earth.terrarium.adastra.client.fabric.AdAstraClientFabric;
import earth.terrarium.adastra.client.models.armor.SpaceSuitModel;
import earth.terrarium.adastra.common.utils.UpgradeUtils;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClientPlatformUtils {

    @FunctionalInterface
    public interface SpriteParticleRegistration<T extends ParticleOptions> {

        ParticleProvider<T> create(SpriteSet spriteSet);
    }

    @FunctionalInterface
    public interface LayerDefinitionRegistry {

        void register(ModelLayerLocation location, Supplier<LayerDefinition> definition);
    }

    @FunctionalInterface
    public interface ArmorFactory {

        HumanoidModel<?> create(ModelPart root, EquipmentSlot slot, ItemStack stack, HumanoidModel<?> parentModel);
    }

    @FunctionalInterface
    public interface RenderHud {

        void renderHud(GuiGraphicsExtractor graphics, float partialTick);
    }

    public static BlockStateModel getModel(ModelManager dispatcher, Identifier id) {
        var key = AdAstraClientFabric.EXTRA_MODELS.get(id);
        if (key == null) return null;
        return dispatcher.getModel(key);
    }

    /**
     * 26.2 helper: collect the model's BlockStateModelParts and submit them to the
     * SubmitNodeCollector for rendering. Replaces the removed {@code ModelBlockRenderer.renderModel}
     * call shape used by BE renderers. Renders to the block cutout layer
     * ({@link RenderTypes#cutoutMovingBlock()}, the replacement for the removed
     * {@code Sheets.cutoutBlockSheet()}).
     */
    public static void submitBlockModel(BlockStateModel model, PoseStack poseStack,
                                        SubmitNodeCollector collector, int light) {
        submitBlockModel(model, poseStack, collector, RenderTypes.cutoutMovingBlock(), light,
            OverlayTexture.NO_OVERLAY);
    }

    public static void submitBlockModel(BlockStateModel model, PoseStack poseStack,
                                        SubmitNodeCollector collector, RenderType renderType,
                                        int light, int overlay) {
        if (model == null) return;
        List<BlockStateModelPart> parts = new ArrayList<>();
        // Use a stable seed; BE renderers don't have a position-based seed available here.
        model.collectParts(RandomSource.create(42L), parts);
        if (parts.isEmpty()) return;
        collector.submitBlockModel(poseStack, renderType, parts,
            BlockModelRenderState.EMPTY_TINTS, light, overlay, 0);
    }

    @SuppressWarnings("unchecked")
    public static void registerArmor(Identifier texture, ModelLayerLocation layer, ArmorFactory factory, Item... items) {
        ArmorRenderer.register(context -> {
            ModelPart root = context.bakeLayer(layer);
            return (matrices, submitNodeCollector, stack, renderState, slot, light, contextModel) -> {
                HumanoidModel<HumanoidRenderState> armorModel = (HumanoidModel<HumanoidRenderState>) factory.create(
                    root, slot, stack, (HumanoidModel<HumanoidRenderState>) contextModel
                );

                // Copy transforms from the parent model to the armor model, then render
                armorModel.setupAnim(renderState);
                // Swap to an upgrade-variant texture for the worn piece so NASA-Workbench upgrades are
                // visible in third person: boost boots get Etrium-blue soles, the analysis visor gets a
                // tinted visor. Each slot only draws its own parts, so swapping the base texture (rather
                // than a second overlay submit) shows the upgrade with no z-fighting.
                Identifier tex = texture;
                if (slot == EquipmentSlot.FEET && UpgradeUtils.hasBoostMode(stack)) {
                    tex = SpaceSuitModel.JET_SUIT_BOOST_TEXTURE;
                } else if (slot == EquipmentSlot.HEAD && UpgradeUtils.hasAnalysisVisor(stack)) {
                    tex = SpaceSuitModel.JET_SUIT_ANALYSIS_TEXTURE;
                }
                RenderType renderLayer = armorModel.renderType(tex);
                ArmorRenderer.submitTransformCopyingModel(
                    contextModel, renderState,     // source model + state
                    armorModel, renderState,        // delegate model + state
                    false,                          // don't call setupAnim again on delegate
                    submitNodeCollector,            // SubmitNodeCollector
                    matrices,                       // PoseStack
                    renderLayer,                    // RenderType
                    light,                          // light
                    OverlayTexture.NO_OVERLAY,      // overlay
                    vibrantDyeTint(stack),          // tintedColor: dye color (saturation-boosted) on worn suits; -1 if undyed
                    null,                           // sprite (null = use render layer texture)
                    renderState.outlineColor,       // outlineColor from entity render state
                    null                            // no crumbling overlay
                );
            };
        }, items);
    }

    /**
     * Tint color for a worn (dyeable) suit. Returns -1 (white / no tint) when undyed, otherwise the dye
     * color with its saturation pushed up so dyes read vividly on the suit textures instead of muted.
     */
    private static int vibrantDyeTint(ItemStack stack) {
        int color = DyedItemColor.getOrDefault(stack, 0xFFFFFFFF);
        if ((color & 0xFFFFFF) == 0xFFFFFF) return -1; // undyed default (white) -> no tint
        int r = ARGB.red(color), g = ARGB.green(color), b = ARGB.blue(color);
        float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        float sat = 1.7f; // push channels away from luminance to boost saturation
        r = Mth.clamp(Math.round(lum + (r - lum) * sat), 0, 255);
        g = Mth.clamp(Math.round(lum + (g - lum) * sat), 0, 255);
        b = Mth.clamp(Math.round(lum + (b - lum) * sat), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void registerPlanetRenderers(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        AdAstraClientFabric.registerDimensionEffects(renderers);
    }

    public static Map<ResourceKey<Level>, ModDimensionSpecialEffects> getPlanetRenderers() {
        return AdAstraClientFabric.DIMENSION_RENDERERS;
    }
}
