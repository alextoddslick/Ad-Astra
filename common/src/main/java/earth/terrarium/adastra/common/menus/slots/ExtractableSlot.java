package earth.terrarium.adastra.common.menus.slots;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExtractableSlot extends Slot {

    private final boolean canPlace;

    public ExtractableSlot(Container container, int slot, int x, int y, boolean canPlace) {
        super(container, slot, x, y);
        this.canPlace = canPlace;
    }

    public static ExtractableSlot input(Container c, int slot, int x, int y) {
        return new ExtractableSlot(c, slot, x, y, true);
    }

    public static ExtractableSlot output(Container c, int slot, int x, int y) {
        return new ExtractableSlot(c, slot, x, y, false);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return canPlace;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return true;
    }

    @Override
    public boolean allowModification(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull Optional<ItemStack> tryRemove(int count, int maxCount, @NotNull Player player) {
        int actual = Math.min(count, maxCount);
        if (actual <= 0 || this.getItem().isEmpty()) return Optional.empty();
        ItemStack stack = this.remove(actual);
        if (stack.isEmpty()) return Optional.empty();
        if (this.getItem().isEmpty()) this.set(ItemStack.EMPTY);
        return Optional.of(stack);
    }
}
