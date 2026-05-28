package earth.terrarium.adastra.client.renderers.entities.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.models.entities.mobs.LunarianModel;
import earth.terrarium.adastra.common.entities.mob.LunarianWanderingTrader;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class LunarianWanderingTraderRenderer extends MobRenderer<LunarianWanderingTrader, VillagerRenderState, LunarianModel> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/entity/mob/lunarian/lunarian_wandering_trader.png");

    public LunarianWanderingTraderRenderer(EntityRendererProvider.Context context) {
        super(context, new LunarianModel(context.bakeLayer(LunarianModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public @NotNull Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(VillagerRenderState state, PoseStack poseStack) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
