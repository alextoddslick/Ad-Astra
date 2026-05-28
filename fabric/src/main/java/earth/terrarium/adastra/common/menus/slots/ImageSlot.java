package earth.terrarium.adastra.common.menus.slots;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public class ImageSlot extends Slot {

    private final Identifier icon;

    public ImageSlot(Container container, int slot, int xPosition, int yPosition, Identifier icon) {
        super(container, slot, xPosition, yPosition);
        this.icon = icon;
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return icon;
    }
}