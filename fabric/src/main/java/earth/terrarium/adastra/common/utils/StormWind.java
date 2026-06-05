package earth.terrarium.adastra.common.utils;

import net.minecraft.world.phys.Vec3;

/**
 * Shared wind-impulse math for the planet storm system. Used both server-side (pushing
 * non-player mobs in {@code PlanetStormHandler}) and client-side (pushing the local player
 * in {@code AdAstraClient.clientTick}). Deriving the vector purely from the level game time
 * keeps the gust pattern coherent across all entities in a dimension without any extra sync.
 */
public final class StormWind {

    private StormWind() {}

    /** Per-tick base thrust at full intensity while airborne. */
    private static final double BASE_STRENGTH = 0.014;
    /** Multiplier applied while standing on the ground (less shove than mid-air). */
    private static final double GROUNDED_MULTIPLIER = 0.4;

    /**
     * @param gameTime  the level's game time (monotonic tick clock, shared by all entities)
     * @param intensity storm intensity 0..1 (0 outside a storm)
     * @param airborne  whether the entity is off the ground (catches more wind)
     * @return a horizontal impulse to add to the entity's delta movement (zero outside a storm)
     */
    public static Vec3 windImpulse(long gameTime, float intensity, boolean airborne) {
        if (intensity <= 0) return Vec3.ZERO;
        // Wind direction drifts slowly so it isn't a constant one-way shove.
        double baseAngle = (gameTime * 0.0015) % (Math.PI * 2);
        // Gusts: ramp the strength up and down between ~0.45x and 1.0x.
        double gust = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(gameTime * 0.07));
        double strength = BASE_STRENGTH * intensity * gust * (airborne ? 1.0 : GROUNDED_MULTIPLIER);
        return new Vec3(Math.cos(baseAngle) * strength, 0, Math.sin(baseAngle) * strength);
    }
}
