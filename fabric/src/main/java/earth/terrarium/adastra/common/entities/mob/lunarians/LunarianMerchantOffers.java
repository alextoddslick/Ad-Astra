package earth.terrarium.adastra.common.entities.mob.lunarians;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Map;

/**
 * LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
 *
 * TODO 26.1.2: VillagerTrades.ItemListing and the legacy trade-factory system were
 * heavily refactored in MC 26.1. The entire trade table has been stubbed out to
 * empty maps so the mod builds. Restoring custom Lunarian trades will require
 * porting to the new {@code net.minecraft.world.item.trading.VillagerTrade} model.
 */
public class LunarianMerchantOffers {

    /** Local stub for the old {@code VillagerTrades.ItemListing} SAM interface. */
    public interface ItemListing {
        // Intentionally empty - just a marker so legacy entity code can still reference the type.
    }

    public static final Map<ResourceKey<VillagerProfession>, Int2ObjectMap<ItemListing[]>> PROFESSION_TO_LEVELED_TRADE = Map.of();

    public static final Int2ObjectMap<ItemListing[]> WANDERING_TRADER_TRADES = new Int2ObjectOpenHashMap<>();
}
