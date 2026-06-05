package earth.terrarium.adastra.client.screens.machines;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.components.PressableImageButton;
import earth.terrarium.adastra.client.components.machines.OptionsBarWidget;
import earth.terrarium.adastra.client.screens.base.MachineScreen;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.adastra.common.blockentities.machines.NasaWorkbenchBlockEntity;
import earth.terrarium.adastra.common.menus.machines.NasaWorkbenchUpgradeMenu;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundOpenNasaWorkbenchMenuPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Dedicated suit-upgrade view of the NASA Workbench: only the armor + material + result slots are
 * shown (see {@link NasaWorkbenchUpgradeMenu}). The "back to crafting" button reopens the standard
 * workbench grid for the same block entity via a server-side menu-open packet.
 */
public class NasaWorkbenchUpgradeScreen extends MachineScreen<NasaWorkbenchUpgradeMenu, NasaWorkbenchBlockEntity> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/nasa_workbench_upgrade.png");

    public NasaWorkbenchUpgradeScreen(NasaWorkbenchUpgradeMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, STEEL_SLOT, 177, 184);
    }

    @Override
    public OptionsBarWidget.Builder createOptionsBar() {
        // "Back to crafting" button, top-right alongside the settings button.
        return super.createOptionsBar().addElement(new PressableImageButton(0, 0, 18, 18,
            GuiUtils.CRAFTING_BUTTON_SPRITES,
            button -> NetworkHandler.CHANNEL.sendToServer(
                new ServerboundOpenNasaWorkbenchMenuPacket(this.entity.getBlockPos(), false)),
            Component.translatable("tooltip.ad_astra.suit_upgrade.crafting")));
    }
}
