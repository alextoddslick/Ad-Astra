package earth.terrarium.adastra.common.recipes.machines;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import earth.terrarium.adastra.common.recipes.AdAstraItemStackCodec;
import net.minecraft.world.item.crafting.Recipe;
import earth.terrarium.adastra.common.blockentities.machines.CompressorBlockEntity;
import earth.terrarium.adastra.common.registry.ModRecipeSerializers;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.adastra.common.utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record CompressingRecipe(
    int cookingTime, int energy,
    Ingredient ingredient, ItemStack result
) implements Recipe<RecipeInput> {

    private static final RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    public static final MapCodec<CompressingRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.fieldOf("cookingtime").forGetter(CompressingRecipe::cookingTime),
            Codec.INT.fieldOf("energy").forGetter(CompressingRecipe::energy),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CompressingRecipe::ingredient),
            AdAstraItemStackCodec.CODEC.fieldOf("result").forGetter(CompressingRecipe::result)
        ).apply(instance, CompressingRecipe::new));

    public static final ByteCodec<CompressingRecipe> NETWORK_CODEC = ObjectByteCodec.create(
        ByteCodec.INT.fieldOf(CompressingRecipe::cookingTime),
        ByteCodec.INT.fieldOf(CompressingRecipe::energy),
        ExtraByteCodecs.INGREDIENT.fieldOf(CompressingRecipe::ingredient),
        ExtraByteCodecs.ITEM_STACK.fieldOf(CompressingRecipe::result),
        CompressingRecipe::new
    );

    @Override
    public boolean matches(RecipeInput container, Level level) {
        if (!ingredient.test(container.getItem(1))) return false;
        if (!(container instanceof CompressorBlockEntity entity)) return true;
        if (entity.getEnergyStorage().extract(energy, true) < energy) return false;
        return ItemUtils.canAddItem(container.getItem(2), result);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        return result.copy();
    }



    @Override
    public @NotNull RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.COMPRESSING.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull RecipeType<CompressingRecipe> getType() {
        return (RecipeType<CompressingRecipe>) (RecipeType<?>) ModRecipeTypes.COMPRESSING.get();
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
