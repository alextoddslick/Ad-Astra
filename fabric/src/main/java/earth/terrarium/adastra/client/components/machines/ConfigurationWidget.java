package earth.terrarium.adastra.client.components.machines;

import earth.terrarium.adastra.client.screens.base.ConfigurationScreen;
import earth.terrarium.adastra.common.menus.configuration.MenuConfiguration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;

public abstract class ConfigurationWidget extends AbstractWidget {

    protected MenuConfiguration configuration;

    public ConfigurationWidget(MenuConfiguration configuration, int width, int height) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.configuration = configuration;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo button) {
        // Never intercept clicks — the configuration overlay should be render-only.
        // Otherwise it eats empty-cursor clicks on machine slots while the side-config
        // widget is open, breaking item extraction. Side-config selection is still
        // reachable via the gear-button cycle (OptionsBarWidget).
        return false;
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean bl) {
        ConfigurationScreen.ifPresent((screen) -> screen.getSideConfigWidget().setIndex(this.configuration.index()));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        ConfigurationScreen.ifPresent(
            (screen) -> {
                boolean isCorrectSlot = this.configuration.index() == screen.getSideConfigWidget().getIndex();
                int color = isCorrectSlot ? 0xFF00FF00 : 0xFF6666FF;
                graphics.outline(
                    this.getX() - 2, this.getY() - 2,
                    this.width + 4, this.height + 4,
                    color
                );
            }
        );
    }
}
