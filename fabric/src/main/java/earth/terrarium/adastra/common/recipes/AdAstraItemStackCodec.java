package earth.terrarium.adastra.common.recipes;

import com.mojang.serialization.Codec;
import com.teamresourceful.resourcefullib.common.codecs.recipes.ItemStackCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Alias for the Resourceful Lib item stack codec used by Ad Astra recipes.
 *
 * <p>The actual fix for the 26.1
 * "Item ad_astra:foo does not have components yet" recipe-parse error lives in
 * {@code earth.terrarium.adastra.mixins.common.ReloadableServerResourcesMixin}.
 * That mixin forces {@code DataComponentInitializers.PendingComponents#apply()}
 * to run inside the {@link net.minecraft.server.ReloadableServerResources}
 * constructor — i.e. before recipes are parsed — so by the time
 * {@code Item.CODEC_WITH_BOUND_COMPONENTS} runs its
 * {@code Holder.areComponentsBound()} validation, every item (vanilla and
 * Ad Astra) is bound.</p>
 *
 * <p>This indirection class is kept so future divergence from the Resourceful
 * Lib codec only requires touching one file.</p>
 */
public final class AdAstraItemStackCodec {

    private AdAstraItemStackCodec() {}

    public static final Codec<ItemStack> CODEC = ItemStackCodec.CODEC;
}
