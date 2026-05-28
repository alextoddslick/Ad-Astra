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
 * Entity model equivalent of the globe_cube.json block model.
 * A 12x12x12 cube positioned from [2,6,2] to [14,18,14] in block space.
 * Texture: 64x64, per-globe planet textures.
 */
public class GlobeCubeModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "globe_cube"), "main");

    public GlobeCubeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // JSON block model: from [2,6,2] to [14,18,14] = 12x12x12 cube
        // Entity model origin is at block center XZ (8,0,8) via renderer translate(0.5, 0, 0.5).
        // Entity model Y-axis: +Y goes DOWN in world space (no flip applied).
        //
        // X/Z offsets from origin (8,8): from 2-8=-6 to 14-8=6
        // Y: block Y=6 to Y=18 (upward). In entity model (Y inverted, origin at floor Y=0):
        //   top of cube (block Y=18) -> entity Y = -18
        //   bottom of cube (block Y=6) -> entity Y = -6
        //   addBox y = -18, height = 12 (extends from -18 to -6)
        partDefinition.addOrReplaceChild("cube",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, -18.0f, -6.0f, 12.0f, 12.0f, 12.0f),
            PartPose.ZERO);

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}
