package earth.terrarium.adastra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import earth.terrarium.adastra.client.fabric.AdAstraClientFabric;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
     * 26.1.2 helper: collect the model's BlockStateModelParts and submit them to the
     * SubmitNodeCollector for rendering. Replaces the removed {@code ModelBlockRenderer.renderModel}
     * call shape used by BE renderers. Renders to {@link Sheets#cutoutBlockSheet()}.
     */
    public static void submitBlockModel(BlockStateModel model, PoseStack poseStack,
                                        SubmitNodeCollector collector, int light) {
        submitBlockModel(model, poseStack, collector, Sheets.cutoutBlockSheet(), light,
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

    /**
     * 26.1.2 immediate-mode helper: render a {@link BlockStateModel} directly to a
     * {@link MultiBufferSource}. Used by item / item-frame / dropped-item render paths
     * which still hand us a {@code MultiBufferSource} rather than a
     * {@link SubmitNodeCollector}. Iterates each part's {@link BakedQuad}s and pushes
     * them into the {@link VertexConsumer} for {@link Sheets#cutoutBlockSheet()}.
     */
    public static void renderBlockModelImmediate(BlockStateModel model, PoseStack poseStack,
                                                 MultiBufferSource buffer, int packedLight,
                                                 int packedOverlay) {
        renderBlockModelImmediate(model, poseStack, buffer, Sheets.cutoutBlockSheet(),
            packedLight, packedOverlay);
    }

    public static void renderBlockModelImmediate(BlockStateModel model, PoseStack poseStack,
                                                 MultiBufferSource buffer, RenderType renderType,
                                                 int packedLight, int packedOverlay) {
        if (model == null) return;
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        if (parts.isEmpty()) return;

        VertexConsumer vc = buffer.getBuffer(renderType);
        QuadInstance quadInstance = new QuadInstance();
        // White (untinted) per-vertex color; quads with a tintIndex would normally pull
        // their color from a tint source, but for BE item rendering the default white
        // works for all of these models (globe, oxygen distributor, gravity normalizer).
        quadInstance.setColor(0xFFFFFFFF);
        quadInstance.setLightCoords(packedLight);
        quadInstance.setOverlayCoords(packedOverlay);

        PoseStack.Pose pose = poseStack.last();
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    vc.putBakedQuad(pose, quad, quadInstance);
                }
            }
            for (BakedQuad quad : part.getQuads(null)) {
                vc.putBakedQuad(pose, quad, quadInstance);
            }
        }
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
                RenderType renderLayer = armorModel.renderType(texture);
                ArmorRenderer.submitTransformCopyingModel(
                    contextModel, renderState,     // source model + state
                    armorModel, renderState,        // delegate model + state
                    false,                          // don't call setupAnim again on delegate
                    submitNodeCollector,            // SubmitNodeCollector
                    matrices,                       // PoseStack
                    renderLayer,                    // RenderType
                    light,                          // light
                    OverlayTexture.NO_OVERLAY,      // overlay
                    -1,                             // tintedColor (0xFFFFFFFF = white/no tint)
                    null,                           // sprite (null = use render layer texture)
                    renderState.outlineColor,       // outlineColor from entity render state
                    null                            // no crumbling overlay
                );
            };
        }, items);
    }

    public static void registerPlanetRenderers(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        AdAstraClientFabric.registerDimensionEffects(renderers);
    }

    public static Map<ResourceKey<Level>, ModDimensionSpecialEffects> getPlanetRenderers() {
        return AdAstraClientFabric.DIMENSION_RENDERERS;
    }
}
