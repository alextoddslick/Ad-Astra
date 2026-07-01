package earth.terrarium.adastra.client.components.machines;

import earth.terrarium.adastra.client._compat.CursorWidget;
import earth.terrarium.adastra.client._compat.CursorScreen;
import com.teamresourceful.resourcefullib.client.closables.CloseableScissor;
import earth.terrarium.adastra.client.components.base.TickableWidget;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.menus.configuration.EnergyConfiguration;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;

import java.time.Duration;

public class EnergyBarWidget extends ConfigurationWidget implements CursorWidget, TickableWidget {

    private static final int RATE_WINDOW_TICKS = 20;

    protected final ValueStorage container;
    protected long lastStoredEnergy;
    protected long windowIn;
    protected long windowOut;
    protected long inPerTick;
    protected long outPerTick;
    protected int windowTick;

    public EnergyBarWidget(EnergyConfiguration configuration) {
        super(configuration, GuiUtils.ENERGY_BAR_WIDTH, GuiUtils.ENERGY_BAR_HEIGHT);
        this.container = configuration.container();
    }

    @Override
    public void tick() {
        long currentEnergy = this.container.getStoredAmount();
        long delta = currentEnergy - this.lastStoredEnergy;
        this.lastStoredEnergy = currentEnergy;
        if (delta > 0) this.windowIn += delta;
        else if (delta < 0) this.windowOut -= delta;

        // A single net delta per tick can't reveal simultaneous in+out flow, so accumulate
        // each direction separately over a short window and report both once it fills.
        if (++this.windowTick >= RATE_WINDOW_TICKS) {
            this.inPerTick = this.windowIn / RATE_WINDOW_TICKS;
            this.outPerTick = this.windowOut / RATE_WINDOW_TICKS;
            this.windowIn = 0;
            this.windowOut = 0;
            this.windowTick = 0;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
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
                TooltipUtils.getEnergyInComponent(this.inPerTick),
                TooltipUtils.getEnergyOutComponent(this.outPerTick),
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
