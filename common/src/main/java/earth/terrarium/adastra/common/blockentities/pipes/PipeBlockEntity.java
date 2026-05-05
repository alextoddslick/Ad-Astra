package earth.terrarium.adastra.common.blockentities.pipes;

import earth.terrarium.adastra.common.blockentities.base.TickableBlockEntity;
import earth.terrarium.adastra.common.blocks.base.BasicEntityBlock;
import earth.terrarium.adastra.common.blocks.pipes.PipeBlock;
import earth.terrarium.adastra.common.blocks.pipes.TransferablePipe;
import earth.terrarium.adastra.common.config.MachineConfig;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class PipeBlockEntity extends BlockEntity implements TickableBlockEntity, Pipe {

    protected final Map<BlockPos, Direction> sources = new HashMap<>();
    protected final Map<BlockPos, Direction> consumers = new HashMap<>();
    private final long transferRate;

    @Nullable
    private Direction[] connectedDirections;
    private boolean initialized;
    private boolean isController;
    private boolean isCanonicalController;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(((BasicEntityBlock) state.getBlock()).entity(state), pos, state);
        PipeBlock pipe = ((PipeBlock) state.getBlock());
        this.transferRate = pipe.transferRate();
    }

    @Override
    public void serverTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        if (isController) {
            if (time % MachineConfig.pipeRefreshRate == 0) {
                sources.clear();
                consumers.clear();
                findNodes(level, pos);
                isCanonicalController = computeIsCanonicalController(level, pos);
            }
            if (isCanonicalController && !consumers.isEmpty() && !sources.isEmpty()) {
                transfer(level, transferRate, sources, consumers);
            }
        }
    }

    /**
     * Returns true if this pipe is the lowest-positioned controller in its connected
     * network. Without this, every controller (every pipe touching a non-pipe block)
     * runs its own transfer cycle each tick, so a 2-cable run between a generator and
     * a consumer would transfer 2× the cable's rated throughput. Designating one
     * canonical controller per network keeps total throughput equal to the rated rate.
     */
    private boolean computeIsCanonicalController(ServerLevel level, BlockPos myPos) {
        long myLong = myPos.asLong();
        long min = myLong;
        LongSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(myLong);
        visited.add(myLong);
        while (!queue.isEmpty()) {
            long curLong = queue.dequeueLong();
            BlockPos cur = BlockPos.of(curLong);
            if (!(level.getBlockEntity(cur) instanceof PipeBlockEntity pipeEntity)) continue;
            if (pipeEntity.isController && curLong < min) min = curLong;
            Direction[] dirs = pipeEntity.connectedDirections();
            if (dirs == null) continue;
            for (var d : dirs) {
                BlockPos next = cur.relative(d);
                long nextLong = next.asLong();
                if (!visited.add(nextLong)) continue;
                if (level.getBlockState(next).getBlock() instanceof TransferablePipe) {
                    queue.enqueue(nextLong);
                }
            }
        }
        return myLong == min;
    }

    public void pipeChanged(Level level, BlockPos pos) {
        Direction[] directions = PipeBlock.getConnectedDirections(level.getBlockState(pos));
        connectedDirections = directions;
        for (var direction : directions) {
            if (!(level.getBlockState(pos.relative(direction)).getBlock() instanceof PipeBlock)) {
                isController = true;
                return;
            }
        }
        isController = false;
    }

    @Override
    public void firstTick(Level level, BlockPos pos, BlockState state) {
        this.initialized = true;
        pipeChanged(level, pos);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Nullable
    public Direction[] connectedDirections() {
        return connectedDirections;
    }
}
