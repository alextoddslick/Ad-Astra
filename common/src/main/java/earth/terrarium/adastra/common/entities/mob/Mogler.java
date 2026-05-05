package earth.terrarium.adastra.common.entities.mob;

import earth.terrarium.adastra.common.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class Mogler extends Hoglin {

    public Mogler(EntityType<? extends Hoglin> entityType, Level level) {
        super(entityType, level);
        this.setImmuneToZombification(true);
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4f)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6f)
            .add(Attributes.ATTACK_KNOCKBACK, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Vanilla Hoglin.readAdditionalSaveData reads "IsImmuneToZombification" with default false,
        // which clobbers the constructor-set true on entity load. Re-assert immunity here.
        this.setImmuneToZombification(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // Belt-and-braces: ensure the synced flag stays true so vanilla
        // Hoglin.customServerAiStep never increments TimeInOverworld for Moglers.
        this.setImmuneToZombification(true);
        super.customServerAiStep(level);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob entity) {
        Mogler moglerEntity = ModEntityTypes.MOGLER.get().create(level, EntitySpawnReason.BREEDING);
        if (moglerEntity != null) {
            moglerEntity.setPersistenceRequired();
        }
        return moglerEntity;
    }

    public void finishConversion() {
        this.convertTo(ModEntityTypes.ZOMBIFIED_MOGLER.get(), ConversionParams.single(this, true, true), zombifiedMoglerEntity -> {
            zombifiedMoglerEntity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        });
    }
}
