package earth.terrarium.adastra.client.components.machines;

import earth.terrarium.adastra.client._compat.CursorWidget;
import earth.terrarium.adastra.client._compat.CursorScreen;
import earth.terrarium.adastra.client.components.base.TickableWidget;
import earth.terrarium.adastra.client.screens.base.ConfigurationScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.menus.configuration.FluidConfiguration;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundClearFluidTankPacket;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidSlot;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;

import java.time.Duration;

public class FluidBarWidget extends ConfigurationWidget implements CursorWidget, TickableWidget {

    private static final int RATE_WINDOW_TICKS = 20;

    protected final BlockPos tankPos;
    protected final int tank;
    protected final SimpleFluidStorage container;
    protected long lastFluidAmount;
    protected long windowIn;
    protected long windowOut;
    protected long inPerTick;
    protected long outPerTick;
    protected int windowTick;

    public FluidBarWidget(BlockPos tankPos, FluidConfiguration configuration) {
        super(configuration, GuiUtils.FLUID_BAR_WIDTH, GuiUtils.FLUID_BAR_HEIGHT);
        this.tankPos = tankPos;
        this.tank = configuration.tank();
        this.container = configuration.container();
    }

    @Override
    public void tick() {
        SimpleFluidSlot slot = container.get(tank);
        long currentAmount = slot.getAmount();
        long delta = currentAmount - this.lastFluidAmount;
        this.lastFluidAmount = currentAmount;
        if (delta > 0) this.windowIn += delta;
        else if (delta < 0) this.windowOut -= delta;

        // A single net delta per tick can't reveal simultaneous in+out flow (e.g. a pipe
        // filling the tank while the machine drains it), so accumulate each direction
        // separately over a short window and report both once it fills.
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
        int x = this.getX();
        int y = this.getY();

        SimpleFluidSlot slot = container.get(tank);
        long amount = slot.getAmount();
        long capacity = slot.getLimit(FluidResource.BLANK);

        if (capacity > 0 && amount > 0) {
            float ratio = amount / (float) capacity;
            int fillHeight = (int) (GuiUtils.FLUID_BAR_HEIGHT * ratio);
            int fillY = y + GuiUtils.FLUID_BAR_HEIGHT - fillHeight;

            // Render the fluid texture tinted with the fluid's color
            FluidResource resource = slot.getResource();
            GuiUtils.renderFluidFill(graphics, resource.getType(), x, fillY, GuiUtils.FLUID_BAR_WIDTH, fillHeight);
        }

        // Always draw the bar frame overlay on top
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.FLUID_BAR, x, y, GuiUtils.FLUID_BAR_WIDTH, GuiUtils.FLUID_BAR_HEIGHT);

        if (this.isHoveredOrFocused()) {
            FluidResource resource = slot.getResource();
            setTooltip(Tooltip.create(CommonComponents.joinLines(
                TooltipUtils.getFluidComponent(resource, amount, capacity),
                TooltipUtils.getFluidInComponent(this.inPerTick),
                TooltipUtils.getFluidOutComponent(this.outPerTick)
            )));
            setTooltipDelay(Duration.ZERO);
        }
    }

    @Override
    protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo button) {
        return super.isValidClickButton(button) || (button.button() == 1 && net.minecraft.client.Minecraft.getInstance().hasShiftDown());
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean bl) {
        if (ConfigurationScreen.isConfigurable()) {
            super.onClick(event, bl);
        } else {
            NetworkHandler.CHANNEL.sendToServer(new ServerboundClearFluidTankPacket(this.tankPos, this.tank));
        }
    }

    @Override
    public CursorScreen.Cursor getCursor() {
        return net.minecraft.client.Minecraft.getInstance().hasShiftDown() ? CursorScreen.Cursor.POINTER : CursorScreen.Cursor.DEFAULT;
    }
}
