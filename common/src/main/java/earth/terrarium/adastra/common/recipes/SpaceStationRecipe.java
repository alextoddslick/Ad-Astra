package earth.terrarium.adastra.common.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.recipe.CodecRecipe;
import com.teamresourceful.resourcefullib.common.recipe.CodecRecipeSerializer;
import earth.terrarium.adastra.common.recipes.base.IngredientHolder;
import earth.terrarium.adastra.common.registry.ModRecipeSerializers;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record SpaceStationRecipe(
    List<IngredientHolder> ingredients,
    ResourceKey<Level> dimension,
    Identifier structure
) implements CodecRecipe<RecipeInput> {

    private static final RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    public static final MapCodec<SpaceStationRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            IngredientHolder.CODEC.listOf().fieldOf("ingredients").forGetter(SpaceStationRecipe::ingredients),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(SpaceStationRecipe::dimension),
            Identifier.CODEC.fieldOf("structure").forGetter(SpaceStationRecipe::structure)
        ).apply(instance, SpaceStationRecipe::new));

    public static final ByteCodec<SpaceStationRecipe> NETWORK_CODEC = ObjectByteCodec.create(
        IngredientHolder.NETWORK_CODEC.listOf().fieldOf(SpaceStationRecipe::ingredients),
        ExtraByteCodecs.DIMENSION.fieldOf(SpaceStationRecipe::dimension),
        ExtraByteCodecs.IDENTIFIER.fieldOf(SpaceStationRecipe::structure),
        SpaceStationRecipe::new
    );

    @Override
    public boolean matches(@NotNull RecipeInput container, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull CodecRecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.SPACE_STATION_SERIALIZER.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull RecipeType<SpaceStationRecipe> getType() {
        return (RecipeType<SpaceStationRecipe>) (RecipeType<?>) ModRecipeTypes.SPACE_STATION_RECIPE.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return BOOK_CATEGORY;
    }

    @SuppressWarnings("unchecked")
    public static Optional<RecipeHolder<SpaceStationRecipe>> getSpaceStation(Level level, ResourceKey<Level> dimension) {
        var server = level.getServer();
        if (server == null && level.isClientSide()) {
            try {
                server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            } catch (Exception ignored) {}
        }
        if (server == null) return Optional.empty();
        return server.getRecipeManager().getRecipes().stream()
            .filter(holder -> holder.value().getType() == ModRecipeTypes.SPACE_STATION_RECIPE.get())
            .map(holder -> (RecipeHolder<SpaceStationRecipe>) (RecipeHolder<?>) holder)
            .filter(recipe -> recipe.value().dimension().equals(dimension))
            .findFirst();
    }

    public static boolean hasIngredients(Player player, Level level, SpaceStationRecipe recipe) {
        for (IngredientHolder holder : recipe.ingredients()) {
            int count = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (holder.ingredient().test(stack)) {
                    count += stack.getCount();
                }
            }
            if (count < holder.count()) return false;
        }
        return true;
    }

    public static void consumeIngredients(Player player, Level level) {
        getSpaceStation(level, level.dimension()).ifPresent(recipe -> {
            for (IngredientHolder holder : recipe.value().ingredients()) {
                int remaining = holder.count();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    var stack = player.getInventory().getItem(i);
                    if (holder.ingredient().test(stack)) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        remaining -= toRemove;
                        if (remaining <= 0) break;
                    }
                }
            }
        });
    }
}
