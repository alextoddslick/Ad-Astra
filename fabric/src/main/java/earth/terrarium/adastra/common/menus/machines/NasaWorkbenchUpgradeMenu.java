package earth.terrarium.adastra.common.menus.machines;

import earth.terrarium.adastra.common.blockentities.machines.NasaWorkbenchBlockEntity;
import earth.terrarium.adastra.common.menus.base.BaseContainerMenu;
import earth.terrarium.adastra.common.menus.slots.CustomSlot;
import earth.terrarium.adastra.common.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

/**
 * A focused secondary view of the {@link NasaWorkbenchBlockEntity}. It exposes only the slots a
 * suit-upgrade recipe touches: the armor input (container slot 0), the material inputs (container
 * slots 1-8) and the result preview (container slot 14). It shares the same block entity container
 * as {@link NasaWorkbenchMenu}, so crafting (driven server-side in the BE tick) and item state are
 * identical regardless of which view the player has open.
 */
public class NasaWorkbenchUpgradeMenu extends BaseContainerMenu<NasaWorkbenchBlockEntity> {

    /** Slot-list index of the result preview: armor (0) + 9 grid cells (1..9), then the output at 10. */
    public static final int OUTPUT_SLOT = 10;

    public NasaWorkbenchUpgradeMenu(int id, Inventory inventory, NasaWorkbenchBlockEntity entity) {
        super(ModMenus.NASA_WORKBENCH_UPGRADE.get(), id, inventory, entity);
    }

    @Override
    protected int getContainerInputEnd() {
        return 10;
    }

    @Override
    protected int getInventoryStart() {
        return 10;
    }

    @Override
    protected int startIndex() {
        return 0;
    }

    @Override
    public int getPlayerInvXOffset() {
        return 8;
    }

    @Override
    public int getPlayerInvYOffset() {
        return 101;
    }

    @Override
    protected void addMenuSlots() {
        // Dedicated armor slot on the left (container slot 0) — the helmet/boots piece goes here.
        // Upgrade recipes match ingredient i -> container slot i, and every upgrade recipe lists the
        // armor piece as ingredient 0, so this slot is the recipe's armor input.
        addSlot(new Slot(entity, 0, 8, 36));
        // Materials 3x3 grid (container slots 1-9) — recipe ingredients 1..N (the materials), in
        // left-to-right, top-to-bottom reading order.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int i = 1 + row * 3 + col;
                addSlot(new Slot(entity, i, 38 + col * 18, 18 + row * 18));
            }
        }
        // Container slot 14 = result preview. The BE fills it and the server-side auto-craft drops the item.
        addSlot(CustomSlot.noPlaceOrTake(entity, 14, 124, 36));
    }

    @Override
    public void clicked(int slotIndex, int button, @NotNull ContainerInput actionType, @NotNull Player player) {
        super.clicked(slotIndex, button, actionType, player);
        if (slotIndex == OUTPUT_SLOT && entity.canCraft()) {
            entity.craft();
        }
    }
}
