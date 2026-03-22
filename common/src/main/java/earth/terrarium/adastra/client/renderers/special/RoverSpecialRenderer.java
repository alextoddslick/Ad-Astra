package earth.terrarium.adastra.client.renderers.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import earth.terrarium.adastra.client.models.entities.vehicles.RoverModel;
import earth.terrarium.adastra.client.renderers.entities.vehicles.RoverRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class RoverSpecialRenderer implements NoDataSpecialModelRenderer {

    private final EntityModel<EntityRenderState> model;
    private final RenderType renderType;

    public RoverSpecialRenderer(ModelPart root) {
        this.model = new RoverModel(root);
        this.renderType = model.renderType(RoverRenderer.TEXTURE);
    }

    @Override
    public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int color) {
        poseStack.pushPose();
        poseStack.scale(1.0f, -1.0f, -1.0f);

        collector.submitModelPart(model.root(), poseStack, renderType, light, OverlayTexture.NO_OVERLAY, null);

        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        PoseStack poseStack = new PoseStack();
        poseStack.scale(1.0f, -1.0f, -1.0f);
        model.root().getExtentsForGui(poseStack, consumer);
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            var root = context.entityModelSet().bakeLayer(RoverModel.LAYER);
            return new RoverSpecialRenderer(root);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
