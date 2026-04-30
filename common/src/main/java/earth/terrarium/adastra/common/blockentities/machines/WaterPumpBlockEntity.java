package earth.terrarium.adastra.common.blockentities.machines;

import earth.terrarium.adastra.common.blockentities.base.EnergyContainerMachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationType;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.menus.machines.WaterPumpMenu;
import earth.terrarium.adastra.common.registry.ModParticleTypes;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.ModUtils;
import earth.terrarium.adastra.common.utils.TransferUtils;
import earth.terrarium.common_storage_lib.fluid.util.FluidProvider;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.fluid.util.FluidStorageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class WaterPumpBlockEntity extends EnergyContainerMachineBlockEntity implements FluidProvider.BlockEntity {

    private static final long BUCKET = 81000L;

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.PULL, ConstantComponents.SIDE_CONFIG_ENERGY),
        new ConfigurationEntry(ConfigurationType.FLUID, Configuration.PUSH, ConstantComponents.SIDE_CONFIG_OUTPUT_FLUID)
    );

    private SimpleFluidStorage fluidContainer;

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 1);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.DESH);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WaterPumpMenu(id, inventory, this);
    }

    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        if (energyContainer != null) return energyContainer;
        return energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.DESH);
    }

    public @Nullable SimpleFluidStorage getFluidContainer(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        return getFluidContainer();
    }

    public SimpleFluidStorage getFluidContainer() {
        if (fluidContainer != null) return fluidContainer;
        // Tank 0 must only ever hold water — never oxygen or any other fluid.
        return fluidContainer = new SimpleFluidStorage(1, MachineConfig.DESH.fluidCapacity * 81L)
            .filter(0, resource -> resource.getType() == Fluids.WATER);
    }

    @Override
    public CommonStorage<FluidResource> getFluids(@Nullable Direction direction) {
        return getFluidContainer();
    }

    @Override
    public void serverTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        if (!canFunction()) return;
        if (canPump(pos, energyContainer)) pump(level, energyContainer);
    }

    private boolean canPump(BlockPos pos, ValueStorage energyStorage) {
        if (!level().getFluidState(pos.below()).is(Fluids.WATER)) return false;
        if (energyStorage.extract(MachineConfig.waterPumpEnergyPerTick, true) < MachineConfig.waterPumpEnergyPerTick)
            return false;
        SimpleFluidStorage fc = getFluidContainer();
        FluidResource water = FluidResource.of(Fluids.WATER);
        // Use slot-level insert with workaround for FluidResource reference equality
        return FluidUtils.insertFluid(fc.get(0), water, 1, true) > 0;
    }

    private void pump(ServerLevel level, ValueStorage energyStorage) {
        energyStorage.extract(MachineConfig.waterPumpEnergyPerTick, false);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidUtils.insertFluid(getFluidContainer().get(0), water, MachineConfig.waterPumpFluidGenerationPerTick * 81L, false);
        ModUtils.sendParticles(level,
            ModParticleTypes.OXYGEN_BUBBLE.get(),
            getBlockPos().getX() + 0.5,
            getBlockPos().getY() - 0.5,
            getBlockPos().getZ() + 0.5,
            1,
            0.0, 0.0, 0.0,
            0.01);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("FluidData", FluidStorageData.CODEC)
            .ifPresent(data -> getFluidContainer().readSnapshot(data));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("FluidData", FluidStorageData.CODEC, getFluidContainer().createSnapshot());
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(0), filter);
        TransferUtils.pushFluidNearby(this, pos, getFluidContainer(), MachineConfig.waterPumpFluidGenerationPerTick * 81L, 0, sideConfig.get(1), filter);
    }

    @Override
    public List<ConfigurationEntry> getDefaultConfig() {
        return SIDE_CONFIG;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{};
    }
}
