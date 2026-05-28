package earth.terrarium.adastra.client.screens.machines;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.components.machines.OptionBarOptions;
import earth.terrarium.adastra.client.components.machines.OptionsBarWidget;
import earth.terrarium.adastra.client.screens.base.MachineScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import earth.terrarium.adastra.common.menus.machines.EtrionicBlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class EtrionicBlastFurnaceScreen extends MachineScreen<EtrionicBlastFurnaceMenu, EtrionicBlastFurnaceBlockEntity> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/etrionic_blast_furnace.png");
    public static final Identifier FURNACE_OVERLAY = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "etrionic_blast_furnace_overlay");
    public static final Identifier FURNACE_OVERLAY_FULL_TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/etrionic_blast_furnace_overlay.png");
    public static final Rect2i CLICK_AREA = new Rect2i(23, 79, 45, 19);

    public EtrionicBlastFurnaceScreen(EtrionicBlastFurnaceMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, STEEL_SLOT, 184, 201);
        this.titleLabelY += 3;
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        // BLASTING crafts each input slot in parallel, so the queue length is the
        // largest stack across the four input slots — that's how many cycles before
        // everything is processed.
        int totalItems = 0;
        for (int i = 1; i <= 4; i++) {
            int c = entity.getItem(i).getCount();
            if (c > totalItems) totalItems = c;
        }
        GuiUtils.drawHorizontalProgressBar(
            graphics, GuiUtils.ARROW, mouseX, mouseY,
            leftPos + 75, topPos + 50, 20, 12,
            entity.cookTime(), entity.cookTimeTotal(), false,
            earth.terrarium.adastra.common.utils.TooltipUtils.getProgressComponent(entity.cookTime(), entity.cookTimeTotal()),
            earth.terrarium.adastra.common.utils.TooltipUtils.getTotalEtaComponent(entity.cookTime(), entity.cookTimeTotal(), totalItems));
        if (entity.cookTimeTotal() > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FURNACE_OVERLAY, leftPos + 30, topPos + 51, 32, 43);
        }
    }

    @Override
    public OptionsBarWidget.Builder createOptionsBar() {
        return super.createOptionsBar()
            .addElement(0, OptionBarOptions.createBlastFurnaceMode(entity));
    }
}
