package earth.terrarium.adastra.common.recipes.machines;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import earth.terrarium.adastra.common.recipes.AdAstraItemStackCodec;
import net.minecraft.world.item.crafting.Recipe;
import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import earth.terrarium.adastra.common.registry.ModRecipeSerializers;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.adastra.common.utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AlloyingRecipe(
    int cookingTime, int energy,
    List<Ingredient> ingredients, ItemStack result
) implements Recipe<RecipeInput> {

    private static final RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.fieldOf("cookingtime").forGetter(AlloyingRecipe::cookingTime),
            Codec.INT.fieldOf("energy").forGetter(AlloyingRecipe::energy),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            AdAstraItemStackCodec.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result)
        ).apply(instance, AlloyingRecipe::new));

    public static final ByteCodec<AlloyingRecipe> NETWORK_CODEC = ObjectByteCodec.create(
        ByteCodec.INT.fieldOf(AlloyingRecipe::cookingTime),
        ByteCodec.INT.fieldOf(AlloyingRecipe::energy),
        ExtraByteCodecs.INGREDIENT.listOf().fieldOf(AlloyingRecipe::ingredients),
        ExtraByteCodecs.ITEM_STACK.fieldOf(AlloyingRecipe::result),
        AlloyingRecipe::new
    );

    @Override
    public boolean matches(@NotNull RecipeInput container, @NotNull Level level) {
        if (container.size() < ingredients.size()) return false;
        for (int i = 0; i < Math.min(4, ingredients.size()); i++) {
            boolean found = false;
            for (int j = 0; j < 4; j++) {
                if (ingredients.get(i).test(container.getItem(j + 1))) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        if (!(container instanceof EtrionicBlastFurnaceBlockEntity entity)) return true;
        if (entity.getEnergyStorage().extract(energy, true) < energy) return false;
        return ItemUtils.canAddItem(entity, result, 5, 6, 7, 8);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        return result.copy();
    }



    @Override
    public @NotNull RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.ALLOYING.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull RecipeType<AlloyingRecipe> getType() {
        return (RecipeType<AlloyingRecipe>) (RecipeType<?>) ModRecipeTypes.ALLOYING.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
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
