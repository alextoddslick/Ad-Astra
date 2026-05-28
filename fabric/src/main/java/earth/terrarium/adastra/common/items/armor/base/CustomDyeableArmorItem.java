package earth.terrarium.adastra.common.items.armor.base;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class CustomDyeableArmorItem extends Item {

    protected final ArmorType type;

    public CustomDyeableArmorItem(ArmorMaterial armorMaterial, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(armorMaterial, type));
        this.type = type;
    }

    // Makes the default color white instead of brown
    public static int getColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, 0xFFFFFFFF);
    }
}
