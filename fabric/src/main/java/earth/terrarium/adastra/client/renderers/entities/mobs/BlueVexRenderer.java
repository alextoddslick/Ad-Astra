package earth.terrarium.adastra.client.renderers.entities.mobs;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the {@link earth.terrarium.adastra.common.entities.mob.BlueVex} by reusing the vanilla Vex
 * model/layers ({@code ModelLayers.VEX}) with a blue Ad Astra texture.
 */
public class BlueVexRenderer extends VexRenderer {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/blue_vex.png");

    public BlueVexRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return TEXTURE;
    }
}
