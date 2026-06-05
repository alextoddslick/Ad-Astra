package earth.terrarium.adastra.common.handlers;

import com.mojang.serialization.Codec;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ClientboundSyncStormPacket;
import earth.terrarium.adastra.common.planets.AdAstraData;
import earth.terrarium.adastra.common.utils.StormWind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Per-dimension storm scheduler + state for planets that opt into the storm system via the
 * {@code has_storms} planet field (e.g. Jupiter). One instance is persisted per storm
 * {@link ServerLevel} via {@code getDataStorage()}.
 *
 * <p>Design notes:
 * <ul>
 *   <li><b>Custom, not vanilla weather.</b> MC weather is effectively global (non-overworld
 *       levels delegate rain/thunder to the overworld), so storms run on this independent
 *       per-dimension state instead. {@code /weather} is redirected into it by
 *       {@code WeatherCommandMixin} on storm dimensions.</li>
 *   <li><b>Scheduler.</b> Jupiter is almost always storming: storms last 1–5 MC days, with a
 *       rare calm window of 0.5–1.5 days (~{@value #STORM_TO_STORM_CHANCE} chance a storm is
 *       followed by another storm). The next state is decided when an episode begins so the
 *       warning window can announce what's coming.</li>
 *   <li><b>Sync.</b> Broadcasts {@link ClientboundSyncStormPacket} to players in the dimension
 *       on state change and on a periodic heartbeat (covers fresh joins / dimension changes).</li>
 * </ul>
 */
public class PlanetStormHandler extends SavedData {

    public static final int DAY_TICKS = 24000;
    /** How long before a transition the "approaching / clearing" warning is shown. */
    public static final int WARN_TICKS = 600; // 30s
    /** Probability that a finished storm is immediately followed by another storm. */
    private static final float STORM_TO_STORM_CHANCE = 0.85f;
    /** Re-broadcast the storm state to dimension players this often, for late joiners. */
    private static final int HEARTBEAT_TICKS = 40; // 2s
    /** Apply server-side wind to mobs every N ticks (players are pushed client-side). */
    private static final int MOB_WIND_INTERVAL = 8;

    // Persisted state.
    private boolean storming;
    private float intensity;
    private int ticksUntilTransition;
    private boolean nextStorming;

    // Transient bookkeeping (not persisted).
    private boolean initialized;
    private boolean hasBroadcast;
    private boolean lastStorming;
    private float lastIntensity;
    private byte lastPhase;

    public PlanetStormHandler() {}

    // ---- persistence ----

    private void loadData(CompoundTag tag) {
        this.storming = tag.getBoolean("storming").orElse(true);
        this.intensity = tag.getFloat("intensity").orElse(0.7f);
        this.ticksUntilTransition = tag.getInt("ticks_until_transition").orElse(DAY_TICKS);
        this.nextStorming = tag.getBoolean("next_storming").orElse(true);
        this.initialized = true;
    }

    private void saveData(CompoundTag tag) {
        tag.putBoolean("storming", storming);
        tag.putFloat("intensity", intensity);
        tag.putInt("ticks_until_transition", ticksUntilTransition);
        tag.putBoolean("next_storming", nextStorming);
    }

    private static final Codec<PlanetStormHandler> CODEC = CompoundTag.CODEC.xmap(
        tag -> {
            PlanetStormHandler handler = new PlanetStormHandler();
            handler.loadData(tag);
            return handler;
        },
        handler -> {
            CompoundTag tag = new CompoundTag();
            handler.saveData(tag);
            return tag;
        }
    );

    private static final SavedDataType<PlanetStormHandler> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planet_storms"),
        PlanetStormHandler::new,
        CODEC,
        null
    );

    public static PlanetStormHandler read(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---- ticking ----

    public static void onServerTick(MinecraftServer server) {
        List<Planet> stormPlanets = AdAstraData.stormPlanets();
        if (stormPlanets.isEmpty()) return;
        for (Planet planet : stormPlanets) {
            ServerLevel level = server.getLevel(planet.dimension());
            if (level == null) continue;
            read(level).tick(level);
        }
    }

    private void tick(ServerLevel level) {
        RandomSource random = level.getRandom();

        // Advance the scheduler (always, even with no players, so storms progress naturally).
        if (!initialized) {
            beginEpisode(true, random); // Jupiter starts stormy.
        } else {
            if (ticksUntilTransition > 0) ticksUntilTransition--;
            if (ticksUntilTransition <= 0) beginEpisode(nextStorming, random);
        }

        // Sync + entity effects only matter when players are present.
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        long gameTime = level.getGameTime();
        byte phase = computePhase();
        boolean changed = !hasBroadcast || storming != lastStorming || intensity != lastIntensity || phase != lastPhase;
        if (changed || gameTime % HEARTBEAT_TICKS == 0) {
            ClientboundSyncStormPacket packet = new ClientboundSyncStormPacket(level.dimension(), storming, intensity, phase);
            for (ServerPlayer player : players) {
                NetworkHandler.CHANNEL.sendToPlayer(packet, player);
            }
            hasBroadcast = true;
            lastStorming = storming;
            lastIntensity = intensity;
            lastPhase = phase;
        }

        // Server-side wind for non-player mobs (the local player is pushed client-side).
        if (storming && intensity > 0 && gameTime % MOB_WIND_INTERVAL == 0) {
            applyMobWind(level, gameTime);
        }
    }

    private void applyMobWind(ServerLevel level, long gameTime) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity instanceof Player) continue; // handled client-side
            if (living.isNoGravity()) continue;
            Vec3 wind = StormWind.windImpulse(gameTime, intensity, !living.onGround());
            if (wind.lengthSqr() == 0) continue;
            living.setDeltaMovement(living.getDeltaMovement().add(wind));
        }
    }

    private void beginEpisode(boolean storm, RandomSource random) {
        this.storming = storm;
        this.initialized = true;
        if (storm) {
            this.intensity = 0.4f + random.nextFloat() * 0.6f;       // 0.4 .. 1.0
            this.ticksUntilTransition = DAY_TICKS + random.nextInt(4 * DAY_TICKS); // 1 .. 5 days
            this.nextStorming = random.nextFloat() < STORM_TO_STORM_CHANCE;        // mostly stays stormy
        } else {
            this.intensity = 0f;
            this.ticksUntilTransition = DAY_TICKS / 2 + random.nextInt(DAY_TICKS);  // 0.5 .. 1.5 days
            this.nextStorming = true; // a calm window always rolls back into a storm
        }
        setDirty();
    }

    private byte computePhase() {
        if (ticksUntilTransition > WARN_TICKS) return ClientboundSyncStormPacket.PHASE_NONE;
        if (!storming && nextStorming) return ClientboundSyncStormPacket.PHASE_APPROACHING;
        if (storming && !nextStorming) return ClientboundSyncStormPacket.PHASE_CLEARING;
        return ClientboundSyncStormPacket.PHASE_NONE;
    }

    // ---- /weather control ----

    /**
     * Override the storm state for a manual window (driven by {@code /weather} on storm
     * dimensions). After the window elapses the auto-scheduler resumes with a storm
     * (so {@code /weather clear 600} gives a calm window, then it storms again).
     */
    /**
     * Immediately sends the current storm state of the player's dimension to that player, if it
     * is a storm dimension. Called on join/datapack-sync so a player doesn't wait for the next
     * heartbeat (up to {@value #HEARTBEAT_TICKS} ticks) before wind + HUD activate. Mid-session
     * dimension changes are still covered by the heartbeat.
     */
    public static void syncToPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!AdAstraData.hasStorms(level.dimension())) return;
        PlanetStormHandler handler = read(level);
        NetworkHandler.CHANNEL.sendToPlayer(
            new ClientboundSyncStormPacket(level.dimension(), handler.storming, handler.intensity, handler.computePhase()),
            player);
    }

    public static void setManual(ServerLevel level, boolean storming, float intensity, int durationTicks) {
        PlanetStormHandler handler = read(level);
        handler.storming = storming;
        handler.intensity = storming ? intensity : 0f;
        handler.ticksUntilTransition = Math.max(1, durationTicks);
        handler.nextStorming = true;
        handler.initialized = true;
        handler.hasBroadcast = false; // force an immediate re-broadcast next tick
        handler.setDirty();
    }
}
