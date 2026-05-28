package earth.terrarium.adastra.client.models.blocks;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Entity model equivalent of the gravity_normalizer_top.json block model.
 * Texture: 32x32 (gravity_normalizer.png)
 *
 * Single element: from [5,9,5] to [11,15,11] = 6x6x6 cube.
 * No rotation on the element itself; the renderer applies complex rotations.
 * Entity model origin at block (8, 0, 8) via renderer translate(0.5, 0, 0.5).
 * +Y goes DOWN in entity model space.
 */
public class GravityNormalizerTopModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "gravity_normalizer_top"), "main");

    public GravityNormalizerTopModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // JSON: from [5,9,5] to [11,15,11] = 6x6x6
        // Relative to origin (8, 0, 8): x=5-8=-3, z=5-8=-3
        // Entity Y (inverted): top at blockY=15 -> entityY=-15, height=6
        // addBox(-3, -15, -3, 6, 6, 6)
        partDefinition.addOrReplaceChild("cube",
            CubeListBuilder.create()
                .texOffs(7, 0)
                .addBox(-3.0f, -15.0f, -3.0f, 6.0f, 6.0f, 6.0f),
            PartPose.ZERO);

        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
