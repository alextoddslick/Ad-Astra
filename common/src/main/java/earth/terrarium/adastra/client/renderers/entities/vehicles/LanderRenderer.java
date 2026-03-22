package earth.terrarium.adastra.client.renderers.entities.vehicles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.vehicles.LanderModel;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class LanderRenderer extends EntityRenderer<Lander, EntityRenderState> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/lander/lander.png");

    protected final EntityModel<EntityRenderState> model;
    private final RenderType renderType;

    public LanderRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context);
        this.shadowRadius = 0.5f;
        this.model = new LanderModel(context.bakeLayer(layer));
        this.renderType = model.renderType(TEXTURE);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.501f, 0.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        poseStack.translate(0.0f, -1.501f, 0.0f);

        collector.submitModelPart(model.root(), poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
