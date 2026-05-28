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
 * Entity model equivalent of the gravity_normalizer_toe.json block model.
 * Texture: 32x32 (gravity_normalizer.png)
 *
 * Single element: from [6,5,1] to [10,10,2] = 4x5x1
 * Rotation: -45 degrees around X at origin [8, 7, 3.5] in block space.
 * Entity model origin at block (8, 0, 8) via renderer translate(0.5, 0, 0.5).
 * +Y goes DOWN in entity model space.
 */
public class GravityNormalizerToeModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "gravity_normalizer_toe"), "main");

    public GravityNormalizerToeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // JSON rotation pivot in block space: (8, 7, 3.5)
        // Relative to entity origin (8, 0, 8): x=0, z=3.5-8=-4.5
        // Entity Y: block Y=7 -> entity Y=-7
        //
        // The bone sits at the pivot point and has the -45 deg X rotation.
        // In entity model, block -45 deg X rotation -> +45 deg entity X rotation (PI/4)
        // because Y axis is inverted.
        PartDefinition bone = partDefinition.addOrReplaceChild("bone",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0f, -7.0f, -4.5f, (float)(Math.PI / 4), 0.0f, 0.0f));

        // Cube relative to rotation pivot:
        // Block space relative to pivot (8, 7, 3.5):
        //   x: 6-8=-2 to 10-8=2, width=4
        //   z: 1-3.5=-2.5 to 2-3.5=-1.5, depth=1
        //   blockY: 5-7=-2 (bottom) to 10-7=3 (top), height=5
        //
        // In entity model (Y inverted, relative to bone):
        //   entityY top: -(10-7) = -3
        //   addBox y=-3, h=5 (extends from -3 to 2)
        bone.addOrReplaceChild("toe",
            CubeListBuilder.create()
                .texOffs(7, 3)
                .addBox(-2.0f, -3.0f, -2.5f, 4.0f, 5.0f, 1.0f),
            PartPose.ZERO);

        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
