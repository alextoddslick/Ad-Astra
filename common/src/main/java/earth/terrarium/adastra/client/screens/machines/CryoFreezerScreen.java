package earth.terrarium.adastra.client.screens.machines;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.screens.base.MachineScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.blockentities.machines.CryoFreezerBlockEntity;
import earth.terrarium.adastra.common.menus.machines.CryoFreezerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class CryoFreezerScreen extends MachineScreen<CryoFreezerMenu, CryoFreezerBlockEntity> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/cryo_freezer.png");
    public static final Identifier CRYO_SLOT = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/slots/cryo.png");

    public static final Rect2i CLICK_AREA = new Rect2i(108, 10, 26, 25);

    public CryoFreezerScreen(CryoFreezerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, CRYO_SLOT, 177, 184);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int totalItems = entity.getItem(1).getCount();
        GuiUtils.drawHorizontalProgressBar(
            graphics, GuiUtils.SNOWFLAKE, mouseX, mouseY,
            leftPos + 54, topPos + 71, 13, 13,
            entity.cookTime(), entity.cookTimeTotal(), false,
            earth.terrarium.adastra.common.utils.TooltipUtils.getProgressComponent(entity.cookTime(), entity.cookTimeTotal()),
            earth.terrarium.adastra.common.utils.TooltipUtils.getTotalEtaComponent(entity.cookTime(), entity.cookTimeTotal(), totalItems));
    }
}
