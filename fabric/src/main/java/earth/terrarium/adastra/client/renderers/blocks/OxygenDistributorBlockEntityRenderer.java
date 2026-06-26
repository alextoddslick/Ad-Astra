package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.common.blockentities.machines.OxygenDistributorBlockEntity;
import earth.terrarium.adastra.common.blocks.base.SidedMachineBlock;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

public class OxygenDistributorBlockEntityRenderer implements BlockEntityRenderer<OxygenDistributorBlockEntity, OxygenDistributorBlockEntityRenderer.OxygenDistributorRenderState> {

    public static final Identifier TOP = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "block/oxygen_distributor_top");

    public static class OxygenDistributorRenderState extends BlockEntityRenderState {
        public float yRot;
        public AttachFace face;
        public Direction direction;
    }

    public OxygenDistributorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public OxygenDistributorRenderState createRenderState() {
        return new OxygenDistributorRenderState();
    }

    @Override
    public void extractRenderState(OxygenDistributorBlockEntity entity, OxygenDistributorRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.yRot = Mth.lerp(partialTick, entity.lastYRot(), entity.yRot());
        state.face = entity.getBlockState().getValue(SidedMachineBlock.FACE);
        state.direction = entity.getBlockState().getValue(SidedMachineBlock.FACING);
    }

    @Override
    public void submit(OxygenDistributorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        BlockStateModel topModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(), TOP);
        if (topModel == null) return;

        int light = state.lightCoords;

        float faceXRot = switch (state.face) {
            case WALL -> 90f;
            case CEILING -> 180f;
            default -> 0f;
        };
        float facingYRot = switch (state.direction) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
        if (state.face == AttachFace.CEILING) facingYRot = (facingYRot + 180f) % 360f;

        PoseStack worldPose = new PoseStack();
        worldPose.last().pose().set(poseStack.last().pose());
        worldPose.last().normal().set(poseStack.last().normal());
        worldPose.translate(0.5, 0.5, 0.5);
        worldPose.mulPose(Axis.YP.rotationDegrees(facingYRot));
        worldPose.mulPose(Axis.XP.rotationDegrees(faceXRot));
        worldPose.translate(-0.5, -0.5, -0.5);
        worldPose.translate(0.5, 0, 0.5);
        worldPose.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        worldPose.translate(-0.5, 0, -0.5);

        ClientPlatformUtils.submitBlockModel(topModel, worldPose, collector, light);
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

    static void renderStatic(BlockState state, float yRot, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        BlockStateModel topModel = ClientPlatformUtils.getModel(
            Minecraft.getInstance().getModelManager(), TOP);
        if (topModel == null) return;

        poseStack.pushPose();
        try {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            poseStack.translate(-0.5, 0, -0.5);

            ClientPlatformUtils.submitBlockModel(topModel, poseStack, collector, packedLight);
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
            float yRot = Util.getMillis() / 5f % 360f;

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
