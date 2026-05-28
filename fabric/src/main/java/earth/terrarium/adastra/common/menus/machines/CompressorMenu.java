package earth.terrarium.adastra.common.menus.machines;

import earth.terrarium.adastra.common.blockentities.machines.CompressorBlockEntity;
import earth.terrarium.adastra.common.menus.base.BaseContainerMenu;
import earth.terrarium.adastra.common.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * NOTE: Refactored to extend {@link BaseContainerMenu} directly (mirroring
 * {@code NasaWorkbenchMenu}'s class hierarchy) instead of {@code MachineMenu} ->
 * {@code BaseConfigurableContainerMenu} -> {@code BaseContainerMenu}. This isolates
 * whether the item-extraction bug originates in those parent classes.
 *
 * Battery slot is added as a vanilla {@link Slot} (not {@code BatterySlot}). All
 * machine slots are vanilla {@link Slot} (no {@code ExtractableSlot}, no
 * {@code CustomSlot}). No side-config UI hooks remain.
 */
public class CompressorMenu extends BaseContainerMenu<CompressorBlockEntity> {

    public CompressorMenu(int id, Inventory inventory, CompressorBlockEntity entity) {
        super(ModMenus.COMPRESSOR.get(), id, inventory, entity);
    }

    @Override
    protected int getContainerInputEnd() {
        return 3;
    }

    @Override
    protected int getInventoryStart() {
        return 3;
    }

    @Override
    protected int startIndex() {
        return 1;
    }

    @Override
    public int getPlayerInvXOffset() {
        return 12;
    }

    @Override
    public int getPlayerInvYOffset() {
        return 114;
    }

    @Override
    protected void addMenuSlots() {
        // Battery slot at slot 0 (static coords; the MachineScreen battery widget no
        // longer repositions it because the menu isn't a MachineMenu instance).
        addSlot(new Slot(entity, 0, 152, 8));
        // Input slot 1 / output slot 2 -- vanilla Slot, not ExtractableSlot.
        addSlot(new Slot(entity, 1, 47, 58));
        addSlot(new Slot(entity, 2, 95, 58));
    }
}
