package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.blockentities.SlidingDoorBlockEntity;
import earth.terrarium.adastra.common.blocks.SlidingDoorBlock;
import earth.terrarium.adastra.common.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
        // 26.1.2: BlockEntityRenderState#blockState is private; mirror it here.
        public net.minecraft.world.level.block.state.BlockState ourBlockState;
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
        state.ourBlockState = entity.getBlockState();
    }

    @Override
    public void submit(SlidingDoorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.ourBlockState == null || state.direction == null) return;
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getBlockStateModelSet().get(state.ourBlockState);
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5f, 1, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.direction.toYRot()));
        poseStack.translate(-0.5f, 0, -0.5f);

        poseStack.translate(state.slide, 0, 0.0625f);
        if (state.direction.getAxis() == Direction.Axis.Z) {
            poseStack.translate(0, 0, 0.6875f);
            if (state.ourBlockState.is(ModBlocks.REINFORCED_DOOR.get())) {
                poseStack.translate(0, 0, -0.3125f);
            }
        }

        // First panel: render via the standard block model (vanilla blockstate already supplied it).
        ClientPlatformUtils.submitBlockModel(model, poseStack, collector, light);

        poseStack.translate(-state.slide - state.slide, 0, 0);

        // Second panel: prefer a baked `<id>_flipped` model when one exists (registered as an
        // extra model in AdAstraClient.onRegisterModels). Falls back to a 180°-rotated copy of
        // the front model for blocks without a flipped variant.
        String blockId = BuiltInRegistries.BLOCK.getKey(state.ourBlockState.getBlock()).getPath();
        BlockStateModel flippedModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(),
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/%s_flipped".formatted(blockId)));

        if (flippedModel != null) {
            poseStack.translate(-1.25f, 0, 0);
            ClientPlatformUtils.submitBlockModel(flippedModel, poseStack, collector, light);
        } else if (!state.flipSecondDoor) {
            poseStack.translate(0.5f, 0, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-0.5f, 0, -0.5f);
            poseStack.translate(0, 0, 0.8125f);

            ClientPlatformUtils.submitBlockModel(model, poseStack, collector, light);
        }

        poseStack.popPose();
    }
}
