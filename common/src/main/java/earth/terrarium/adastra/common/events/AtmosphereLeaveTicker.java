package earth.terrarium.adastra.common.events;

import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-tick handler that auto-teleports a player from Earth (the overworld) to the
 * Earth orbit / space station dimension when they ascend through y=1000.
 *
 * Mirrors the "left the atmosphere" feel without requiring a rocket. Existing rocket /
 * jet-suit launch flows remain untouched — those go through {@link ModUtils#land} with
 * the standard top-of-space clamp; this path uses {@link ModUtils#landAt} so the
 * caller-specified Y (-100) is honoured.
 */
public class AtmosphereLeaveTicker {

    /** Y at which the auto-teleport triggers when ascending. */
    private static final double TRIGGER_Y = 1000.0;

    /** Y the player materialises at inside the space station. */
    private static final double ARRIVAL_Y = -100.0;

    /** Per-player previous-tick Y, so we only fire on the upward crossing. */
    private static final Map<UUID, Double> previousY = new HashMap<>();

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(server, player);
        }
        // Cheap GC: prune entries for players no longer online.
        if (previousY.size() > server.getPlayerList().getPlayers().size() * 4) {
            previousY.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayer player) {
        UUID id = player.getUUID();
        double currentY = player.getY();
        Double prev = previousY.get(id);
        previousY.put(id, currentY);

        // Only act on the overworld ("Earth") and only while not already in space.
        if (!player.level().dimension().equals(Level.OVERWORLD)) return;
        if (PlanetApi.API.isSpace(player.level())) return; // safety belt

        // Need a previous sample to detect the upward crossing — avoids firing for
        // players who log in already above 1000 from some other path.
        if (prev == null) return;
        if (!(prev < TRIGGER_Y && currentY >= TRIGGER_Y)) return;

        ServerLevel orbit = server.getLevel(Planet.EARTH_ORBIT);
        if (orbit == null) return;

        ModUtils.landAt(player, orbit, new Vec3(player.getX(), ARRIVAL_Y, player.getZ()));
        // After teleport the player's dimension is no longer overworld, so subsequent
        // ticks naturally skip the check until they return.
    }
}
