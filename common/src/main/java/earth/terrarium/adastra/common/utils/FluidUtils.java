package earth.terrarium.adastra.common.utils;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import earth.terrarium.adastra.common.items.GasTankItem;
import earth.terrarium.adastra.common.items.ZipGunItem;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.adastra.common.items.vehicles.VehicleItem;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidSlot;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.resources.fluid.ingredient.FluidIngredient;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import earth.terrarium.common_storage_lib.storage.base.StorageSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidUtils {

    /**
     * @deprecated Use getItemFluidContainer(stack) instead.
     */
    @Deprecated
    public static Object getTank(ItemStack stack) {
        return null;
    }

    /**
     * Gets the fluid contained in a bucket item by checking the fluid registry.
     */
    public static Fluid getFluidFromBucket(Item item) {
        if (item == Items.WATER_BUCKET) return Fluids.WATER;
        if (item == Items.LAVA_BUCKET) return Fluids.LAVA;
        if (!(item instanceof BucketItem)) return Fluids.EMPTY;
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid.getBucket() == item && fluid != Fluids.EMPTY) {
                return fluid;
            }
        }
        return Fluids.EMPTY;
    }

    public static boolean hasFluid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (getFluidFromBucket(stack.getItem()) != Fluids.EMPTY) return true;
        SimpleFluidStorage container = getItemFluidContainer(stack);
        return container != null && !container.get(0).getResource().isBlank();
    }

    public static boolean hasFluid(ItemStack stack, int tank) {
        return hasFluid(stack);
    }

    public static long getCapacity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof BucketItem) return 81000L;
        SimpleFluidStorage container = getItemFluidContainer(stack);
        if (container != null) return container.get(0).getLimit(container.get(0).getResource());
        return 0;
    }

    public static long getCapacity(ItemStack stack, int tank) {
        return getCapacity(stack);
    }

    /**
     * Creates an ItemStack filled with the given fluid to capacity.
     */
    public static ItemStack fluidFilledItem(RegistryEntry<Item> item, RegistryEntry<Fluid> fluid) {
        ItemStack stack = item.get().getDefaultInstance();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        SimpleFluidStorage container = getItemFluidContainer(stack);
        if (container == null) return ItemStack.EMPTY;
        FluidResource resource = FluidResource.of(fluid.get());
        container.get(0).insert(resource, container.get(0).getLimit(resource), false);
        saveItemFluidStorage(stack, container);
        return stack;
    }

    /**
     * Gets an NBT-backed fluid storage for items that hold fluid (SpaceSuit, GasTank, etc.).
     * Reads the current stored fluid from the stack's CustomData.
     */
    public static SimpleFluidStorage getItemFluidStorage(ItemStack stack, int slots, long capacity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        SimpleFluidStorage storage = new SimpleFluidStorage(slots, capacity);
        if (tag.contains("FluidType") && tag.contains("FluidAmount")) {
            String fluidId = tag.getStringOr("FluidType", "");
            long amount = tag.getLongOr("FluidAmount", 0L);
            Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId));
            if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                storage.get(0).insert(FluidResource.of(fluid), Math.min(amount, capacity), false);
            }
        }
        return storage;
    }

    /**
     * Saves fluid storage state back to the ItemStack's CustomData NBT.
     */
    public static void saveItemFluidStorage(ItemStack stack, SimpleFluidStorage storage) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        FluidResource resource = storage.get(0).getResource();
        if (!resource.isBlank() && storage.get(0).getAmount() > 0) {
            tag.putString("FluidType", BuiltInRegistries.FLUID.getKey(resource.getType()).toString());
            tag.putLong("FluidAmount", storage.get(0).getAmount());
        } else {
            tag.remove("FluidType");
            tag.remove("FluidAmount");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Gets the fluid container for any item that holds fluid (SpaceSuit, JetSuit, GasTank).
     * Returns null if the item doesn't hold fluid.
     */
    public static SimpleFluidStorage getItemFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof SpaceSuitItem suit) {
            return suit.getFluidContainer(stack);
        } else if (item instanceof GasTankItem tank) {
            return tank.getFluidContainer(stack);
        } else if (item instanceof ZipGunItem zipGun) {
            return zipGun.getFluidContainer(stack);
        } else if (item instanceof VehicleItem vehicle) {
            return vehicle.getFluidContainer(stack);
        }
        return null;
    }

    /**
     * Checks if two FluidResources represent the same fluid type.
     * CSL's FluidResource doesn't override equals(), so we compare the underlying Fluid type.
     */
    public static boolean isSameFluid(FluidResource a, FluidResource b) {
        if (a.isBlank() && b.isBlank()) return true;
        if (a.isBlank() || b.isBlank()) return false;
        return a.getType() == b.getType();
    }

    /**
     * Tests if a FluidIngredient matches a FluidResource.
     * CSL 0.0.7's BaseFluidIngredient.test() always returns false (stub),
     * so we check against getMatchingFluids() instead.
     */
    public static boolean ingredientMatches(FluidIngredient ingredient, FluidResource resource) {
        if (resource.isBlank()) return false;
        for (FluidResource matching : ingredient.getMatchingFluids()) {
            if (matching.getType() == resource.getType()) return true;
        }
        return false;
    }

    /**
     * Inserts fluid into a SimpleFluidSlot, working around CSL's FluidResource reference equality bug.
     * When the slot already contains the same fluid type, reuses the slot's existing FluidResource
     * instance so that SimpleFluidSlot.insert()'s equals() check passes.
     */
    public static long insertFluid(SimpleFluidSlot slot, FluidResource resource, long amount, boolean simulate) {
        FluidResource existing = slot.getResource();
        if (!existing.isBlank() && isSameFluid(existing, resource)) {
            // Reuse the slot's own FluidResource instance to bypass reference equality
            return slot.insert(existing, amount, simulate);
        }
        return slot.insert(resource, amount, simulate);
    }

    /**
     * Inserts fluid into a CommonStorage (e.g. SimpleFluidStorage), working around
     * CSL's FluidResource reference equality bug. Iterates each slot and reuses
     * the slot's existing FluidResource instance when the fluid type matches.
     */
    public static long insertFluidStorage(CommonStorage<FluidResource> storage, FluidResource resource, long amount, boolean simulate) {
        // Work with any CommonStorage by iterating slots and reusing existing FluidResource
        // instances to work around CSL's FluidResource reference equality bug.
        long remaining = amount;

        // First pass: insert into slots that already contain the same fluid type
        for (int i = 0; i < storage.size(); i++) {
            if (remaining <= 0) break;
            StorageSlot<FluidResource> slot = storage.get(i);
            FluidResource existing = slot.getResource();
            if (!existing.isBlank() && isSameFluid(existing, resource)) {
                // Use the slot's own FluidResource instance to bypass reference equality
                long inserted = slot.insert(existing, remaining, simulate);
                remaining -= inserted;
            }
        }

        // Second pass: insert into empty slots
        for (int i = 0; i < storage.size(); i++) {
            if (remaining <= 0) break;
            StorageSlot<FluidResource> slot = storage.get(i);
            if (slot.getResource().isBlank()) {
                // Test with simulate first to respect slot filters
                if (slot.insert(resource, 1, true) <= 0) continue;
                long inserted = slot.insert(resource, remaining, simulate);
                remaining -= inserted;
            }
        }

        return amount - remaining;
    }

    /**
     * Moves fluid from a bucket item to a fluid container.
     */
    public static void moveItemToContainer(Container container, Object fluidContainer, int slot, int resultSlot, int tank) {
        if (!(fluidContainer instanceof SimpleFluidStorage storage)) return;
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty()) return;

        Fluid fluid = getFluidFromBucket(stack.getItem());
        if (fluid != Fluids.EMPTY) {
            FluidResource resource = FluidResource.of(fluid);
            SimpleFluidSlot fluidSlot = storage.get(tank);
            long inserted = insertFluid(fluidSlot, resource, 81000L, true);

            ItemStack result = container.getItem(resultSlot);
            boolean resultSlotCanAccept = result.isEmpty() || (result.is(Items.BUCKET) && result.getCount() < result.getMaxStackSize());
            if (!resultSlotCanAccept) return;

            if (inserted >= 81000L) {
                insertFluid(fluidSlot, resource, 81000L, false);
                container.setItem(slot, ItemStack.EMPTY);
                if (result.isEmpty()) {
                    container.setItem(resultSlot, new ItemStack(Items.BUCKET));
                } else if (result.is(Items.BUCKET) && result.getCount() < result.getMaxStackSize()) {
                    result.grow(1);
                }
            }
        }
    }

    /**
     * Gets a display color for the given fluid, suitable for item bar rendering.
     */
    public static int getFluidBarColor(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) return 0x3F76E4;
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) return 0xFFFFFF;
        if (fluid == ModFluids.OXYGEN.get()) return 0xDAE6F0;
        if (fluid == ModFluids.HYDROGEN.get()) return 0x89CFF0;
        if (fluid == ModFluids.OIL.get()) return 0x373A36;
        if (fluid == ModFluids.FUEL.get()) return 0xE5292B;
        if (fluid == ModFluids.CRYO_FUEL.get()) return 0x6CFFFA;
        return 0xFFFFFF;
    }

    /**
     * Moves fluid from a fluid container to an item (bucket, space suit, gas tank, etc.).
     */
    public static void moveContainerToItem(Container container, Object fluidContainer, int slot, int resultSlot, int tank) {
        if (!(fluidContainer instanceof SimpleFluidStorage storage)) return;
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty()) return;

        SimpleFluidSlot fluidSlot = storage.get(tank);
        FluidResource resource = fluidSlot.getResource();
        if (resource.isBlank()) return;

        // Handle non-bucket fluid items (space suit, jet suit, gas tank)
        SimpleFluidStorage itemContainer = getItemFluidContainer(stack);
        if (itemContainer != null) {
            long transferAmount = Math.min(fluidSlot.getAmount(), 200 * 81L); // 200 mB per tick
            long inserted = insertFluid(itemContainer.get(0), resource, transferAmount, true);
            if (inserted > 0) {
                insertFluid(itemContainer.get(0), resource, inserted, false);
                fluidSlot.extract(resource, inserted, false);
                saveItemFluidStorage(stack, itemContainer);
            }
            // When the item is full, move it to the result slot
            long currentAmount = itemContainer.get(0).getAmount();
            long capacity = itemContainer.get(0).getLimit(resource);
            if (currentAmount >= capacity && resultSlot >= 0) {
                ItemStack result = container.getItem(resultSlot);
                if (result.isEmpty()) {
                    container.setItem(resultSlot, stack.copy());
                    container.setItem(slot, ItemStack.EMPTY);
                }
            }
            return;
        }

        // Handle buckets
        if (!stack.is(Items.BUCKET)) return;
        if (fluidSlot.getAmount() < 81000L) return;

        ItemStack result = container.getItem(resultSlot);
        Item bucketItem = resource.getType().getBucket();
        if (bucketItem == Items.AIR) return;

        ItemStack filledBucket = new ItemStack(bucketItem);
        if (!result.isEmpty() && (!ItemStack.isSameItemSameComponents(result, filledBucket) || result.getCount() >= result.getMaxStackSize())) {
            return;
        }

        fluidSlot.extract(resource, 81000L, false);
        stack.shrink(1);
        if (result.isEmpty()) {
            container.setItem(resultSlot, filledBucket);
        } else {
            result.grow(1);
        }
    }
}
