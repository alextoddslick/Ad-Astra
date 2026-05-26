package earth.terrarium.adastra.common.entities.mob;

import earth.terrarium.adastra.common.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class Pygro extends Piglin {

    public Pygro(EntityType<? extends AbstractPiglin> entityType, Level level) {
        super(entityType, level);
        this.setImmuneToZombification(true);
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.45)
            .add(Attributes.MAX_HEALTH, 24)
            .add(Attributes.ATTACK_DAMAGE, 6);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Vanilla AbstractPiglin.readAdditionalSaveData reads "IsImmuneToZombification" with default false,
        // which clobbers the constructor-set true on entity load. Re-assert immunity here.
        this.setImmuneToZombification(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // Belt-and-braces: ensure the synced flag stays true so vanilla
        // AbstractPiglin.customServerAiStep never increments TimeInOverworld for Pygros.
        this.setImmuneToZombification(true);
        super.customServerAiStep(level);
    }

    @Override
    protected void finishConversion(ServerLevel level) {
        this.convertTo(ModEntityTypes.ZOMBIFIED_PYGRO.get(), ConversionParams.single(this, true, true), zombifiedPygroEntity -> {
            zombifiedPygroEntity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        });
    }
}
