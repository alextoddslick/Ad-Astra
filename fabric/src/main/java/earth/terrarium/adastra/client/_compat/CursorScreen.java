package earth.terrarium.adastra.client._compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Stub replacement for {@code earth.terrarium.adastra.client._compat.CursorScreen},
 * which was removed in ResourcefulLib 4.0.1. The cursor-changing behaviour is a
 * no-op for now; visual parity is not required for the 26.1.2 port.
 *
 * TODO 26.1.2: restore cursor change support (originally driven by GLFW SetCursor).
 */
public interface CursorScreen {

    enum Cursor {
        DEFAULT,
        POINTER,
        TEXT;

        public void apply(GuiGraphicsExtractor graphics) {
            // TODO 26.1.2: implement cursor-switching via GLFW
        }
    }
}
