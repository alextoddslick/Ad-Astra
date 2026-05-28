package earth.terrarium.adastra.client.renderers.entities.mobs;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.mobs.StarCrawlerModel;
import earth.terrarium.adastra.common.entities.mob.StarCrawler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class StarCrawlerRenderer extends MobRenderer<StarCrawler, LivingEntityRenderState, StarCrawlerModel> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/star_crawler.png");

    public StarCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new StarCrawlerModel(context.bakeLayer(StarCrawlerModel.LAYER_LOCATION)), 0.0f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public @NotNull Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
