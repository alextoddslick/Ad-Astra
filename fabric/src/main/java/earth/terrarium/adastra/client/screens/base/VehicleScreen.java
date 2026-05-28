package earth.terrarium.adastra.client.screens.base;

import earth.terrarium.adastra.client._compat.AbstractContainerCursorScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.menus.base.BaseEntityContainerMenu;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        super(menu, inventory, component, width, height);
        this.texture = texture;
        this.entity = menu.getEntity();

        this.inventoryLabelX = width - 180;
        this.inventoryLabelY = height - 94;
        this.titleLabelX = width - 180;
        this.titleLabelY = 6;
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, left - 8, top, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // Same fix as MachineScreen: extractContents is an orchestrator, not a 1:1
        // replacement for the old renderBg. Without chaining to super, slots/items,
        // labels, highlights and child widgets all silently stop rendering — which
        // is why the rocket UI showed an empty player inventory.
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, this.title, this.titleLabelX, this.titleLabelY, this.getTextColor(), false);
        graphics.text(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.getTextColor(), false);
    }

    public int getTextColor() {
        return 0xFF2a262b;
    }

    public void drawFluidBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int xOffset, int yOffset, FluidResource fluid, long amount, long capacity) {
        int x = this.leftPos + xOffset;
        int y = this.topPos + yOffset;
        GuiUtils.drawFluidBar(graphics, mouseX, mouseY, x, y, fluid, amount, capacity);
    }
}
