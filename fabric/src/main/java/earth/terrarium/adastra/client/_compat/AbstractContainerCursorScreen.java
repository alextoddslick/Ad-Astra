package earth.terrarium.adastra.client._compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Stub replacement for {@code earth.terrarium.adastra.client._compat.AbstractContainerCursorScreen},
 * removed in ResourcefulLib 4.0.1. Falls back to vanilla {@link AbstractContainerScreen}.
 *
 * TODO 26.1.2: restore cursor change handling.
 */
public abstract class AbstractContainerCursorScreen<T extends AbstractContainerMenu>
    extends AbstractContainerScreen<T>
    implements CursorScreen {

    public AbstractContainerCursorScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public AbstractContainerCursorScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }
}
