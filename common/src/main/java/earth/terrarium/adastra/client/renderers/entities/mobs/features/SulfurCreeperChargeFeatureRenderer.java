package earth.terrarium.adastra.client.renderers.entities.mobs.features;

import earth.terrarium.adastra.client.models.entities.mobs.SulfurCreeperModel;
import earth.terrarium.adastra.client.renderers.entities.mobs.SulfurCreeperRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.Identifier;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class SulfurCreeperChargeFeatureRenderer extends EnergySwirlLayer<SulfurCreeperRenderer.SulfurCreeperRenderState, SulfurCreeperModel<SulfurCreeperRenderer.SulfurCreeperRenderState>> {

    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private final SulfurCreeperModel<SulfurCreeperRenderer.SulfurCreeperRenderState> model;

    public SulfurCreeperChargeFeatureRenderer(RenderLayerParent<SulfurCreeperRenderer.SulfurCreeperRenderState, SulfurCreeperModel<SulfurCreeperRenderer.SulfurCreeperRenderState>> context, EntityModelSet loader) {
        super(context);
        this.model = new SulfurCreeperModel<>(loader.bakeLayer(SulfurCreeperModel.LAYER_LOCATION));
    }

    @Override
    protected boolean isPowered(SulfurCreeperRenderer.SulfurCreeperRenderState state) {
        return state.isPowered;
    }

    @Override
    protected float xOffset(float partialAge) {
        return partialAge * 0.01f;
    }

    @Override
    protected Identifier getTextureLocation() {
        return SKIN;
    }

    @Override
    protected SulfurCreeperModel<SulfurCreeperRenderer.SulfurCreeperRenderState> model() {
        return this.model;
    }
}
