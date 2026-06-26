package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.blockentities.machines.GravityNormalizerBlockEntity;
import earth.terrarium.adastra.common.blocks.base.SidedMachineBlock;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class GravityNormalizerBlockEntityRenderer implements BlockEntityRenderer<GravityNormalizerBlockEntity, GravityNormalizerBlockEntityRenderer.GravityNormalizerRenderState> {

    public static final Identifier TOP = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/gravity_normalizer_top");
    public static final Identifier TOE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/gravity_normalizer_toe");

    private static final float SIN_45 = (float) Math.sin(Math.PI / 4);

    public static class GravityNormalizerRenderState extends BlockEntityRenderState {
        public float animation;
        public AttachFace face;
        public Direction direction;
    }

    @Override
    public GravityNormalizerRenderState createRenderState() {
        return new GravityNormalizerRenderState();
    }

    @Override
    public void extractRenderState(GravityNormalizerBlockEntity entity, GravityNormalizerRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.animation = Mth.lerp(partialTick, entity.lastAnimation(), entity.animation());
        state.face = entity.getBlockState().getValue(SidedMachineBlock.FACE);
        state.direction = entity.getBlockState().getValue(SidedMachineBlock.FACING);
    }

    @Override
    public void submit(GravityNormalizerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        var minecraft = Minecraft.getInstance();
        BlockStateModel topModel = ClientPlatformUtils.getModel(minecraft.getModelManager(), TOP);
        BlockStateModel toeModel = ClientPlatformUtils.getModel(minecraft.getModelManager(), TOE);
        if (topModel == null || toeModel == null) return;

        int light = state.lightCoords;
        float yRot = state.animation / 1.2f;

        // Render the spinning top part with captured world transform
        {
            PoseStack topPose = new PoseStack();
            topPose.last().pose().set(poseStack.last().pose());
            topPose.last().normal().set(poseStack.last().normal());
            // Rotate around center of block at Y=0.7 (above base)
            topPose.translate(0.5, 0.7, 0.5);
            topPose.mulPose(Axis.XP.rotationDegrees(state.animation));
            topPose.mulPose(Axis.YP.rotationDegrees(state.animation));
            topPose.mulPose(Axis.ZP.rotationDegrees(state.animation));
            topPose.mulPose(Axis.YP.rotationDegrees(yRot));
            topPose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            topPose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            topPose.mulPose(Axis.YP.rotationDegrees(yRot));
            topPose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            topPose.mulPose(Axis.YP.rotationDegrees(yRot));
            // Translate back so model pixel origin aligns with block corner
            topPose.translate(-0.5, -0.7, -0.5);

            ClientPlatformUtils.submitBlockModel(topModel, topPose, collector, light);
        }

        // Render 4 toe parts with captured world transforms
        for (int i = 0; i < 4; i++) {
            PoseStack toePose = new PoseStack();
            toePose.last().pose().set(poseStack.last().pose());
            toePose.last().normal().set(poseStack.last().normal());
            toePose.translate(0.5, 0, 0.5);
            toePose.mulPose(Axis.YP.rotationDegrees(90 * i));
            toePose.translate(-0.5, 0, -0.5);
            toePose.translate(0.27, 0.27, 0.27);
            toePose.mulPose(Axis.XP.rotationDegrees(Mth.sin(state.animation / 50 + i) * 10));
            toePose.translate(-0.27, -0.27, -0.27);

            ClientPlatformUtils.submitBlockModel(toeModel, toePose, collector, light);
        }
    }

    // Taken from geckolib
    protected static void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poseStack.mulPose(Axis.XN.rotationDegrees(90));
        }
    }

    static void renderStatic(BlockState state, float animation, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        var minecraft = Minecraft.getInstance();
        BlockStateModel topModel = ClientPlatformUtils.getModel(minecraft.getModelManager(), TOP);
        BlockStateModel toeModel = ClientPlatformUtils.getModel(minecraft.getModelManager(), TOE);
        if (topModel == null || toeModel == null) return;

        poseStack.pushPose();
        try {
            poseStack.pushPose();
            poseStack.translate(0.5, 0, 0.5);

            poseStack.mulPose(Axis.XP.rotationDegrees(animation));
            poseStack.mulPose(Axis.YP.rotationDegrees(animation));
            poseStack.mulPose(Axis.ZP.rotationDegrees(animation));

            float yRot = animation / 1.2f;
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            poseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0, SIN_45));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

            poseStack.translate(-0.5, 0, -0.5);

            ClientPlatformUtils.submitBlockModel(topModel, poseStack, collector, packedLight);
            poseStack.popPose();

            for (int i = 0; i < 4; i++) {
                poseStack.pushPose();

                poseStack.translate(0.5, 0, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(90 * i));
                poseStack.translate(-0.5, 0, -0.5);

                poseStack.translate(0.27, 0.27, 0.27);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(animation / 50 + i) * 10));
                poseStack.translate(-0.27, -0.27, -0.27);

                ClientPlatformUtils.submitBlockModel(toeModel, poseStack, collector, packedLight);

                poseStack.popPose();
            }
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
            float yRot = Util.getMillis() / 5f;

            poseStack.pushPose();
            try {
                var model = minecraft.getModelManager().getBlockStateModelSet().get(state);
                ClientPlatformUtils.submitBlockModel(model, poseStack, collector, packedLight);
                renderStatic(state, yRot, poseStack, collector, packedLight, packedOverlay);
            } finally {
                poseStack.popPose();
            }
        }
    }
}
