package earth.terrarium.adastra.client.renderers.entities.mobs;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.mobs.MoglerModel;
import earth.terrarium.adastra.common.entities.mob.Mogler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class MoglerRenderer extends MobRenderer<Mogler, MoglerRenderer.MoglerRenderState, MoglerModel<MoglerRenderer.MoglerRenderState>> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/mogler.png");

    public static class MoglerRenderState extends LivingEntityRenderState {
        public boolean isConverting;
    }

    public MoglerRenderer(EntityRendererProvider.Context context) {
        super(context, new MoglerModel<>(context.bakeLayer(MoglerModel.LAYER_LOCATION)), 0.7f);
    }

    @Override
    public MoglerRenderState createRenderState() {
        return new MoglerRenderState();
    }

    @Override
    public void extractRenderState(Mogler entity, MoglerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isConverting = entity.isConverting();
    }

    @Override
    public @NotNull Identifier getTextureLocation(MoglerRenderState state) {
        return TEXTURE;
    }

    @Override
    protected boolean isShaking(MoglerRenderState state) {
        return super.isShaking(state) || state.isConverting;
    }
}
