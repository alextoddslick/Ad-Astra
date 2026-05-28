package earth.terrarium.adastra.common.entities.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.Level;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class LunarianWanderingTrader extends WanderingTrader {

    public LunarianWanderingTrader(EntityType<? extends WanderingTrader> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void updateTrades(ServerLevel serverLevel) {
        // TODO 26.1.2: VillagerTrades.ItemListing was removed; trade tables stubbed.
        // Re-implement against the new net.minecraft.world.item.trading.VillagerTrade model.
    }
}
