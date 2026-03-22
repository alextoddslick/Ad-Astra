package earth.terrarium.adastra.client.renderers.entities.vehicles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.vehicles.RocketModel;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
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

public class RocketRenderer extends EntityRenderer<Rocket, EntityRenderState> {

    public static final Identifier TIER_1_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/rocket/tier_1_rocket.png");
    public static final Identifier TIER_2_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/rocket/tier_2_rocket.png");
    public static final Identifier TIER_3_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/rocket/tier_3_rocket.png");
    public static final Identifier TIER_4_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/rocket/tier_4_rocket.png");

    protected final EntityModel<EntityRenderState> model;
    private final RenderType renderType;

    public RocketRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, Identifier texture) {
        super(context);
        this.shadowRadius = 0.5f;
        this.model = new RocketModel(context.bakeLayer(layer));
        this.renderType = model.renderType(texture);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.55f, 0.0f);
        poseStack.scale(-1.0f, -1.0f, 1.0f);

        collector.submitModelPart(model.root(), poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
