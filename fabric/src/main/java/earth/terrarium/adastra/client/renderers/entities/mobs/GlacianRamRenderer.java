package earth.terrarium.adastra.client.renderers.entities.mobs;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.mobs.GlacianRamModel;
import earth.terrarium.adastra.common.entities.mob.GlacianRam;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class GlacianRamRenderer extends MobRenderer<GlacianRam, GlacianRamRenderer.GlacianRamRenderState, GlacianRamModel<GlacianRamRenderer.GlacianRamRenderState>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/glacian_ram/glacian_ram.png");
    private static final Identifier SHEARED_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/glacian_ram/sheared_glacian_ram.png");

    public static class GlacianRamRenderState extends LivingEntityRenderState {
        public boolean isSheared;
    }

    public GlacianRamRenderer(EntityRendererProvider.Context context) {
        super(context, new GlacianRamModel<>(context.bakeLayer(GlacianRamModel.LAYER_LOCATION)), 0.7f);
    }

    @Override
    public GlacianRamRenderState createRenderState() {
        return new GlacianRamRenderState();
    }

    @Override
    public void extractRenderState(GlacianRam entity, GlacianRamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isSheared = entity.isSheared();
    }

    public @NotNull Identifier getTextureLocation(GlacianRamRenderState state) {
        return state.isSheared ? SHEARED_TEXTURE : TEXTURE;
    }
}
