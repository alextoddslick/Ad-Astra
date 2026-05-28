package earth.terrarium.adastra.common.items;

import earth.terrarium.adastra.common.utils.TooltipUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class TooltipBlockItem extends BlockItem {

    private final Component description;

    public TooltipBlockItem(Block block, Component description, Properties properties) {
        super(block, properties);
        this.description = description;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        TooltipUtils.addDescriptionComponent(consumer, description);
    }
}
