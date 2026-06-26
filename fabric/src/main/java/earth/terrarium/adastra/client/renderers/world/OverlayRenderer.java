package earth.terrarium.adastra.client.renderers.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class OverlayRenderer {

    private final Map<BlockPos, Set<BlockPos>> positions = new HashMap<>();
    private final int color;
    private final BooleanSupplier config;
    private final Supplier<Block> block;

    public OverlayRenderer(int color, BooleanSupplier config, Supplier<Block> block) {
        this.color = color;
        this.config = config;
        this.block = block;
    }

    public void addPositions(BlockPos pos, Set<BlockPos> positions) {
        this.positions.put(pos, positions);
    }

    public void removePositions(BlockPos pos) {
        positions.remove(pos);
    }

    public void clearPositions() {
        positions.clear();
    }

    public boolean canAdd(BlockPos pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.blockPosition().closerThan(pos, 128);
    }

    public void render(PoseStack poseStack, Camera camera) {
        if (!config.getAsBoolean()) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;
        if (level.getGameTime() % 40 == 0) {
            positions.keySet().removeIf(pos -> !level.isLoaded(pos)
                || !canAdd(pos)
                || !(level.getBlockState(pos).is(block.get()))
            );
        }

        // TODO 26.2: re-enable the in-world area overlay once the world-render hook is back.
        // This is invoked from AdAstraClient.renderOverlays(), whose caller in AdAstraClientFabric
        // is itself stubbed (WorldRenderEvents.BEFORE_TRANSLUCENT — fabric-rendering-v1.world
        // missing since the 26.1.2 port). In 26.2 the immediate MultiBufferSource was removed
        // (Minecraft.renderBuffers()/bufferSource() are gone), so the draw path below needs to be
        // re-expressed against the new submit API (SubmitNodeCollector) threaded from that hook.
        // The cube-building geometry in renderCube() is preserved for that re-wiring.
        // poseStack.pushPose();
        // var consumer = /* SubmitNodeCollector-backed VertexConsumer for RenderTypes.debugQuads() */;
        // poseStack.translate(-camera.position().x(), -camera.position().y(), -camera.position().z());
        // positions.values().forEach(positions -> positions.forEach(pos ->
        //     renderCube(poseStack, consumer, pos, positions)));
        // poseStack.popPose();
    }

    private void renderCube(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, Set<BlockPos> others) {
        Matrix4f matrix = poseStack.last().pose();

        int minX = pos.getX();
        int minY = pos.getY();
        int minZ = pos.getZ();
        int maxX = minX + 1;
        int maxY = minY + 1;
        int maxZ = minZ + 1;

        // Bottom Face
        if (!others.contains(pos.below())) {
            consumer.addVertex(matrix, minX, minY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(color);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(color);
        }

        // Top Face
        if (!others.contains(pos.above())) {
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(color);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(color);
        }

        // North Face
        if (!others.contains(pos.north())) {
            consumer.addVertex(matrix, minX, minY, minZ).setColor(color);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(color);
        }

        // South Face
        if (!others.contains(pos.south())) {
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(color);
        }

        // East Face
        if (!others.contains(pos.east())) {
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(color);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(color);
        }

        // West Face
        if (!others.contains(pos.west())) {
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(color);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(color);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(color);
            consumer.addVertex(matrix, minX, minY, minZ).setColor(color);
        }
    }
}
