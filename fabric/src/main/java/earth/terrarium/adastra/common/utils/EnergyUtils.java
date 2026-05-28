package earth.terrarium.adastra.common.utils;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import earth.terrarium.adastra.common.config.machines.MachineTypeConfigObject;
import earth.terrarium.adastra.common.items.EtrionicCapacitorItem;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.items.machines.EnergizerBlockItem;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public class EnergyUtils {

    private static final String ENERGY_TAG = "Energy";

    public static SimpleValueStorage machineInsertOnlyEnergy(MachineTypeConfigObject config) {
        return new SimpleValueStorage(config.energyCapacity);
    }

    public static SimpleValueStorage machineExtractOnlyEnergy(MachineTypeConfigObject config) {
        return new SimpleValueStorage(config.energyCapacity);
    }

    /**
     * Creates a SimpleValueStorage backed by the ItemStack's CustomData NBT.
     * Reads the current stored energy from the stack and initializes the storage with it.
     */
    public static SimpleValueStorage getItemEnergyStorage(ItemStack stack, long capacity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long stored = tag.getLongOr(ENERGY_TAG, 0L);
        SimpleValueStorage storage = new SimpleValueStorage(capacity);
        storage.set(Math.min(stored, capacity));
        return storage;
    }

    /**
     * Saves the energy storage state back to the ItemStack's CustomData NBT.
     */
    public static void saveItemEnergyStorage(ItemStack stack, SimpleValueStorage storage) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(ENERGY_TAG, storage.getStoredAmount());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Checks if the given ItemStack has an energy-holding item (items that support energy storage).
     */
    public static boolean holdsEnergy(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof EtrionicCapacitorItem
            || item instanceof JetSuitItem
            || item instanceof EnergizerBlockItem;
    }

    /**
     * Returns the maximum energy output per tick for the given energy-holding item.
     * Etrionic Capacitor: 25/tick (500/second).
     * Other items: no practical limit.
     */
    public static long getMaxOutputPerTick(ItemStack stack) {
        if (stack.getItem() instanceof EtrionicCapacitorItem) {
            return 25; // 500 energy per second / 20 ticks
        }
        return Long.MAX_VALUE;
    }

    /**
     * Gets the energy storage for an item, or null if the item does not hold energy.
     * The returned storage is initialized from the ItemStack's NBT data.
     */
    @Nullable
    public static SimpleValueStorage getItemEnergyStorageOrNull(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof EtrionicCapacitorItem capacitor) {
            return capacitor.getEnergyStorage(stack);
        } else if (item instanceof JetSuitItem jetSuit) {
            return jetSuit.getEnergyStorage(stack);
        } else if (item instanceof EnergizerBlockItem energizer) {
            return energizer.getEnergyStorage(stack);
        }
        return null;
    }

    /**
     * Transfers energy from a source ValueStorage to an item's energy storage.
     * Returns the amount of energy actually transferred.
     */
    public static long transferToItem(ValueStorage source, ItemStack stack, long maxAmount) {
        SimpleValueStorage itemStorage = getItemEnergyStorageOrNull(stack);
        if (itemStorage == null) return 0;

        long extractable = source.extract(maxAmount, true);
        if (extractable <= 0) return 0;

        long inserted = itemStorage.insert(extractable, false);
        if (inserted <= 0) return 0;

        source.extract(inserted, false);
        saveItemEnergyStorage(stack, itemStorage);
        return inserted;
    }

    /**
     * Transfers energy from an item's energy storage to a destination ValueStorage.
     * Returns the amount of energy actually transferred.
     */
    public static long transferFromItem(ItemStack stack, ValueStorage destination, long maxAmount) {
        SimpleValueStorage itemStorage = getItemEnergyStorageOrNull(stack);
        if (itemStorage == null) return 0;

        long extractable = itemStorage.extract(maxAmount, true);
        if (extractable <= 0) return 0;

        long inserted = destination.insert(extractable, false);
        if (inserted <= 0) return 0;

        itemStorage.extract(inserted, false);
        saveItemEnergyStorage(stack, itemStorage);
        return inserted;
    }

    public static ItemStack energyFilledItem(RegistryEntry<Item> item) {
        return energyFilledItem(item.get().getDefaultInstance());
    }

    public static ItemStack energyFilledItem(ItemStack stack) {
        SimpleValueStorage container = getItemEnergyStorageOrNull(stack);
        if (container != null) {
            container.set(container.getCapacity());
            saveItemEnergyStorage(stack, container);
        }
        return stack;
    }
}
