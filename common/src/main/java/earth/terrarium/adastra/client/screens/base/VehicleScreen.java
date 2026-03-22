package earth.terrarium.adastra.client.screens.base;

import com.teamresourceful.resourcefullib.client.screens.AbstractContainerCursorScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.menus.base.BaseEntityContainerMenu;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class VehicleScreen<T extends BaseEntityContainerMenu<U>, U extends Entity> extends AbstractContainerCursorScreen<T> {

    private final Identifier texture;

    protected final U entity;

    public VehicleScreen(T menu, Inventory inventory, Component component, Identifier texture, int width, int height) {
        super(menu, inventory, component);
        this.texture = texture;
        this.imageWidth = width;
        this.imageHeight = height;
        this.entity = menu.getEntity();

        this.inventoryLabelX = width - 180;
        this.inventoryLabelY = height - 94;
        this.titleLabelX = width - 180;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, left - 8, top, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, this.titleLabelX, this.titleLabelY, this.getTextColor(), false);
        graphics.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.getTextColor(), false);
    }

    public int getTextColor() {
        return 0xFF2a262b;
    }

    public void drawFluidBar(GuiGraphics graphics, int mouseX, int mouseY, int xOffset, int yOffset, FluidResource fluid, long amount, long capacity) {
        int x = this.leftPos + xOffset;
        int y = this.topPos + yOffset;
        GuiUtils.drawFluidBar(graphics, mouseX, mouseY, x, y, fluid, amount, capacity);
    }
}
