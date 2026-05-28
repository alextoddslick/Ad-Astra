package earth.terrarium.adastra.common.recipes.base;

import com.mojang.serialization.MapCodec;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.StreamCodecByteCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * In 26.1.2, {@code RecipeSerializer} was made a {@code final record} so it can no
 * longer be subclassed. This helper now acts as a factory that returns a vanilla
 * {@link RecipeSerializer} instance, plus carries the {@link RecipeType} for the
 * mod's own bookkeeping (still referenced by the recipe types/serializers registry).
 */
public class AdAstraCodecRecipeSerializer<T extends Recipe<?>> {

    private final RecipeType<T> type;
    private final RecipeSerializer<T> serializer;

    public AdAstraCodecRecipeSerializer(RecipeType<T> type, MapCodec<T> codec, ByteCodec<T> networkCodec) {
        this.type = type;
        this.serializer = new RecipeSerializer<>(codec, StreamCodecByteCodec.toRegistry(networkCodec));
    }

    public RecipeType<T> type() {
        return type;
    }

    public RecipeSerializer<T> serializer() {
        return serializer;
    }
}
