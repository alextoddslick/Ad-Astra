package earth.terrarium.adastra.client.fabric;

import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public class ClientPlatformUtilsImpl {

    public static BlockStateModel getModel(ModelManager dispatcher, Identifier id) {
        var key = AdAstraClientFabric.EXTRA_MODELS.get(id);
        if (key == null) return null;
        return dispatcher.getModel(key);
    }

    @SuppressWarnings("unchecked")
    public static void registerArmor(Identifier texture, ModelLayerLocation layer, ClientPlatformUtils.ArmorFactory factory, Item... items) {
        ArmorRenderer.register(context -> {
            ModelPart root = context.bakeLayer(layer);
            return (matrices, submitNodeCollector, stack, renderState, slot, light, contextModel) -> {
                HumanoidModel<HumanoidRenderState> armorModel = (HumanoidModel<HumanoidRenderState>) factory.create(
                    root, slot, stack, (HumanoidModel<HumanoidRenderState>) contextModel
                );

                // Copy transforms from the parent model to the armor model, then render
                armorModel.setupAnim(renderState);
                RenderType renderLayer = armorModel.renderType(texture);
                ArmorRenderer.submitTransformCopyingModel(
                    contextModel, renderState,     // source model + state
                    armorModel, renderState,        // delegate model + state
                    false,                          // don't call setupAnim again on delegate
                    submitNodeCollector,            // SubmitNodeCollector
                    matrices,                       // PoseStack
                    renderLayer,                    // RenderType
                    light,                          // light
                    OverlayTexture.NO_OVERLAY,      // overlay
                    -1,                             // tintedColor (0xFFFFFFFF = white/no tint)
                    null,                           // sprite (null = use render layer texture)
                    renderState.outlineColor,       // outlineColor from entity render state
                    null                            // no crumbling overlay
                );
            };
        }, items);
    }

    public static void registerPlanetRenderers(Map<ResourceKey<Level>, ModDimensionSpecialEffects> renderers) {
        AdAstraClientFabric.registerDimensionEffects(renderers);
    }

    public static Map<ResourceKey<Level>, ModDimensionSpecialEffects> getPlanetRenderers() {
        return AdAstraClientFabric.DIMENSION_RENDERERS;
    }
}
