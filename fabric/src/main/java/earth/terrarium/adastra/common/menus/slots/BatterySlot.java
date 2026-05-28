package earth.terrarium.adastra.common.menus.slots;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;

public class BatterySlot extends ImageSlot {

    public static final Identifier BATTERY_SLOT_ICON = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "item/icons/battery_slot_icon");

    public BatterySlot(Container container, int slot) {
        super(container, slot, 0, 0, BATTERY_SLOT_ICON);
    }

    // TODO: getSlotTexture() was removed in 1.21.11 - custom slot textures need alternative rendering
}
