package earth.terrarium.adastra.common.entities.mob;

import earth.terrarium.adastra.common.entities.mob.lunarians.LunarianMerchantOffers;
import earth.terrarium.adastra.common.registry.ModEntityTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class Lunarian extends Villager {

    public Lunarian(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.MAX_HEALTH, 20)
            .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, CorruptedLunarian.class, 15.0f, 0.5f, 0.5f));
    }

    @Override
    public Villager getBreedOffspring(ServerLevel serverWorld, AgeableMob passiveEntity) {
        Lunarian entity = new Lunarian(ModEntityTypes.LUNARIAN.get(), serverWorld);
        entity.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.BREEDING, null);
        return entity;
    }

    // Custom trade offers
    @Override
    protected void updateTrades(ServerLevel serverLevel) {
        // TODO 26.1.2: re-implement custom Lunarian trades against the new
        // VillagerTrade model. Stubbed to no-op for the build to compile.
    }
}
