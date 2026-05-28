package earth.terrarium.adastra.common.items.armor;

import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
// FluidConstants removed - using BUCKET from parent class
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import net.minecraft.world.item.component.TooltipDisplay;

public class NetheriteSpaceSuitItem extends SpaceSuitItem {

    public NetheriteSpaceSuitItem(ArmorMaterial material, ArmorType type, long tankSize, Properties properties) {
        super(material, type, tankSize, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> consumer, @NotNull TooltipFlag isAdvanced) {
        consumer.accept(TooltipUtils.getFluidComponent(
            FluidUtils.getTank(stack),
            tankSize * BUCKET / 1000L,
            ModFluids.OXYGEN.get()));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.NETHERITE_SPACE_SUIT_INFO);
    }
}
