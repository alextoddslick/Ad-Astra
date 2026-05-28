package earth.terrarium.adastra.common.recipes.machines;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import net.minecraft.world.item.crafting.Recipe;
import earth.terrarium.adastra.common.registry.ModRecipeSerializers;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.common_storage_lib.resources.ResourceStack;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.resources.fluid.ingredient.SizedFluidIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record RefiningRecipe(
    int cookingTime, int energy,
    SizedFluidIngredient input,
    FluidResource result,
    long resultAmount
) implements Recipe<RecipeInput> {

    private static final RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    public static final MapCodec<RefiningRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.fieldOf("cookingtime").forGetter(RefiningRecipe::cookingTime),
            Codec.INT.fieldOf("energy").forGetter(RefiningRecipe::energy),
            SizedFluidIngredient.NESTED_MB_CODEC.codec().fieldOf("input").forGetter(RefiningRecipe::input),
            ResourceStack.FLUID_MB_CODEC.fieldOf("result").forGetter(r -> new ResourceStack<>(r.result(), r.resultAmount()))
        ).apply(instance, (cookingTime, energy, input, resultStack) ->
            new RefiningRecipe(cookingTime, energy, input, resultStack.resource(), resultStack.amount())));

    private static final ByteCodec<FluidResource> FLUID_RESOURCE_BYTE_CODEC = ByteCodec.STRING.map(
        str -> FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(str))),
        res -> BuiltInRegistries.FLUID.getKey(res.getType()).toString()
    );

    private static final ByteCodec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT_BYTE_CODEC = ObjectByteCodec.create(
        ByteCodec.STRING.fieldOf(i -> BuiltInRegistries.FLUID.getKey(i.getFluids().getFirst().resource().getType()).toString()),
        ByteCodec.LONG.fieldOf(SizedFluidIngredient::getAmount),
        (fluidStr, amount) -> SizedFluidIngredient.of(FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidStr))), (int) (long) amount)
    );

    public static final ByteCodec<RefiningRecipe> NETWORK_CODEC = ObjectByteCodec.create(
        ByteCodec.INT.fieldOf(RefiningRecipe::cookingTime),
        ByteCodec.INT.fieldOf(RefiningRecipe::energy),
        SIZED_FLUID_INGREDIENT_BYTE_CODEC.fieldOf(RefiningRecipe::input),
        FLUID_RESOURCE_BYTE_CODEC.fieldOf(RefiningRecipe::result),
        ByteCodec.LONG.fieldOf(RefiningRecipe::resultAmount),
        RefiningRecipe::new
    );

    @Override
    public boolean matches(@NotNull RecipeInput container, @NotNull Level level) {
        // Recipe matching is handled by the block entity checking fluid contents
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        return ItemStack.EMPTY;
    }



    @Override
    public @NotNull RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.REFINING.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull RecipeType<RefiningRecipe> getType() {
        return (RecipeType<RefiningRecipe>) (RecipeType<?>) ModRecipeTypes.REFINING.get();
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
