package earth.terrarium.adastra.common.compat.jei.drawables;

import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public class FluidBarDrawable implements IDrawable {

    private final int mouseX;
    private final int mouseY;
    private final boolean gain;
    private final long perTick;
    private final int cookTime;
    private final long capacity;
    private final Fluid fluid;

    public FluidBarDrawable(double mouseX, double mouseY, boolean generate, long capacity, int cookTime, Fluid fluid, long amount) {
        this.mouseX = (int) mouseX;
        this.mouseY = (int) mouseY;
        this.gain = generate;
        this.perTick = amount;
        this.cookTime = cookTime;
        this.capacity = capacity;
        this.fluid = fluid;
    }

    @Override
    public int getWidth() {
        return GuiUtils.FLUID_BAR_WIDTH;
    }

    @Override
    public int getHeight() {
        return GuiUtils.FLUID_BAR_HEIGHT;
    }

    @Override
    public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
        if (fluid == null || capacity <= 0) return;
        long displayAmount = gain ? perTick * cookTime : capacity - perTick * cookTime;
        displayAmount = Math.max(0, Math.min(capacity, displayAmount));
        float ratio = displayAmount / (float) capacity;
        int fillHeight = (int) (getHeight() * ratio);
        if (fillHeight > 0) {
            GuiUtils.renderFluidFill(graphics, fluid, xOffset, yOffset + getHeight() - fillHeight, getWidth(), fillHeight);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.FLUID_BAR, xOffset, yOffset, getWidth(), getHeight());

        if (mouseX >= xOffset && mouseX < xOffset + getWidth() && mouseY >= yOffset && mouseY < yOffset + getHeight()) {
            Component tooltip = TooltipUtils.getFluidComponent(displayAmount, capacity, fluid);
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font,
                java.util.List.of(tooltip.getVisualOrderText()), mouseX, mouseY);
        }
    }
}
