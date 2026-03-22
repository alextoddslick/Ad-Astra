package earth.terrarium.adastra.common.utils;

import earth.terrarium.adastra.common.blockentities.base.ContainerMachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.common_storage_lib.energy.EnergyApi;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidSlot;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.function.Predicate;

public class TransferUtils {

    public static void pushEnergyNearby(ContainerMachineBlockEntity machine, BlockPos pos, long amount, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        pushEnergyNearby(machine, pos, amount, sideConfig, filter, entity -> true);
    }

    public static void pushEnergyNearby(ContainerMachineBlockEntity machine, BlockPos pos, long amount, ConfigurationEntry sideConfig, Predicate<Direction> filter, Predicate<BlockEntity> entityFilter) {
        ValueStorage container = EnergyApi.BLOCK.find(machine.getLevel(), pos, null);
        if (container == null) return;
        if (container.getStoredAmount() == 0) return;

        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPush()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            BlockPos nearbyPos = pos.relative(direction);
            BlockEntity nearbyEntity = machine.getLevel().getBlockEntity(nearbyPos);
            if (nearbyEntity != null && !entityFilter.test(nearbyEntity)) continue;
            ValueStorage nearbyContainer = EnergyApi.BLOCK.find(machine.getLevel(), nearbyPos, direction.getOpposite());
            if (nearbyContainer == null) continue;
            long extracted = container.extract(amount, true);
            if (extracted > 0) {
                long inserted = nearbyContainer.insert(extracted, false);
                if (inserted > 0) {
                    container.extract(inserted, false);
                }
            }
        }
    }

    public static void pullEnergyNearby(ContainerMachineBlockEntity machine, BlockPos pos, long amount, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        ValueStorage container = EnergyApi.BLOCK.find(machine.getLevel(), pos, null);
        if (container == null) return;

        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPull()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            BlockPos nearbyPos = pos.relative(direction);
            // Don't pull energy from other machines - only from cables/generators
            var nearbyEntity = machine.getLevel().getBlockEntity(nearbyPos);
            if (nearbyEntity instanceof ContainerMachineBlockEntity) continue;
            ValueStorage nearbyContainer = EnergyApi.BLOCK.find(machine.getLevel(), nearbyPos, direction.getOpposite());
            if (nearbyContainer == null) continue;
            long extracted = nearbyContainer.extract(amount, true);
            if (extracted > 0) {
                long inserted = container.insert(extracted, false);
                if (inserted > 0) {
                    nearbyContainer.extract(inserted, false);
                }
            }
        }
    }

    public static void pushFluidNearby(ContainerMachineBlockEntity machine, BlockPos pos, SimpleFluidStorage container, long amount, int tank, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        SimpleFluidSlot slot = container.get(tank);
        FluidResource resource = slot.getResource();
        if (resource.isBlank()) return;

        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPush()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            BlockPos nearbyPos = pos.relative(direction);
            CommonStorage<FluidResource> nearbyContainer = FluidApi.BLOCK.find(machine.getLevel(), nearbyPos, direction.getOpposite());
            if (nearbyContainer == null) continue;
            long extracted = slot.extract(resource, amount, true);
            if (extracted > 0) {
                long inserted = FluidUtils.insertFluidStorage(nearbyContainer, resource, extracted, false);
                if (inserted > 0) {
                    slot.extract(resource, inserted, false);
                }
            }
        }
    }

    public static void pullFluidNearby(ContainerMachineBlockEntity machine, BlockPos pos, SimpleFluidStorage container, long amount, int tank, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPull()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            BlockPos nearbyPos = pos.relative(direction);
            CommonStorage<FluidResource> nearbyContainer = FluidApi.BLOCK.find(machine.getLevel(), nearbyPos, direction.getOpposite());
            if (nearbyContainer == null) continue;
            for (int i = 0; i < nearbyContainer.size(); i++) {
                FluidResource resource = nearbyContainer.get(i).getResource();
                if (resource.isBlank()) continue;
                long extracted = nearbyContainer.extract(resource, amount, true);
                if (extracted > 0) {
                    long inserted = FluidUtils.insertFluid(container.get(tank), resource, extracted, false);
                    if (inserted > 0) {
                        nearbyContainer.extract(resource, inserted, false);
                    }
                }
            }
        }
    }

    public static void pushItemsNearby(ContainerMachineBlockEntity machine, BlockPos pos, int[] slots, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        if (machine.isEmpty()) return;

        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPush()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            Container nearbyContainer = HopperBlockEntity.getContainerAt(machine.level(), pos.relative(direction));
            if (nearbyContainer == null) continue;
            ItemUtils.push(machine, nearbyContainer, slots, direction);
        }
    }

    public static void pullItemsNearby(ContainerMachineBlockEntity machine, BlockPos pos, int[] slots, ConfigurationEntry sideConfig, Predicate<Direction> filter) {
        for (var entry : sideConfig.sides().entrySet()) {
            Configuration configuration = entry.getValue();
            if (!configuration.canPull()) continue;
            Direction direction = ModUtils.relative(machine, entry.getKey());
            if (!filter.test(direction)) continue;
            Container nearbyContainer = HopperBlockEntity.getContainerAt(machine.level(), pos.relative(direction));
            if (nearbyContainer == null) continue;
            ItemUtils.pull(nearbyContainer, machine, slots, direction);
        }
    }
}
