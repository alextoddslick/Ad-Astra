package earth.terrarium.adastra.common.recipes.machines;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import earth.terrarium.adastra.common.recipes.AdAstraItemStackCodec;
import net.minecraft.world.item.crafting.Recipe;
import earth.terrarium.adastra.common.registry.ModRecipeSerializers;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record NasaWorkbenchRecipe(
    List<Ingredient> ingredients,
    ItemStack result,
    boolean shapeless
) implements Recipe<RecipeInput> {

    private static final RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    /** The workbench container's result-preview slot; never part of the recipe inputs. */
    private static final int OUTPUT_SLOT = 14;

    public static final MapCodec<NasaWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(NasaWorkbenchRecipe::ingredients),
            AdAstraItemStackCodec.CODEC.fieldOf("result").forGetter(NasaWorkbenchRecipe::result),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("shapeless", false).forGetter(NasaWorkbenchRecipe::shapeless)
        ).apply(instance, NasaWorkbenchRecipe::new));

    public static final ByteCodec<NasaWorkbenchRecipe> NETWORK_CODEC = ObjectByteCodec.create(
        ExtraByteCodecs.INGREDIENT.listOf().fieldOf(NasaWorkbenchRecipe::ingredients),
        ExtraByteCodecs.ITEM_STACK.fieldOf(NasaWorkbenchRecipe::result),
        ByteCodec.BOOLEAN.fieldOf(NasaWorkbenchRecipe::shapeless),
        NasaWorkbenchRecipe::new
    );

    @Override
    public boolean matches(@NotNull RecipeInput container, @NotNull Level level) {
        if (shapeless) return matchesShapeless(container);
        if (container.size() < ingredients.size()) return false;
        for (int i = 0; i < ingredients.size(); i++) {
            if (!ingredients.get(i).test(container.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Position-independent matching (suit-upgrade recipes): the non-empty input slots must be
     * exactly the ingredient multiset — any arrangement, no extras. The result-preview slot is
     * excluded; the workbench writes the pending result there while inputs still match.
     */
    private boolean matchesShapeless(RecipeInput container) {
        List<ItemStack> items = new java.util.ArrayList<>();
        int inputSlots = Math.min(container.size(), OUTPUT_SLOT);
        for (int i = 0; i < inputSlots; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }
        if (items.size() != ingredients.size()) return false;
        return matchRemaining(items, 0, new boolean[items.size()]);
    }

    private boolean matchRemaining(List<ItemStack> items, int ingredientIndex, boolean[] used) {
        if (ingredientIndex >= ingredients.size()) return true;
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int i = 0; i < items.size(); i++) {
            if (used[i] || !ingredient.test(items.get(i))) continue;
            used[i] = true;
            if (matchRemaining(items, ingredientIndex + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        return result.copy();
    }



    @Override
    public @NotNull RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.NASA_WORKBENCH_SERIALIZER.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull RecipeType<NasaWorkbenchRecipe> getType() {
        return (RecipeType<NasaWorkbenchRecipe>) (RecipeType<?>) ModRecipeTypes.NASA_WORKBENCH.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return BOOK_CATEGORY;
    }
    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }
}
