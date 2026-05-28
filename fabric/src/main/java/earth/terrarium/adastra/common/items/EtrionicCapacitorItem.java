package earth.terrarium.adastra.common.items;

import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.utils.DistributionMode;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class EtrionicCapacitorItem extends Item implements EnergyProvider.Item {

    public static final String ACTIVE_TAG = "Active";
    public static final String MODE_TAG = "Mode";

    public EtrionicCapacitorItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }

    private static void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean active(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag.contains(ACTIVE_TAG)) {
            return tag.getBooleanOr(ACTIVE_TAG, false);
        }
        return false;
    }

    public static boolean toggleActive(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        boolean active = active(stack);
        tag.putBoolean(ACTIVE_TAG, !active);
        setCustomData(stack, tag);
        return !active;
    }

    public static DistributionMode mode(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag.contains(MODE_TAG)) {
            return DistributionMode.values()[tag.getByteOr(MODE_TAG, (byte) 0)];
        }
        return DistributionMode.SEQUENTIAL;
    }

    public static DistributionMode toggleMode(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        DistributionMode mode = mode(stack);
        DistributionMode toggled = mode == DistributionMode.SEQUENTIAL ? DistributionMode.ROUND_ROBIN : DistributionMode.SEQUENTIAL;
        tag.putByte(MODE_TAG, (byte) toggled.ordinal());
        setCustomData(stack, tag);
        return toggled;
    }

    public SimpleValueStorage getEnergyStorage(ItemStack holder) {
        return EnergyUtils.getItemEnergyStorage(holder, 250_000);
    }

    @Override
    public ValueStorage getEnergy(ItemStack stack, ItemContext context) {
        return getEnergyStorage(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> consumer, @NotNull TooltipFlag isAdvanced) {
        SimpleValueStorage energy = getEnergyStorage(stack);
        consumer.accept(TooltipUtils.getEnergyComponent(energy.getStoredAmount(), energy.getCapacity()));
        consumer.accept(TooltipUtils.getMaxEnergyInComponent(250));
        consumer.accept(TooltipUtils.getMaxEnergyOutComponent(25));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.ETRIONIC_CAPACITOR_INFO);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @NotNull EquipmentSlot slot) {
        if (entity.tickCount % 5 != 0) return;
        if (!active(stack)) return;
        if (!(entity instanceof Player player)) return;
        Inventory inventory = player.getInventory();
        SimpleValueStorage container = getEnergyStorage(stack);
        if (container.getStoredAmount() == 0) return;

        long maxExtract = 500;
        if (mode(stack) == DistributionMode.SEQUENTIAL) {
            distributeSequential(stack, maxExtract, inventory);
        } else {
            distributeRoundRobin(stack, maxExtract, inventory);
        }
    }

    public void distributeSequential(ItemStack from, long maxExtract, Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack target = inventory.getItem(i);
            if (target == from || target.isEmpty()) continue;
            if (!EnergyUtils.holdsEnergy(target)) continue;

            SimpleValueStorage fromStorage = getEnergyStorage(from);
            SimpleValueStorage targetStorage = EnergyUtils.getItemEnergyStorageOrNull(target);
            if (targetStorage == null) continue;

            long extractable = fromStorage.extract(maxExtract, true);
            if (extractable <= 0) continue;

            long inserted = targetStorage.insert(extractable, false);
            if (inserted > 0) {
                fromStorage.extract(inserted, false);
                EnergyUtils.saveItemEnergyStorage(from, fromStorage);
                EnergyUtils.saveItemEnergyStorage(target, targetStorage);
                break;
            }
        }
    }

    public void distributeRoundRobin(ItemStack from, long maxExtract, Inventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack target = inventory.getItem(i);
            if (target == from || target.isEmpty()) continue;
            if (!EnergyUtils.holdsEnergy(target)) continue;
            count++;
        }
        if (count == 0) return;

        long perItem = Math.max(1, maxExtract / count);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack target = inventory.getItem(i);
            if (target == from || target.isEmpty()) continue;
            if (!EnergyUtils.holdsEnergy(target)) continue;

            SimpleValueStorage fromStorage = getEnergyStorage(from);
            if (fromStorage.getStoredAmount() <= 0) break;

            SimpleValueStorage targetStorage = EnergyUtils.getItemEnergyStorageOrNull(target);
            if (targetStorage == null) continue;

            long extractable = fromStorage.extract(perItem, true);
            if (extractable <= 0) continue;

            long inserted = targetStorage.insert(extractable, false);
            if (inserted > 0) {
                fromStorage.extract(inserted, false);
                EnergyUtils.saveItemEnergyStorage(from, fromStorage);
                EnergyUtils.saveItemEnergyStorage(target, targetStorage);
            }
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getEnergyStorage(stack).getStoredAmount() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var energyStorage = getEnergyStorage(stack);
        return (int) (((double) energyStorage.getStoredAmount() / energyStorage.getCapacity()) * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0x63dcc2;
    }

    // Fabric disabling of nbt change animation
    @SuppressWarnings("unused")
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    // NeoForge disabling of nbt change animation
    @SuppressWarnings("unused")
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
