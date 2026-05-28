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
 * Entity model equivalent of the oxygen_distributor_top.json block model.
 * Texture: 32x32 (oxygen_distributor.png)
 *
 * All elements are grouped into two rotation groups around Y at block (8, 0, 8).
 * Entity model origin is at block (8, 0, 8) via renderer translate(0.5, 0, 0.5).
 * +Y goes DOWN in entity model space (no flip).
 */
public class OxygenDistributorTopModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "oxygen_distributor_top"), "main");

    public OxygenDistributorTopModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // All elements rotate around block (8, 0, 8) = entity origin (0, 0, 0).
        // Block Y maps to entity Y as: entityY = -blockY (since +Y is down in entity space).

        // Group 1: -45 degree Y rotation (elements 0-4)
        PartDefinition group1 = partDefinition.addOrReplaceChild("group1",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -(float)(Math.PI / 4), 0.0f));

        // Element 0: Central box from [4,5,4] to [12,15,12] = 8x10x8
        // x=4-8=-4, z=4-8=-4, entityY: top=-15, bottom=-5, addBox y=-15, h=10
        group1.addOrReplaceChild("central",
            CubeListBuilder.create()
                .texOffs(7, 11)
                .addBox(-4.0f, -15.0f, -4.0f, 8.0f, 10.0f, 8.0f),
            PartPose.ZERO);

        // Element 1: North arm from [4,6,0] to [12,14,2] = 8x8x2
        // x=-4, z=0-8=-8, entityY: top=-14, addBox y=-14, h=8
        group1.addOrReplaceChild("arm_n",
            CubeListBuilder.create()
                .texOffs(7, 0)
                .addBox(-4.0f, -14.0f, -8.0f, 8.0f, 8.0f, 2.0f),
            PartPose.ZERO);

        // Element 2: North connector from [6,8,1] to [10,12,5] = 4x4x4
        // x=6-8=-2, z=1-8=-7, entityY: top=-12, addBox y=-12, h=4
        group1.addOrReplaceChild("conn_n",
            CubeListBuilder.create()
                .texOffs(11, 4)
                .addBox(-2.0f, -12.0f, -7.0f, 4.0f, 4.0f, 4.0f),
            PartPose.ZERO);

        // Element 3: West arm from [0,6,4] to [2,14,12] = 2x8x8
        // x=0-8=-8, z=4-8=-4, entityY: top=-14, addBox y=-14, h=8
        group1.addOrReplaceChild("arm_w",
            CubeListBuilder.create()
                .texOffs(7, 0)
                .addBox(-8.0f, -14.0f, -4.0f, 2.0f, 8.0f, 8.0f),
            PartPose.ZERO);

        // Element 4: West connector from [1,8,6] to [5,12,10] = 4x4x4
        // x=1-8=-7, z=6-8=-2, entityY: top=-12, addBox y=-12, h=4
        group1.addOrReplaceChild("conn_w",
            CubeListBuilder.create()
                .texOffs(11, 4)
                .addBox(-7.0f, -12.0f, -2.0f, 4.0f, 4.0f, 4.0f),
            PartPose.ZERO);

        // Group 2: +45 degree Y rotation (elements 5-8)
        PartDefinition group2 = partDefinition.addOrReplaceChild("group2",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, (float)(Math.PI / 4), 0.0f));

        // Element 5: South arm from [4,6,14] to [12,14,16] = 8x8x2
        // x=-4, z=14-8=6, entityY: top=-14, addBox y=-14, h=8
        group2.addOrReplaceChild("arm_s",
            CubeListBuilder.create()
                .texOffs(7, 0)
                .addBox(-4.0f, -14.0f, 6.0f, 8.0f, 8.0f, 2.0f),
            PartPose.ZERO);

        // Element 6: South connector from [6,8,11] to [10,12,15] = 4x4x4
        // x=-2, z=11-8=3, entityY: top=-12, addBox y=-12, h=4
        group2.addOrReplaceChild("conn_s",
            CubeListBuilder.create()
                .texOffs(11, 4)
                .addBox(-2.0f, -12.0f, 3.0f, 4.0f, 4.0f, 4.0f),
            PartPose.ZERO);

        // Element 7: East arm from [0,6,4] to [2,14,12] = 2x8x8 (same geom as arm_w)
        // x=-8, z=-4, entityY: top=-14, addBox y=-14, h=8
        group2.addOrReplaceChild("arm_e",
            CubeListBuilder.create()
                .texOffs(7, 0)
                .addBox(-8.0f, -14.0f, -4.0f, 2.0f, 8.0f, 8.0f),
            PartPose.ZERO);

        // Element 8: East connector from [1,8,6] to [5,12,10] = 4x4x4
        // x=-7, z=-2, entityY: top=-12, addBox y=-12, h=4
        group2.addOrReplaceChild("conn_e",
            CubeListBuilder.create()
                .texOffs(11, 4)
                .addBox(-7.0f, -12.0f, -2.0f, 4.0f, 4.0f, 4.0f),
            PartPose.ZERO);

        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
