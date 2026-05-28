package earth.terrarium.adastra.client.screens.base;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

public interface AbstractContainerScreenExtension {

    default void adastra$renderPreSlot(GuiGraphicsExtractor graphics, Slot slot) {
    }
}
