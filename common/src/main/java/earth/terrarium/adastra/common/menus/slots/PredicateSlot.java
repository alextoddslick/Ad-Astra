package earth.terrarium.adastra.common.menus.slots;

import earth.terrarium.adastra.common.container.SingleSlotContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class PredicateSlot extends Slot {

    private final Predicate<ItemStack> predicate;

    public PredicateSlot(Container container, int slot, int x, int y, Predicate<ItemStack> predicate) {
        super(container, slot, x, y);
        this.predicate = predicate;
    }

    public static <T extends Recipe<RecipeInput>> PredicateSlot ofRecipeInput(Container container, int slot, int x, int y, Level level, RecipeType<T> type) {
        // Accept any item. The previous implementation looked up the recipe
        // manager via level.getServer().getRecipeManager() — which is null on
        // the client, so the predictive mayPlace check rejected every click
        // before it ever reached the server. (Affected cryo freezer etc.)
        //
        // In 1.21+, RecipeManager is server-only; clients only have a synced
        // RecipePropertySet subset which doesn't support generic getRecipeFor.
        // Rather than register a custom property set per recipe type, allow
        // placement; the server's recipeTick will simply not fire for items
        // that have no matching recipe — same UX as most tech mods.
        return new PredicateSlot(container, slot, x, y, item -> true);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return predicate.test(stack);
    }
}
