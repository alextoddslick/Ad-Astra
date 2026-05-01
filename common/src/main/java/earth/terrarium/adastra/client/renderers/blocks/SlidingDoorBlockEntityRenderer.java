package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.blockentities.SlidingDoorBlockEntity;
import earth.terrarium.adastra.common.blocks.SlidingDoorBlock;
import earth.terrarium.adastra.common.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SlidingDoorBlockEntityRenderer implements BlockEntityRenderer<SlidingDoorBlockEntity, SlidingDoorBlockEntityRenderer.SlidingDoorRenderState> {

    public static class SlidingDoorRenderState extends BlockEntityRenderState {
        public float slide;
        public Direction direction;
        public boolean flipSecondDoor;
    }

    @Override
    public SlidingDoorRenderState createRenderState() {
        return new SlidingDoorRenderState();
    }

    @Override
    public void extractRenderState(SlidingDoorBlockEntity entity, SlidingDoorRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.slide = Mth.lerp(partialTick, entity.lastSlideTicks(), entity.slideTicks()) / 81.0f;
        state.direction = entity.getBlockState().getValue(SlidingDoorBlock.FACING);
        state.flipSecondDoor = ModBlocks.SIMPLE_SLIDING_DOORS.stream().noneMatch(block -> block.get().equals(entity.getBlockState().getBlock()));
    }

    @Override
    public void submit(SlidingDoorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.blockState == null || state.direction == null) return;
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getBlockRenderer().getBlockModel(state.blockState);
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5f, 1, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.direction.toYRot()));
        poseStack.translate(-0.5f, 0, -0.5f);

        poseStack.translate(state.slide, 0, 0.0625f);
        if (state.direction.getAxis() == Direction.Axis.Z) {
            poseStack.translate(0, 0, 0.6875f);
            if (state.blockState.is(ModBlocks.REINFORCED_DOOR.get())) {
                poseStack.translate(0, 0, -0.3125f);
            }
        }

        // First panel: render via the standard block model (vanilla blockstate already supplied it).
        // Use submitCustomGeometry + ModelBlockRenderer.renderModel — same pattern as
        // OxygenDistributorBlockEntityRenderer, which is the known-working precedent for drawing
        // an arbitrary BlockStateModel into the cutout block sheet.
        final var firstPanelModel = model;
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) ->
            ModelBlockRenderer.renderModel(pose, consumer, firstPanelModel, 1, 1, 1, light, OverlayTexture.NO_OVERLAY));

        poseStack.translate(-state.slide - state.slide, 0, 0);

        // Second panel: prefer a baked `<id>_flipped` model when one exists (registered as an
        // extra model in AdAstraClient.onRegisterModels). Falls back to a 180°-rotated copy of
        // the front model for blocks without a flipped variant.
        String blockId = BuiltInRegistries.BLOCK.getKey(state.blockState.getBlock()).getPath();
        BlockStateModel flippedModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(),
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_flipped".formatted(blockId)));

        if (flippedModel != null) {
            poseStack.translate(-1.25f, 0, 0);
            final var secondPanelModel = flippedModel;
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) ->
                ModelBlockRenderer.renderModel(pose, consumer, secondPanelModel, 1, 1, 1, light, OverlayTexture.NO_OVERLAY));
        } else if (!state.flipSecondDoor) {
            poseStack.translate(0.5f, 0, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-0.5f, 0, -0.5f);
            poseStack.translate(0, 0, 0.8125f);

            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) ->
                ModelBlockRenderer.renderModel(pose, consumer, model, 1, 1, 1, light, OverlayTexture.NO_OVERLAY));
        }

        poseStack.popPose();
    }
}
