package earth.terrarium.adastra.common.compat.rei.displays;

import earth.terrarium.adastra.common.compat.rei.categories.CryoFreezingCategory;
import earth.terrarium.adastra.common.recipes.machines.CryoFreezingRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public record CryoFreezingDisplay(CryoFreezingRecipe recipe) implements Display {

    public CryoFreezingDisplay(RecipeHolder<CryoFreezingRecipe> recipe) {
        this(recipe.value());
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return List.of(EntryIngredients.ofIngredient(recipe.input()));
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return List.of();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CryoFreezingCategory.ID;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.empty();
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
