package earth.terrarium.adastra.common.entities.mob;

import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
// TODO: Migrate to CSL
// import earth.terrarium.botarium.common.fluid.base.FluidContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class SulfurCreeper extends Creeper {

    public SulfurCreeper(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    public void explodeCreeper() {
        if (this.level().isClientSide()) return;
        float power = isPowered() ? 2 : 1;
        this.dead = true;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3 * power, Level.ExplosionInteraction.MOB);
        this.discard();

        // TODO: Migrate to CSL - re-implement oxygen drain on explosion
        // Explosion no longer returns a value or tracks hit players directly.
        // Nearby players need to be queried manually if oxygen drain is re-enabled.
        if (this.level() instanceof ServerLevel serverLevel) {
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(7))) {
                var stack = player.getItemBySlot(EquipmentSlot.CHEST);
                if (SpaceSuitItem.hasFullSet(player)) {
                    if (!(stack.getItem() instanceof SpaceSuitItem suit)) continue;
                    long amount = Math.max(0, (long) ((7 - player.distanceTo(this)) * 125));
                    suit.consumeOxygen(stack, amount);
                    player.setItemSlot(EquipmentSlot.CHEST, stack);
                }
            }
        }

        Collection<MobEffectInstance> effects = this.getActiveEffects();
        if (!effects.isEmpty()) {
            AreaEffectCloud areaEffectCloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            areaEffectCloud.setRadius(2.5F);
            areaEffectCloud.setRadiusOnUse(-0.5F);
            areaEffectCloud.setWaitTime(10);
            areaEffectCloud.setDuration(areaEffectCloud.getDuration() / 2);
            areaEffectCloud.setRadiusPerTick(-areaEffectCloud.getRadius() / (float) areaEffectCloud.getDuration());

            for (MobEffectInstance mobEffectInstance : effects) {
                areaEffectCloud.addEffect(new MobEffectInstance(mobEffectInstance));
            }

            this.level().addFreshEntity(areaEffectCloud);
        }
    }
}
