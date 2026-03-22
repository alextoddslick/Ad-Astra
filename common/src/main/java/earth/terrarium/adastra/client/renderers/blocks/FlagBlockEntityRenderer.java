package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import earth.terrarium.adastra.client.renderers.textures.FlagImageTexture;
import earth.terrarium.adastra.client.renderers.textures.FlagUrlTexture;
import earth.terrarium.adastra.common.blockentities.flag.FlagBlockEntity;
import earth.terrarium.adastra.common.blockentities.flag.content.FlagContent;
import earth.terrarium.adastra.common.blockentities.flag.content.ImageContent;
import earth.terrarium.adastra.common.blockentities.flag.content.UrlContent;
import earth.terrarium.adastra.common.blocks.FlagBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class FlagBlockEntityRenderer implements BlockEntityRenderer<FlagBlockEntity, FlagBlockEntityRenderer.FlagRenderState> {

    public static class FlagRenderState extends BlockEntityRenderState {
        public float facingRotation;
        public boolean isLowerHalf;
        public FlagContent content;
        public Identifier skinTextureId;
    }

    @Override
    public FlagRenderState createRenderState() {
        return new FlagRenderState();
    }

    @Override
    public void extractRenderState(FlagBlockEntity entity, FlagRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.facingRotation = entity.getBlockState().getValue(FlagBlock.FACING).asRotation();
        state.isLowerHalf = entity.getBlockState().getValue(FlagBlock.HALF) == DoubleBlockHalf.LOWER;
        state.content = entity.getContent();
        state.skinTextureId = null;

        if (!state.isLowerHalf && state.content == null && entity.getOwner() != null) {
            // Resolve the player skin texture on the main thread
            var skinSupplier = Minecraft.getInstance().getSkinManager().createLookup(entity.getOwner(), false);
            var skin = skinSupplier.get();
            if (skin.body() != null) {
                state.skinTextureId = skin.body().texturePath();
            }
        }
    }

    @Override
    public void submit(FlagRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.blockState == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facingRotation));
        poseStack.translate(-0.5, 0, -0.5);

        if (state.isLowerHalf) {
            var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state.blockState);
            collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), model, 1, 1, 1, state.lightCoords, 0, -1);
        } else {
            if (state.content == null) {
                // No content - render player head
                if (state.skinTextureId != null) {
                    var minecraft = Minecraft.getInstance();
                    SkullModelBase skullModel = new SkullModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_HEAD));
                    RenderType renderType = SkullBlockRenderer.getPlayerSkinRenderType(state.skinTextureId);

                    poseStack.pushPose();
                    poseStack.translate(-0.75, 0.19, 0.495);
                    poseStack.scale(1, 1, 0.1f / 16f);
                    SkullBlockRenderer.submitSkull(null, 0, 0, poseStack, collector,
                        state.lightCoords, skullModel, renderType, -1, state.breakProgress);

                    poseStack.translate(0.5, 0, 0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    poseStack.translate(-0.5, 0, -0.5);
                    poseStack.translate(0, 0, -1);
                    SkullBlockRenderer.submitSkull(null, 0, 0, poseStack, collector,
                        state.lightCoords, skullModel, renderType, -1, state.breakProgress);
                    poseStack.popPose();
                }
            } else {
                // URL/image content - render textured quad
                RenderType flagRenderType = getFlagImage(state.content);

                poseStack.pushPose();
                poseStack.translate(0.5, 0, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.translate(-0.5, 0, -0.5);
                poseStack.scale(1 + 5.8f / 16f, 1, 1);
                poseStack.translate(-11f / 16f, -15f / 16f, 0.495);

                // Front face
                collector.submitCustomGeometry(poseStack, flagRenderType, (pose, consumer) ->
                    renderQuad(pose, consumer, state.lightCoords));

                poseStack.translate(0.5, 0, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.translate(-0.5, 0, -0.5);
                poseStack.translate(0, 0, 0.99);

                // Back face
                collector.submitCustomGeometry(poseStack, flagRenderType, (pose, consumer) ->
                    renderQuad(pose, consumer, state.lightCoords));

                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer, int light) {
        consumer.addVertex(pose, 0, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(0).setUv2(light & 0xFFFF, light >> 16 & 0xFFFF).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, 0, 1, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(0).setUv2(light & 0xFFFF, light >> 16 & 0xFFFF).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, 1, 1, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(0).setUv2(light & 0xFFFF, light >> 16 & 0xFFFF).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, 1, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(0).setUv2(light & 0xFFFF, light >> 16 & 0xFFFF).setNormal(pose, 0, 0, -1);
    }

    private static RenderType getFlagImage(FlagContent content) {
        Identifier id = content.toTexture();
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = manager.getTexture(id);
        if (texture == null) {
            if (content instanceof UrlContent url) {
                manager.register(id, new FlagUrlTexture(url.url()));
            } else if (content instanceof ImageContent image) {
                manager.register(id, new FlagImageTexture(image.data()));
            }
        }
        return RenderTypes.entitySolid(id);
    }
}
