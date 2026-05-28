package earth.terrarium.adastra.client.screens.machines;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.screens.base.MachineScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.blockentities.machines.CompressorBlockEntity;
import earth.terrarium.adastra.common.menus.machines.CompressorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class CompressorScreen extends MachineScreen<CompressorMenu, CompressorBlockEntity> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/compressor.png");
    public static final Rect2i CLICK_AREA = new Rect2i(41, 25, 26, 25);

    public CompressorScreen(CompressorMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, STEEL_SLOT, 184, 201);
        this.titleLabelY += 3;
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        int totalItems = entity.getItem(1).getCount();
        GuiUtils.drawHorizontalProgressBar(
            graphics, GuiUtils.HAMMER, mouseX, mouseY,
            leftPos + 72, topPos + 59, 15, 16,
            entity.cookTime(), entity.cookTimeTotal(), false,
            earth.terrarium.adastra.common.utils.TooltipUtils.getProgressComponent(entity.cookTime(), entity.cookTimeTotal()),
            earth.terrarium.adastra.common.utils.TooltipUtils.getTotalEtaComponent(entity.cookTime(), entity.cookTimeTotal(), totalItems));
    }
}
