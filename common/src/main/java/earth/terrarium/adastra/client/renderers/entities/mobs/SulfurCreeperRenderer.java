package earth.terrarium.adastra.client.renderers.entities.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.mobs.SulfurCreeperModel;
import earth.terrarium.adastra.client.renderers.entities.mobs.features.SulfurCreeperChargeFeatureRenderer;
import earth.terrarium.adastra.common.entities.mob.SulfurCreeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class SulfurCreeperRenderer extends MobRenderer<SulfurCreeper, SulfurCreeperRenderer.SulfurCreeperRenderState, SulfurCreeperModel<SulfurCreeperRenderer.SulfurCreeperRenderState>> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/sulfur_creeper.png");

    public static class SulfurCreeperRenderState extends LivingEntityRenderState {
        public float swelling;
        public boolean isPowered;
    }

    public SulfurCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new SulfurCreeperModel<>(context.bakeLayer(SulfurCreeperModel.LAYER_LOCATION)), 0.7f);
        this.addLayer(new SulfurCreeperChargeFeatureRenderer(this, context.getModelSet()));
    }

    @Override
    public SulfurCreeperRenderState createRenderState() {
        return new SulfurCreeperRenderState();
    }

    @Override
    public void extractRenderState(SulfurCreeper entity, SulfurCreeperRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.swelling = entity.getSwelling(partialTick);
        state.isPowered = entity.isPowered();
    }

    @Override
    protected void scale(SulfurCreeperRenderState state, PoseStack poseStack) {
        float swelling = state.swelling;
        float scale = 1.0f + Mth.sin(swelling * 100.0f) * swelling * 0.01f;
        swelling = Mth.clamp(swelling, 0.0f, 1.0f);
        swelling *= swelling;
        swelling *= swelling;
        float xzScale = (1.0f + swelling * 0.4f) * scale;
        float yScale = (1.0f + swelling * 0.1f) / scale;
        poseStack.scale(xzScale, yScale, xzScale);
    }

    @Override
    protected float getWhiteOverlayProgress(SulfurCreeperRenderState state) {
        float swelling = state.swelling;
        return (int) (swelling * 10.0f) % 2 == 0 ? 0.0f : Mth.clamp(swelling, 0.5f, 1.0f);
    }

    @Override
    public @NotNull Identifier getTextureLocation(SulfurCreeperRenderState state) {
        return TEXTURE;
    }
}
