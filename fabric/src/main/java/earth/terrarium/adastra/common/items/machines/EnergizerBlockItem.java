package earth.terrarium.adastra.common.items.machines;

import earth.terrarium.adastra.common.blockentities.machines.EnergizerBlockEntity;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class EnergizerBlockItem extends BlockItem implements EnergyProvider.Item {

    public EnergizerBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof EnergizerBlockEntity entity)) {
            return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        }

        SimpleValueStorage itemEnergyContainer = getEnergyStorage(stack);
        ((SimpleValueStorage) entity.getEnergyStorage()).set(itemEnergyContainer.getStoredAmount());
        entity.onEnergyChange();

        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    public SimpleValueStorage getEnergyStorage(ItemStack holder) {
        return EnergyUtils.getItemEnergyStorage(holder, 2_000_000);
    }

    @Override
    public ValueStorage getEnergy(ItemStack stack, ItemContext context) {
        return getEnergyStorage(stack);
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


    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        var energy = getEnergyStorage(stack);
        consumer.accept(TooltipUtils.getEnergyComponent(energy.getStoredAmount(), energy.getCapacity()));
        consumer.accept(TooltipUtils.getMaxEnergyInComponent(energy.getCapacity()));
        consumer.accept(TooltipUtils.getMaxEnergyOutComponent(energy.getCapacity()));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.ENERGIZER_INFO);
    }
}
