package earth.terrarium.adastra.client.screens.vehicles;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.screens.base.VehicleScreen;
import earth.terrarium.adastra.common.entities.vehicles.Rover;
import earth.terrarium.adastra.common.menus.vehicles.RoverMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class RoverScreen extends VehicleScreen<RoverMenu, Rover> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/rover.png");

    public RoverScreen(RoverMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, 177, 181);
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        this.drawFluidBar(graphics, mouseX, mouseY, 37, 57, entity.fluidResource(), entity.fluidAmount(), entity.fluidCapacity());
    }
}
