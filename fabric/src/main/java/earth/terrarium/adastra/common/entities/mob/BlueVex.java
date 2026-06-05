package earth.terrarium.adastra.common.entities.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Wild "blue vex" — spawns underground in Jupiter's caves and drops Etrium Nuggets (loot table at
 * data/ad_astra/loot_table/entities/blue_vex.json). Reuses vanilla {@link Vex} flight + charge AI;
 * differences from a summoned vex:
 * <ul>
 *   <li>adds a player target selector (no Evoker owner to copy a target from),</li>
 *   <li>anchors its bound origin to its spawn position so the random-move goal has a valid anchor,</li>
 *   <li>never gets a limited life (setLimitedLife is only called by the Evoker summon path).</li>
 * </ul>
 */
public class BlueVex extends Vex {

    public BlueVex(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // No owner to copy a target from, so hunt nearby players directly.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        // Wild vexes have no summoner, so anchor the bound origin to the spawn point.
        this.setBoundOrigin(this.blockPosition());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Spawn only underground (no sky access) and under the usual monster darkness rules. */
    public static boolean checkBlueVexSpawnRules(EntityType<BlueVex> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return !level.canSeeSky(pos) && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
