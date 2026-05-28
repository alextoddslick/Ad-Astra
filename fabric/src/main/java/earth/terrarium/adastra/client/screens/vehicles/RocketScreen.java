package earth.terrarium.adastra.client.screens.vehicles;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.screens.base.VehicleScreen;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.menus.vehicles.RocketMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class RocketScreen extends VehicleScreen<RocketMenu, Rocket> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/rocket.png");

    public RocketScreen(RocketMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, 177, 174);
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        this.drawFluidBar(graphics, mouseX, mouseY, 37, 55, entity.fluidResource(), entity.fluidAmount(), entity.fluidCapacity());
    }
}
