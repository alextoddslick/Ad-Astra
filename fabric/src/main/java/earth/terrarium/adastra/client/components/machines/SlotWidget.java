package earth.terrarium.adastra.client.components.machines;

import earth.terrarium.adastra.client._compat.CursorWidget;
import earth.terrarium.adastra.client._compat.CursorScreen;
import earth.terrarium.adastra.common.menus.configuration.SlotConfiguration;

public class SlotWidget extends ConfigurationWidget implements CursorWidget {

    public SlotWidget(SlotConfiguration configuration) {
        super(configuration, configuration.width(), configuration.height());
    }

    @Override
    public CursorScreen.Cursor getCursor() {
        return CursorScreen.Cursor.DEFAULT;
    }
}
