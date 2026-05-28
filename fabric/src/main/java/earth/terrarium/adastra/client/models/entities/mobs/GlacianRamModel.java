package earth.terrarium.adastra.client.models.entities.mobs;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class GlacianRamModel<T extends LivingEntityRenderState> extends QuadrupedModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "glacian_ram"), "main");
    public GlacianRamModel(ModelPart modelPart) {
        super(modelPart);
    }

    @SuppressWarnings("unused")
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(3, 19).addBox(-3.5F, -3.8F, -2.9F, 7.0F, 6.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(50, 53).addBox(1.5F, -5.8F, -0.9F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(32, 52).addBox(3.5F, -5.8F, 1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(51, 56).addBox(-5.5F, -5.8F, 1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(52, 18).addBox(-5.5F, -5.8F, -0.9F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 12.8F, -1.9F, 0.0F, 3.1416F, 0.0F));

        PartDefinition ear_left = head.addOrReplaceChild("ear_left", CubeListBuilder.create().texOffs(21, 38).addBox(-3.3927F, -0.7456F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9417F, -2.791F, 1.35F, 0.0F, 0.0F, -0.9599F));

        PartDefinition ear_right = head.addOrReplaceChild("ear_right", CubeListBuilder.create().texOffs(21, 38).mirror().addBox(-0.6073F, -0.7456F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.9417F, -2.791F, 1.35F, 0.0F, 0.0F, 0.9599F));

        PartDefinition left_front_leg = partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 54).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 20.0F, 0.5F));

        PartDefinition right_front_leg = partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(12, 30).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 20.0F, 0.5F));

        PartDefinition left_hind_leg = partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 54).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 20.0F, 9.5F));

        PartDefinition right_hind_leg = partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(12, 30).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 20.0F, 9.5F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40).addBox(-1.25F, -2.8333F, -9.1667F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(0, 40).addBox(5.75F, 1.1667F, -2.1667F, 0.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-5.25F, -6.8333F, -2.1667F, 11.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(27, 27).addBox(-5.25F, -4.8333F, -7.1667F, 11.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(0, 40).addBox(-5.25F, 1.1667F, -2.1667F, 0.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 18.8333F, 5.0333F, 0.0F, 3.1416F, 0.0F));

        PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 40).addBox(13.0F, -1.0F, 3.0F, 0.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.75F, 2.1667F, -5.1667F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);
        this.head.y = 12.0f; // TODO: restore getNeckAngle from render state
        this.head.z = -2.0f;
        this.head.xRot = 0.0f; // TODO: restore getHeadAngle from render state
    }

}
