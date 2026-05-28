package earth.terrarium.adastra.common.blockentities.pipes;

import earth.terrarium.adastra.common.blocks.properties.PipeProperty;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FluidPipeBlockEntity extends PipeBlockEntity {

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void addNode(@NotNull BlockEntity entity, PipeProperty pipeProperty, Direction direction, BlockPos pos) {
        if (pipeProperty.isNone()) return;
        CommonStorage<FluidResource> container = FluidApi.BLOCK.find(entity.getLevel(), pos, direction.getOpposite());
        if (container == null) return;

        if (pipeProperty.isExtract()) {
            sources.put(pos, direction);
        } else if (pipeProperty.isInsert()) {
            consumers.put(pos, direction);
        } else {
            // Normal mode: blocks with fluid are sources, blocks with room are consumers
            // A block can be both (e.g. oxygen distributor has input + output tanks)
            boolean hasFluid = canExtractFluid(container);
            boolean hasRoom = canInsertFluid(container);
            if (hasFluid) sources.put(pos, direction);
            if (hasRoom) consumers.put(pos, direction);
        }
    }

    private boolean canInsertFluid(CommonStorage<FluidResource> container) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).getAmount() < container.get(i).getLimit(container.get(i).getResource())) return true;
        }
        return false;
    }

    private boolean canExtractFluid(CommonStorage<FluidResource> container) {
        for (int i = 0; i < container.size(); i++) {
            FluidResource resource = container.get(i).getResource();
            if (resource.isBlank()) continue;
            if (container.extract(resource, 1, true) > 0) return true;
        }
        return false;
    }

    @Override
    public void moveContents(long transferRate, @NotNull BlockEntity source, @NotNull BlockEntity consumer, Direction sourceDirection, Direction consumerDirection) {
        CommonStorage<FluidResource> sourceStorage = FluidApi.BLOCK.find(source.getLevel(), source.getBlockPos(), sourceDirection.getOpposite());
        CommonStorage<FluidResource> consumerStorage = FluidApi.BLOCK.find(consumer.getLevel(), consumer.getBlockPos(), consumerDirection.getOpposite());
        if (sourceStorage == null || consumerStorage == null) return;

        for (int i = 0; i < sourceStorage.size(); i++) {
            FluidResource resource = sourceStorage.get(i).getResource();
            if (resource.isBlank()) continue;
            long extracted = sourceStorage.extract(resource, transferRate, true);
            if (extracted > 0) {
                long inserted = FluidUtils.insertFluidStorage(consumerStorage, resource, extracted, false);
                if (inserted > 0) {
                    sourceStorage.extract(resource, inserted, false);
                }
            }
            break;
        }
    }

    @Override
    public boolean isValid(@NotNull BlockEntity entity, Direction direction) {
        return FluidApi.BLOCK.find(entity.getLevel(), entity.getBlockPos(), direction.getOpposite()) != null;
    }
}
