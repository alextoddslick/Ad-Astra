package earth.terrarium.adastra.client._compat;

/**
 * Stub replacement for {@code earth.terrarium.adastra.client._compat.CursorWidget},
 * removed in ResourcefulLib 4.0.1.
 *
 * TODO 26.1.2: restore cursor-aware widget behaviour.
 */
public interface CursorWidget {
    default CursorScreen.Cursor getCursor() {
        return CursorScreen.Cursor.DEFAULT;
    }
}
