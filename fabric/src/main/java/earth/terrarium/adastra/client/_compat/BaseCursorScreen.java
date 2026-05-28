package earth.terrarium.adastra.client._compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Stub replacement for {@code earth.terrarium.adastra.client._compat.BaseCursorScreen},
 * which was removed in ResourcefulLib 4.0.1. Falls back to vanilla {@link Screen} behaviour.
 *
 * TODO 26.1.2: restore cursor change handling.
 */
public abstract class BaseCursorScreen extends Screen implements CursorScreen {

    protected BaseCursorScreen(Component title) {
        super(title);
    }
}
