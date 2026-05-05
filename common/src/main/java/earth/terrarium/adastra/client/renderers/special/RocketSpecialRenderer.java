package earth.terrarium.adastra.client.renderers.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.client.model.geom.ModelPart;
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
        applyContextTransform(displayContext, poseStack);

        PoseStack worldPose = new PoseStack();
        worldPose.last().pose().set(poseStack.last().pose());
        worldPose.last().normal().set(poseStack.last().normal());
        worldPose.scale(1.0f, -1.0f, -1.0f);

        ModelPart root = model.root();
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            root.render(worldPose, consumer, 15728880, OverlayTexture.NO_OVERLAY);
        });

        poseStack.popPose();
    }

    private static void applyContextTransform(ItemDisplayContext ctx, PoseStack pose) {
        switch (ctx) {
            case GUI -> {
                pose.translate(0.5f, 0.45f, 0.5f);
                pose.mulPose(Axis.XP.rotationDegrees(-30));
                pose.mulPose(Axis.YP.rotationDegrees(225));
                pose.scale(0.22f, 0.22f, 0.22f);
            }
            case GROUND -> {
                pose.translate(0.5f, 0.3f, 0.5f);
                pose.scale(0.15f, 0.15f, 0.15f);
            }
            case FIXED -> {
                pose.translate(0.5f, 0.5f, 0.5f);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.scale(0.2f, 0.2f, 0.2f);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // Rocket model has its origin at the bottom; push high so it appears
                // above the camera, no rotation (the Y-flip in submit() already orients
                // the entity model upright relative to world).
                pose.translate(0.5f, 3.0f, 0.5f);
                pose.scale(1.0f, 1.0f, 1.0f);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                pose.translate(0.5f, 3.5f, 0.5f);
                pose.scale(1.0f, 1.0f, 1.0f);
            }
            case HEAD -> {
                pose.translate(0.5f, 0.5f, 0.5f);
                pose.scale(0.4f, 0.4f, 0.4f);
            }
            default -> {
                pose.translate(0.5f, 0.5f, 0.5f);
                pose.scale(0.2f, 0.2f, 0.2f);
            }
        }
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
