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
import earth.terrarium.adastra.common.registry.ModItems;
import earth.terrarium.adastra.common.utils.UpgradeUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Dedicated suit-upgrade view of the NASA Workbench: only the armor + material + result slots are
 * shown (see {@link NasaWorkbenchUpgradeMenu}). The "back to crafting" button reopens the standard
 * workbench grid for the same block entity via a server-side menu-open packet.
 *
 * Acts as its own recipe guide (no recipe viewer exists for MC 26.2 yet): two selector buttons at
 * the top of the panel pick which upgrade to preview; every empty slot then shows a greyed ghost
 * of what belongs there — armor piece, materials, and the expected result. Upgrade recipes are
 * shapeless, so the ghost grid is a shopping list, not a required arrangement.
 */
public class NasaWorkbenchUpgradeScreen extends MachineScreen<NasaWorkbenchUpgradeMenu, NasaWorkbenchBlockEntity> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/container/nasa_workbench_upgrade.png");

    private record UpgradePattern(ItemStack armor, ItemStack[] grid, ItemStack result, Component name) {}

    private static List<UpgradePattern> patterns;

    // Selector button geometry (screen-relative). Top-right of the panel, beside the title —
    // clear of the grid, the output slot, and the "Inventory" label.
    private static final int[][] SELECTOR_POS = {{128, 4}, {148, 4}};

    private int selectedUpgrade = 0;

    public NasaWorkbenchUpgradeScreen(NasaWorkbenchUpgradeMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, TEXTURE, STEEL_SLOT, 177, 184);
    }

    private static List<UpgradePattern> patterns() {
        if (patterns == null) {
            ItemStack plate = new ItemStack(ModItems.STEEL_PLATE.get());
            ItemStack etrium = new ItemStack(ModItems.ETRIUM_INGOT.get());
            ItemStack core = new ItemStack(ModItems.ETRIONIC_CORE.get());

            ItemStack boostedBoots = new ItemStack(ModItems.JET_SUIT_BOOTS.get());
            UpgradeUtils.set(boostedBoots, UpgradeUtils.BOOST_MODE, true);
            ItemStack visorHelmet = new ItemStack(ModItems.JET_SUIT_HELMET.get());
            UpgradeUtils.set(visorHelmet, UpgradeUtils.ANALYSIS_VISOR, true);

            patterns = List.of(
                new UpgradePattern(
                    new ItemStack(ModItems.JET_SUIT_BOOTS.get()),
                    new ItemStack[]{
                        plate, plate, plate,
                        etrium, ItemStack.EMPTY, etrium,
                        core, new ItemStack(ModItems.CALORITE_ENGINE.get()), core
                    },
                    boostedBoots,
                    Component.translatable("tooltip.ad_astra.upgrade.boost_mode")),
                new UpgradePattern(
                    new ItemStack(ModItems.JET_SUIT_HELMET.get()),
                    new ItemStack[]{
                        plate, plate, plate,
                        core, ItemStack.EMPTY, core,
                        ItemStack.EMPTY, new ItemStack(Items.GLASS_PANE), ItemStack.EMPTY
                    },
                    visorHelmet,
                    Component.translatable("tooltip.ad_astra.upgrade.analysis_visor"))
            );
        }
        return patterns;
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        List<UpgradePattern> all = patterns();
        UpgradePattern pattern = all.get(selectedUpgrade);

        // Selector buttons: pick which upgrade recipe to preview.
        for (int i = 0; i < all.size(); i++) {
            int x = leftPos + SELECTOR_POS[i][0];
            int y = topPos + SELECTOR_POS[i][1];
            boolean selected = i == selectedUpgrade;
            int frame = selected ? 0xFFFFAA00 : 0xFF555555;
            graphics.fill(x, y, x + 18, y + 1, frame);
            graphics.fill(x, y + 17, x + 18, y + 18, frame);
            graphics.fill(x, y, x + 1, y + 18, frame);
            graphics.fill(x + 17, y, x + 18, y + 18, frame);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, selected ? 0x40FFAA00 : 0x40000000);
            graphics.item(all.get(i).armor(), x + 1, y + 1);
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                graphics.setTooltipForNextFrame(font, all.get(i).name(), mouseX, mouseY);
            }
        }

        // Ghost hints in every empty slot: armor piece, materials, and the expected result.
        ghost(graphics, pattern.armor(), 0, leftPos + 8, topPos + 36);
        for (int cell = 0; cell < 9; cell++) {
            ghost(graphics, pattern.grid()[cell], 1 + cell,
                leftPos + 38 + (cell % 3) * 18, topPos + 18 + (cell / 3) * 18);
        }
        ghost(graphics, pattern.result(), NasaWorkbenchUpgradeMenu.OUTPUT_SLOT, leftPos + 124, topPos + 36);
    }

    private void ghost(GuiGraphicsExtractor graphics, ItemStack stack, int slotIndex, int x, int y) {
        if (stack.isEmpty() || menu.getSlot(slotIndex).hasItem()) return;
        graphics.fakeItem(stack, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x808B8B8B);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < patterns().size(); i++) {
            int x = leftPos + SELECTOR_POS[i][0];
            int y = topPos + SELECTOR_POS[i][1];
            if (event.x() >= x && event.x() < x + 18 && event.y() >= y && event.y() < y + 18) {
                selectedUpgrade = i;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
