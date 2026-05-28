package earth.terrarium.adastra.common.utils.floodfill;

import earth.terrarium.adastra.common.blocks.SlidingDoorBlock;
import earth.terrarium.adastra.common.blocks.base.MachineBlock;
import earth.terrarium.adastra.common.blocks.pipes.TransferablePipe;
import earth.terrarium.adastra.common.tags.ModBlockTags;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FloodFill3D {

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final SolidBlockPredicate TEST_FULL_SEAL = (level, pos, state, positions, queue, direction) -> {
        if (state.isAir()) return true;

        if (state.is(ModBlockTags.PASSES_FLOOD_FILL)) return true;
        if (state.is(ModBlockTags.BLOCKS_FLOOD_FILL)) return false;

        if (state.isCollisionShapeFullBlock(level, pos)) return false;

        // Liquid blocks should block oxygen
        if (state.getBlock() instanceof LiquidBlock) return false;
        if (!state.getFluidState().isEmpty()) return false;

        // Open sliding doors let oxygen through, closed ones block it
        if (state.getBlock() instanceof SlidingDoorBlock) {
            return state.getValue(SlidingDoorBlock.OPEN) || state.getValue(SlidingDoorBlock.POWERED);
        }

        // Cables, pipes, and machines block oxygen
        if (state.getBlock() instanceof TransferablePipe) return false;
        if (state.getBlock() instanceof MachineBlock) return false;

        // Non-vanilla blocks with collision block oxygen
        VoxelShape earlyCollision = state.getCollisionShape(level, pos);
        if (!earlyCollision.isEmpty()) {
            var blockKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!blockKey.getNamespace().equals("minecraft")) return false;
        }

        // Minecraft's face sturdiness check for partial blocks
        Direction entryFace = direction.getOpposite();
        if (state.isFaceSturdy(level, pos, entryFace, SupportType.FULL)) {
            positions.add(pos.asLong());
            return false;
        }

        // Fallback: glass panes and thin blocks that cover the full face
        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        if (!collisionShape.isEmpty() && coversFullFace(collisionShape, direction.getAxis())) {
            positions.add(pos.asLong());
            return false;
        }

        return true;
    };

    public static Set<BlockPos> run(Level level, BlockPos start, int limit, SolidBlockPredicate predicate, boolean retainOrder) {
        net.minecraft.util.profiling.Profiler.get().push("adastra-floodfill");

        LongSet positions = retainOrder ? new LongLinkedOpenHashSet(limit) : new LongOpenHashSet(limit);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue(limit);
        queue.enqueue(start.asLong());

        while (!queue.isEmpty() && positions.size() < limit) {
            long packedPos = queue.dequeueLong();
            if (positions.contains(packedPos)) continue;
            positions.add(packedPos);

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(BlockPos.getX(packedPos), BlockPos.getY(packedPos), BlockPos.getZ(packedPos));
            for (Direction direction : DIRECTIONS) {
                pos.set(packedPos);
                pos.move(direction);
                BlockState state = level.getBlockState(pos);
                if (!predicate.test(level, pos, state, positions, queue, direction)) continue;
                queue.enqueue(pos.asLong());
            }
        }

        Set<BlockPos> result = retainOrder ? new LinkedHashSet<>(positions.size()) : new HashSet<>(positions.size());
        for (long pos : positions) {
            result.add(BlockPos.of(pos));
        }

        net.minecraft.util.profiling.Profiler.get().pop();
        return result;
    }

    private static boolean coversFullFace(VoxelShape shape, Direction.Axis movementAxis) {
        return switch (movementAxis) {
            case X -> shape.min(Direction.Axis.Y) <= 0 && shape.max(Direction.Axis.Y) >= 1
                    && shape.min(Direction.Axis.Z) <= 0 && shape.max(Direction.Axis.Z) >= 1;
            case Y -> shape.min(Direction.Axis.X) <= 0 && shape.max(Direction.Axis.X) >= 1
                    && shape.min(Direction.Axis.Z) <= 0 && shape.max(Direction.Axis.Z) >= 1;
            case Z -> shape.min(Direction.Axis.X) <= 0 && shape.max(Direction.Axis.X) >= 1
                    && shape.min(Direction.Axis.Y) <= 0 && shape.max(Direction.Axis.Y) >= 1;
        };
    }

    @FunctionalInterface
    public interface SolidBlockPredicate {
        boolean test(Level level, BlockPos pos, BlockState state, LongSet positions, LongArrayFIFOQueue queue, Direction direction);
    }
}
