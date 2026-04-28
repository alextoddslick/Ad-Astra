package earth.terrarium.adastra.common.compat.rei.widgets;

import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReiFluidBarWidget extends Widget {

    private final Rectangle bounds;
    private final boolean gain;
    private final long perTick;
    private final int cookTime;
    private final long capacity;
    private final Fluid fluid;

    public ReiFluidBarWidget(Point point, boolean generate, long capacity, int cookTime, Fluid fluid, long amount) {
        this.bounds = new Rectangle(point.x, point.y, GuiUtils.FLUID_BAR_WIDTH, GuiUtils.FLUID_BAR_HEIGHT);
        this.gain = generate;
        this.perTick = amount;
        this.cookTime = cookTime;
        this.capacity = capacity;
        this.fluid = fluid;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (fluid == null || capacity <= 0) return;
        long displayAmount = gain ? perTick * cookTime : capacity - perTick * cookTime;
        displayAmount = Math.max(0, Math.min(capacity, displayAmount));
        float ratio = displayAmount / (float) capacity;
        int fillHeight = (int) (bounds.getHeight() * ratio);
        if (fillHeight > 0) {
            GuiUtils.renderFluidFill(graphics, fluid, bounds.x, bounds.y + bounds.getHeight() - fillHeight, bounds.getWidth(), fillHeight);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.FLUID_BAR, bounds.x, bounds.y, bounds.getWidth(), bounds.getHeight());

        if (mouseX >= bounds.x && mouseX < bounds.getMaxX() && mouseY >= bounds.y && mouseY < bounds.getMaxY()) {
            Component tooltip = TooltipUtils.getFluidComponent(displayAmount, capacity, fluid);
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font,
                List.of(tooltip.getVisualOrderText()), mouseX, mouseY);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return List.of();
    }
}
