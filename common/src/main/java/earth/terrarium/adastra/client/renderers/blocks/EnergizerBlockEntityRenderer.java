package earth.terrarium.adastra.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import earth.terrarium.adastra.common.blockentities.machines.EnergizerBlockEntity;
import earth.terrarium.adastra.common.blocks.base.MachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;


public class EnergizerBlockEntityRenderer implements BlockEntityRenderer<EnergizerBlockEntity, EnergizerBlockEntityRenderer.EnergizerRenderState> {

    public static class EnergizerRenderState extends BlockEntityRenderState {
        public boolean active;
        public double yOffset;
        public float rotation;
        public int lightAbove;
        public final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    }

    @Override
    public EnergizerRenderState createRenderState() {
        return new EnergizerRenderState();
    }

    @Override
    public void extractRenderState(EnergizerBlockEntity entity, EnergizerRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        var stack = entity.getItem(0);
        state.active = !stack.isEmpty();

        state.itemRenderState.clear();
        if (state.active && entity.getLevel() != null) {
            long gameTime = entity.getLevel().getGameTime();
            state.yOffset = Math.sin((gameTime + partialTick) / 8.0) / 8.0;
            state.rotation = (gameTime + partialTick) * 4;
            state.lightAbove = LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().above());

            ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
            resolver.updateForTopItem(state.itemRenderState, stack, ItemDisplayContext.GROUND, entity.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(EnergizerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.active || state.itemRenderState.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.6 + state.yOffset, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation));
        state.itemRenderState.submit(poseStack, collector, state.lightAbove, 0, -1);
        poseStack.popPose();
    }
}
