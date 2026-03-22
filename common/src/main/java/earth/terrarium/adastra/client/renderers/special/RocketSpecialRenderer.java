package earth.terrarium.adastra.client.renderers.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import earth.terrarium.adastra.client.models.entities.vehicles.RocketModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.function.Consumer;

public class RocketSpecialRenderer implements NoDataSpecialModelRenderer {

    private static final Map<String, ModelLayerLocation> LAYER_MAP = Map.of(
        "ad_astra:tier_1_rocket/main", RocketModel.TIER_1_LAYER,
        "ad_astra:tier_2_rocket/main", RocketModel.TIER_2_LAYER,
        "ad_astra:tier_3_rocket/main", RocketModel.TIER_3_LAYER,
        "ad_astra:tier_4_rocket/main", RocketModel.TIER_4_LAYER
    );

    private final EntityModel<EntityRenderState> model;
    private final RenderType renderType;

    public RocketSpecialRenderer(EntityModel<EntityRenderState> model, Identifier texture) {
        this.model = model;
        this.renderType = model.renderType(texture);
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

    public record Unbaked(Identifier texture, String layer) implements SpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
                com.mojang.serialization.Codec.STRING.fieldOf("layer").forGetter(Unbaked::layer)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            ModelLayerLocation layerLocation = LAYER_MAP.get(layer);
            if (layerLocation == null) layerLocation = RocketModel.TIER_1_LAYER;
            var root = context.entityModelSet().bakeLayer(layerLocation);
            var model = new RocketModel(root);
            return new RocketSpecialRenderer(model, texture);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
