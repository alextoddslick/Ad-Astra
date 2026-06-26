package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.blockentities.GlobeBlockEntity;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class GlobeBlockEntityRenderer implements BlockEntityRenderer<GlobeBlockEntity, GlobeBlockEntityRenderer.GlobeRenderState> {

    public GlobeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class GlobeRenderState extends BlockEntityRenderState {
        public float yRot;
        // 26.1.2 made BlockEntityRenderState#blockState private; mirror it here for rendering.
        public BlockState ourBlockState;
    }

    @Override
    public GlobeRenderState createRenderState() {
        return new GlobeRenderState();
    }

    @Override
    public void extractRenderState(GlobeBlockEntity entity, GlobeRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.yRot = Mth.lerp(partialTick, entity.lastYRot(), entity.yRot());
        state.ourBlockState = entity.getBlockState();
    }

    /**
     * Get the extra model ID for the globe cube based on the block state.
     * Models are registered as "block/<globe_name>_cube" (e.g., "block/earth_globe_cube").
     */
    private static Identifier getCubeModelId(BlockState blockState) {
        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
        return Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_cube".formatted(blockId));
    }

    @Override
    public void submit(GlobeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.ourBlockState == null) return;

        BlockStateModel cubeModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(), getCubeModelId(state.ourBlockState));
        if (cubeModel == null) return;

        int light = state.lightCoords;
        float yRot = state.yRot;

        // Apply rotation transform, then render the JSON block model via submitCustomGeometry.
        // The JSON block model has correct per-face UVs and embedded textures.
        PoseStack worldPose = new PoseStack();
        worldPose.last().pose().set(poseStack.last().pose());
        worldPose.last().normal().set(poseStack.last().normal());
        worldPose.translate(0.5, 0, 0.5);
        worldPose.mulPose(Axis.YP.rotationDegrees(-yRot));
        worldPose.translate(-0.5, 0, -0.5);

        ClientPlatformUtils.submitBlockModel(cubeModel, worldPose, collector, light);
    }

    static void renderStatic(BlockState blockState, float yRot, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        BlockStateModel cubeModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(), getCubeModelId(blockState));
        if (cubeModel == null) return;

        poseStack.pushPose();
        try {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            poseStack.translate(-0.5, 0, -0.5);

            ClientPlatformUtils.submitBlockModel(cubeModel, poseStack, collector, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    public static class ItemRenderer {

        public ItemRenderer() {
        }

        public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
            BlockState state = BuiltInRegistries.BLOCK.getValue(BuiltInRegistries.ITEM.getKey(stack.getItem())).defaultBlockState();

            var minecraft = Minecraft.getInstance();
            float yRot = Util.getMillis() / 20f % 360f;

            poseStack.pushPose();
            try {
                // Render the base block model
                var model = minecraft.getModelManager().getBlockStateModelSet().get(state);
                ClientPlatformUtils.submitBlockModel(model, poseStack, collector, packedLight);
                // Render the spinning globe cube
                renderStatic(state, yRot, poseStack, collector, packedLight, packedOverlay);
            } finally {
                poseStack.popPose();
            }
        }
    }
}
