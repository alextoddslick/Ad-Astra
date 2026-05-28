package earth.terrarium.adastra.common.blockentities.base;

import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EnergyContainerMachineBlockEntity extends ContainerMachineBlockEntity implements EnergyProvider.BlockEntity {
    protected SimpleValueStorage energyContainer;

    public EnergyContainerMachineBlockEntity(BlockPos pos, BlockState state, int containerSize) {
        super(pos, state, containerSize);
    }

    public ChargeSlotType getChargeSlotType() {
        return ChargeSlotType.POWER_MACHINE;
    }

    @Override
    public void internalServerTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        super.internalServerTick(level, time, state, pos);
        switch (getChargeSlotType()) {
            case POWER_ITEM -> insertBatterySlot();
            case POWER_MACHINE -> extractBatterySlot();
        }
        if (time % 2 == 0) {
            setChanged();
            sync();
        }
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        if (energyContainer != null) {
            energyContainer.set(input.getLongOr("Energy", 0L));
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        if (energyContainer != null) {
            output.putLong("Energy", energyContainer.getStoredAmount());
        }
    }

    public SimpleValueStorage getEnergyStorage() {
        return energyContainer;
    }

    public ValueStorage getEnergyStorage(Direction direction) {
        return energyContainer;
    }

    @Override
    public ValueStorage getEnergy(@Nullable Direction direction) {
        return energyContainer;
    }

    public void extractBatterySlot() {
        ItemStack stack = this.getItem(0);
        if (stack.isEmpty()) return;
        if (!EnergyUtils.holdsEnergy(stack)) return;
        long maxTransfer = EnergyUtils.getMaxOutputPerTick(stack);
        EnergyUtils.transferFromItem(stack, energyContainer, maxTransfer);
    }

    public void insertBatterySlot() {
        ItemStack stack = this.getItem(0);
        if (stack.isEmpty()) return;
        if (!EnergyUtils.holdsEnergy(stack)) return;
        EnergyUtils.transferToItem(energyContainer, stack, energyContainer.getCapacity());
    }

    public enum ChargeSlotType {
        NONE,
        POWER_MACHINE,
        POWER_ITEM
    }
}
