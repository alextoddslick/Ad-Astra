package earth.terrarium.adastra.client.components.machines;

import com.teamresourceful.resourcefullib.client.components.CursorWidget;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import com.teamresourceful.resourcefullib.client.closables.CloseableScissor;
import earth.terrarium.adastra.client.components.base.TickableWidget;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.menus.configuration.EnergyConfiguration;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;

import java.time.Duration;

public class EnergyBarWidget extends ConfigurationWidget implements CursorWidget, TickableWidget {

    protected final ValueStorage container;
    protected long lastStoredEnergy;
    protected long difference;

    public EnergyBarWidget(EnergyConfiguration configuration) {
        super(configuration, GuiUtils.ENERGY_BAR_WIDTH, GuiUtils.ENERGY_BAR_HEIGHT);
        this.container = configuration.container();
    }

    @Override
    public void tick() {
        long currentEnergy = this.container.getStoredAmount();
        if (currentEnergy != this.lastStoredEnergy) {
            this.difference = currentEnergy - this.lastStoredEnergy;
            this.lastStoredEnergy = currentEnergy;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        long capacity = this.container.getCapacity();
        long energy = this.container.getStoredAmount();
        float ratio = energy / (float) capacity;
        int x = this.getX();
        int y = this.getY();
        int scissorY = y + GuiUtils.ENERGY_BAR_HEIGHT - (int) (GuiUtils.ENERGY_BAR_HEIGHT * ratio);
        try (var ignored = new CloseableScissor(graphics, x, scissorY, x + GuiUtils.ENERGY_BAR_WIDTH, scissorY + GuiUtils.ENERGY_BAR_HEIGHT)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.ENERGY_BAR, x, y, GuiUtils.ENERGY_BAR_WIDTH, GuiUtils.ENERGY_BAR_HEIGHT);
        }

        if (this.isHoveredOrFocused()) {
            setTooltip(Tooltip.create(CommonComponents.joinLines(
                TooltipUtils.getEnergyComponent(energy, capacity),
                TooltipUtils.getEnergyDifferenceComponent(this.difference),
                TooltipUtils.getMaxEnergyInComponent(container.getCapacity()),
                TooltipUtils.getMaxEnergyOutComponent(container.getCapacity())
            )));
            setTooltipDelay(Duration.ZERO);
        }
    }

    @Override
    public CursorScreen.Cursor getCursor() {
        return CursorScreen.Cursor.DEFAULT;
    }
}
