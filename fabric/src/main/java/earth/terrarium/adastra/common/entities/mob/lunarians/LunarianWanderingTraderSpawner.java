package earth.terrarium.adastra.common.entities.mob.lunarians;

import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;

// LEGACY ENTITY. WILL BE REPLACED IN THE FUTURE.
public class LunarianWanderingTraderSpawner implements CustomSpawner {

    public static final int DEFAULT_SPAWN_DELAY = 24000;
    private static final int DEFAULT_SPAWN_TIMER = 1200;
    private final Random random = new Random();
    private final ServerLevelData properties;
    private int spawnTimer;
    private int spawnDelay;
    private int spawnChance;

    public LunarianWanderingTraderSpawner(ServerLevelData properties) {
        this.properties = properties;
        this.spawnTimer = DEFAULT_SPAWN_TIMER;
        // TODO 26.1.2: ServerLevelData no longer carries wandering-trader spawn state;
        // track it locally for now.
        this.spawnDelay = DEFAULT_SPAWN_DELAY;
        this.spawnChance = 25;
    }

    @Override
    public void tick(ServerLevel level, boolean spawnMonsters) {
        if (!(Boolean) level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) return;
        if (--this.spawnTimer > 0) return;

        this.spawnTimer = DEFAULT_SPAWN_TIMER;
        this.spawnDelay -= DEFAULT_SPAWN_TIMER;
        // TODO 26.1.2: persistent wandering-trader counters moved off ServerLevelData.
        if (this.spawnDelay > 0) return;
        this.spawnDelay = DEFAULT_SPAWN_DELAY;
        if (!(Boolean) level.getGameRules().get(GameRules.SPAWN_MOBS)) return;
        int i = this.spawnChance;
        this.spawnChance = Mth.clamp(this.spawnChance + 25, 25, 75);
        if (this.random.nextInt(100) > i) return;
        if (this.trySpawn(level)) {
            this.spawnChance = 25;
        }
    }

    private boolean trySpawn(ServerLevel level) {
        ServerPlayer playerEntity = level.getRandomPlayer();
        if (playerEntity == null) return true;
        if (this.random.nextInt(10) != 0) return false;
        if (!PlanetApi.API.isPlanet(level.dimension())) return false;
        if (Level.OVERWORLD.equals(level.dimension())) return false;

        BlockPos blockPos = playerEntity.blockPosition();
        PoiManager pointOfInterestStorage = level.getPoiManager();
        Optional<BlockPos> optional = pointOfInterestStorage.find(holder -> holder.is(PoiTypes.MEETING), pos -> true, blockPos, 48, PoiManager.Occupancy.ANY);
        BlockPos blockPos2 = optional.orElse(blockPos);
        BlockPos blockPos3 = this.getNearbySpawnPos(level, blockPos2, 48);
        if (blockPos3 != null && this.doesNotSuffocateAt(level, blockPos3)) {
            if (level.getBiome(blockPos3).is((BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS))) {
                return false;
            }

            WanderingTrader wanderingTraderEntity = ModEntityTypes.LUNARIAN_WANDERING_TRADER.get().spawn(level, blockPos3, EntitySpawnReason.EVENT);
            if (wanderingTraderEntity != null) {

                // TODO 26.1.2: setWanderingTraderId removed from ServerLevelData.
                wanderingTraderEntity.setDespawnDelay(48000);
                wanderingTraderEntity.setWanderTarget(blockPos2);
                // restrictTo was removed in 1.21.11 - wandering trader home area restriction no longer available
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos getNearbySpawnPos(LevelReader level, BlockPos pos, int range) {
        BlockPos blockPos = null;
        for (int i = 0; i < 10; ++i) {
            int k;
            int j = pos.getX() + this.random.nextInt(range * 2) - range;
            BlockPos blockPos2 = BlockPos.containing(j, level.getHeight(Heightmap.Types.WORLD_SURFACE, j, k = pos.getZ() + this.random.nextInt(range * 2) - range), k);
            if (!SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, blockPos2, ModEntityTypes.LUNARIAN_WANDERING_TRADER.get()))
                continue;
            blockPos = blockPos2;
            break;
        }
        return blockPos;
    }

    private boolean doesNotSuffocateAt(BlockGetter level, BlockPos pos) {
        for (BlockPos blockPos : BlockPos.betweenClosed(pos, pos.offset(1, 2, 1))) {
            if (level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty())
                continue;
            return false;
        }
        return true;
    }
}