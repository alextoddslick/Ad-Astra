package earth.terrarium.adastra.common.blockentities.machines;

import net.minecraft.world.level.material.Fluids;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import earth.terrarium.adastra.client.AdAstraClient;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationType;
import earth.terrarium.adastra.common.blocks.machines.GravityNormalizerBlock;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.constants.PlanetConstants;
import earth.terrarium.adastra.common.entities.AirVortex;
import earth.terrarium.adastra.common.menus.machines.OxygenDistributorMenu;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.registry.ModSoundEvents;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TransferUtils;
import earth.terrarium.adastra.common.utils.floodfill.FloodFill3D;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class OxygenDistributorBlockEntity extends OxygenLoaderBlockEntity {

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.PULL, ConstantComponents.SIDE_CONFIG_ENERGY),
        new ConfigurationEntry(ConfigurationType.FLUID, Configuration.PULL, ConstantComponents.SIDE_CONFIG_INPUT_FLUID),
        new ConfigurationEntry(ConfigurationType.FLUID, Configuration.PUSH, ConstantComponents.SIDE_CONFIG_OUTPUT_FLUID)
    );

    private final Set<BlockPos> lastDistributedBlocks = new HashSet<>();
    private long energyPerTick;
    private float fluidPerTick;
    private int distributedBlocksCount;
    private double accumulatedFluid;
    private int shutDownTicks;
    private int limit = MachineConfig.maxDistributionBlocks;
    private boolean shouldSyncPositions;

    private SimpleFluidStorage distributorFluidContainer;
    private float yRot;
    private float lastYRot;

    public OxygenDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 3);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.DESH);
    }

    @Override
    public SimpleFluidStorage getFluidContainer() {
        if (distributorFluidContainer != null) return distributorFluidContainer;
        // Tank 0 (input/left bar) must only accept water — never oxygen.
        // Tank 1 (output) must only ever hold oxygen.
        return distributorFluidContainer = new SimpleFluidStorage(2, MachineConfig.DESH.fluidCapacity * 81L)
            .filter(0, resource -> resource.getType() == Fluids.WATER)
            .filter(1, resource -> resource.getType() == ModFluids.OXYGEN.get());
    }


    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        input.read("LastDistributedBlocks", com.mojang.serialization.Codec.LONG_STREAM)
            .ifPresent(longStream -> {
                lastDistributedBlocks.clear();
                longStream.forEach(pos -> lastDistributedBlocks.add(BlockPos.of(pos)));
            });
        energyPerTick = input.getLongOr("EnergyPerTick", 0L);
        fluidPerTick = input.getFloatOr("FluidPerTick", 0f);
        distributedBlocksCount = input.getIntOr("DistributedBlocksCount", 0);
        accumulatedFluid = input.getDoubleOr("AccumulatedFluid", 0.0);
        limit = input.getIntOr("Limit", MachineConfig.maxDistributionBlocks);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("EnergyPerTick", energyPerTick);
        output.putFloat("FluidPerTick", fluidPerTick);
        output.putInt("DistributedBlocksCount", distributedBlocksCount);
        output.putDouble("AccumulatedFluid", accumulatedFluid);
        output.putInt("Limit", limit);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OxygenDistributorMenu(id, inventory, this);
    }

    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        if (this.energyContainer != null) return this.energyContainer;
        return this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.DESH);
    }

    @Override
    public void serverTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        super.serverTick(level, time, state, pos);
        if (shutDownTicks > 0) {
            shutDownTicks--;
            return;
        }

        long fluidPerTick = calculateFluidPerTick();
        boolean canDistribute = canCraftDistribution(Math.max(1 * 81L, fluidPerTick));

        if (canFunction() && canDistribute) {
            getEnergyStorage().extract(calculateEnergyPerTick(), false);
            setLit(true);
            accumulatedFluid += fluidPerTick;
            // accumulatedFluid is in droplets; consume when we've accumulated at least 81 droplets (1 mB)
            if (accumulatedFluid >= 81.0) {
                long toConsume = (long) (accumulatedFluid / 81.0) * 81L; // round down to whole mB in droplets
                consumeDistribution(toConsume);
                accumulatedFluid -= toConsume;
            }

            if (time % MachineConfig.distributionRefreshRate == 0) tickOxygen(level, pos, state);

            if (time % 200 == 0) {
                level.playSound(null, pos, ModSoundEvents.OXYGEN_OUTTAKE.get(), SoundSource.BLOCKS, 0.2f, 1);
            } else if (time % 100 == 0) {
                level.playSound(null, pos, ModSoundEvents.OXYGEN_INTAKE.get(), SoundSource.BLOCKS, 0.2f, 1);
            }
        } else if (!lastDistributedBlocks.isEmpty()) {
            clearOxygenBlocks();
            shutDownTicks = 60;
            setLit(false);
        } else if (time % 10 == 0) setLit(false);

        energyPerTick = (recipe != null && canCraft() ? recipe.energy() : 0) + (canDistribute ? calculateEnergyPerTick() : 0);
        this.fluidPerTick = canDistribute ? fluidPerTick : 0;
        distributedBlocksCount = canDistribute ? lastDistributedBlocks.size() : 0;
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullItemsNearby(this, pos, new int[]{1}, sideConfig.get(0), filter);
        TransferUtils.pushItemsNearby(this, pos, new int[]{2}, sideConfig.get(1), filter);
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(2), filter);
        TransferUtils.pullFluidNearby(this, pos, getFluidContainer(), 200 * 81L, 0, sideConfig.get(3), filter);
        TransferUtils.pushFluidNearby(this, pos, getFluidContainer(), 200 * 81L, 1, sideConfig.get(4), filter);
    }

    @Override
    public void onRemoved() {
        clearOxygenBlocks();
    }

    private boolean canCraftDistribution(long fluidAmount) {
        long energy = calculateEnergyPerTick();
        if (getEnergyStorage().extract(energy, true) < energy) return false;
        // Check that output tank (tank 1) has enough oxygen fluid to consume
        var outputSlot = getFluidContainer().get(1);
        if (outputSlot.getResource().isBlank() || outputSlot.getAmount() < fluidAmount) {
            return false;
        }
        return true;
    }

    protected void consumeDistribution(long fluidAmount) {
        var outputSlot = getFluidContainer().get(1);
        var resource = outputSlot.getResource();
        if (resource.isBlank()) return;
        long extracted = outputSlot.extract(resource, fluidAmount, false);
        // Fluid consumed for oxygen distribution
    }

    protected void tickOxygen(ServerLevel level, BlockPos pos, BlockState state) {
        limit = MachineConfig.maxDistributionBlocks;
        Direction offset = switch (state.getValue(GravityNormalizerBlock.FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            default -> state.getValue(GravityNormalizerBlock.FACING);
        };
        Set<BlockPos> positions = FloodFill3D.run(level, pos.relative(offset), limit, FloodFill3D.TEST_FULL_SEAL, true);

        OxygenApi.API.setOxygen(level, positions, true);
        TemperatureApi.API.setTemperature(level, positions, PlanetConstants.COMFY_EARTH_TEMPERATURE); // TODO: move to Temperature Regulator machine

        Set<BlockPos> lastPositionsCopy = new HashSet<>(lastDistributedBlocks);
        this.resetLastDistributedBlocks(positions);

        if (AdAstraConfig.disableAirVortexes) return;
        if (positions.size() < limit) return;
        if (lastPositionsCopy.size() >= limit) return;
        if (OxygenApi.API.hasOxygen(level)) return;

        positions.removeAll(lastPositionsCopy);
        BlockPos target = positions.stream()
            .skip(1)
            .findFirst()
            .orElse(positions.stream().findFirst().orElse(null));
        if (target == null) return;
        AirVortex vortex = new AirVortex(level, pos, lastPositionsCopy);
        vortex.setPos(Vec3.atCenterOf(target));
        level.addFreshEntity(vortex);
    }

    protected void resetLastDistributedBlocks(Set<BlockPos> positions) {
        lastDistributedBlocks.removeAll(positions);
        clearOxygenBlocks();
        lastDistributedBlocks.addAll(positions);
        shouldSyncPositions = true;
    }

    protected void clearOxygenBlocks() {
        OxygenApi.API.removeOxygen(level, lastDistributedBlocks);
        TemperatureApi.API.removeTemperature(level, lastDistributedBlocks); // TODO: move to Temperature Regulator machine
        lastDistributedBlocks.clear();
    }

    @Override
    public void internalServerTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        // Replicate parent logic but always run recipe conversion (water→oxygen)
        // regardless of redstone settings. Only distribution is gated by canFunction().

        // From ContainerMachineBlockEntity: periodic recipe search
        if (time % 50 == 0 && shouldUpdate()) {
            update();
        }
        // Extract energy from battery slot (e.g. Etrionic Capacitor)
        extractBatterySlot();

        // Side interactions (pull fluid, push fluid, pull energy) always run
        tickSideInteractions(getBlockPos(), f -> true, getSideConfig());

        // Always run recipeTick — conversion should happen whenever there's power + water
        if (recipe != null) {
            recipeTick(level, getEnergyStorage());
        }
        if (time % 5 == 0 && shouldAutomaticallyUpdateLitState()) {
            setLit(cookTimeTotal > 0 && recipe != null);
        }

        // From OxygenLoaderBlockEntity: update slots and re-search recipes
        updateSlots();
        if (time % 10 == 0 && recipe == null) {
            update();
        }

        // Sync block entity data to client (from EnergyContainerMachineBlockEntity)
        if (time % 2 == 0) {
            setChanged();
            sync();
        }
    }

    @Override
    public void updateSlots() {
        FluidUtils.moveItemToContainer(this, getFluidContainer(), 1, 2, 0);
        // Don't call sync() here - parent EnergyContainerMachineBlockEntity already syncs every 2 ticks
    }

    @Override
    public void clientTick(ClientLevel level, long time, BlockState state, BlockPos pos) {
        if (time % 40 == 0) {
            if (AdAstraConfigClient.showOxygenDistributorArea) {
                AdAstraClient.OXYGEN_OVERLAY_RENDERER.removePositions(pos);
                if (AdAstraClient.OXYGEN_OVERLAY_RENDERER.canAdd(pos)
                    && canFunction()
                    && canCraftDistribution(Math.max(1, calculateFluidPerTick() / 1000) * 81L)) {
                    AdAstraClient.OXYGEN_OVERLAY_RENDERER.addPositions(pos, lastDistributedBlocks);
                }
            } else AdAstraClient.OXYGEN_OVERLAY_RENDERER.clearPositions();
        }

        lastYRot = yRot;
        if (isLit()) {
            yRot += 10;
        }
    }

    @Override
    public boolean shouldAutomaticallyUpdateLitState() {
        return false;
    }

    public int distributedBlocksCount() {
        return canFunction() ? distributedBlocksCount : 0;
    }

    public int distributedBlocksLimit() {
        return limit;
    }

    public long energyPerTick() {
        return canFunction() ? energyPerTick : 0;
    }

    public float fluidPerTick() {
        return canFunction() ? fluidPerTick : 0;
    }

    public float yRot() {
        return yRot;
    }

    public float lastYRot() {
        return lastYRot;
    }

    private long calculateEnergyPerTick() {
        return Math.max(1, lastDistributedBlocks.size() / 50);
    }

    private long calculateFluidPerTick() {
        // Use ceiling division to ensure any distributed blocks consume fluid proportionally.
        // Previously, integer division caused 0 consumption for < 1500 blocks.
        int blocks = lastDistributedBlocks.size();
        if (blocks <= 0) return 81L;
        return Math.max(1, (blocks + 1499) / 1500) * 81L;
    }

    @Override
    public List<ConfigurationEntry> getDefaultConfig() {
        return SIDE_CONFIG;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{1, 2};
    }

    // Only sync positions when recalculating the distributed blocks.
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        if (shouldSyncPositions) {
            tag.putLongArray("LastDistributedBlocks", lastDistributedBlocks.stream()
                .mapToLong(BlockPos::asLong).toArray());
            shouldSyncPositions = false;
        }
        return tag;
    }
}
