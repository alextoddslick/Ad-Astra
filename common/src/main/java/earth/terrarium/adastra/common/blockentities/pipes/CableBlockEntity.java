package earth.terrarium.adastra.common.blockentities.pipes;

import earth.terrarium.adastra.common.blockentities.machines.CoalGeneratorBlockEntity;
import earth.terrarium.adastra.common.blockentities.machines.EnergizerBlockEntity;
import earth.terrarium.adastra.common.blockentities.machines.SolarPanelBlockEntity;
import earth.terrarium.adastra.common.blocks.properties.PipeProperty;
import earth.terrarium.common_storage_lib.energy.EnergyApi;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CableBlockEntity extends PipeBlockEntity {

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    private boolean isProducer(BlockEntity entity) {
        return entity instanceof SolarPanelBlockEntity
            || entity instanceof CoalGeneratorBlockEntity
            || entity instanceof EnergizerBlockEntity;
    }

    @Override
    public void addNode(@NotNull BlockEntity entity, PipeProperty pipeProperty, Direction direction, BlockPos pos) {
        if (pipeProperty.isNone()) return;
        var container = EnergyApi.BLOCK.find(entity.getLevel(), pos, direction.getOpposite());
        if (container == null) return;

        if (pipeProperty.isExtract()) {
            // Wrench set to extract: only allow on producers
            if (isProducer(entity)) {
                sources.put(pos, direction);
            }
        } else if (pipeProperty.isInsert()) {
            consumers.put(pos, direction);
        } else {
            // Normal mode: only producers are sources, non-producers are consumers
            if (isProducer(entity) && container.getStoredAmount() > 0) {
                sources.put(pos, direction);
            }
            if (!isProducer(entity) && container.getCapacity() > 0) {
                consumers.put(pos, direction);
            }
        }
    }

    @Override
    public void moveContents(long transferRate, @NotNull BlockEntity source, @NotNull BlockEntity consumer, Direction sourceDirection, Direction consumerDirection) {
        var sourceContainer = EnergyApi.BLOCK.find(source.getLevel(), source.getBlockPos(), sourceDirection.getOpposite());
        if (sourceContainer == null) return;
        var consumerContainer = EnergyApi.BLOCK.find(consumer.getLevel(), consumer.getBlockPos(), consumerDirection.getOpposite());
        if (consumerContainer == null) return;
        long toTransfer = Math.min(transferRate, sourceContainer.getStoredAmount());
        long extracted = sourceContainer.extract(toTransfer, true);
        if (extracted > 0) {
            long inserted = consumerContainer.insert(extracted, false);
            if (inserted > 0) {
                sourceContainer.extract(inserted, false);
            }
        }
    }

    @Override
    public boolean isValid(@NotNull BlockEntity entity, Direction direction) {
        return EnergyApi.BLOCK.find(entity.getLevel(), entity.getBlockPos(), direction.getOpposite()) != null;
    }
}
