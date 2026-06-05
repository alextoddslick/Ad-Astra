package earth.terrarium.adastra.client.utils;

import earth.terrarium.adastra.common.registry.ModParticleTypes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Client-side storm gas: dense, multi-coloured gas streaming past the player on the wind during a
 * planet storm. Spawned every client tick from {@code AdAstraClient.clientTick}, scaled by
 * {@link ClientStormData#intensity()}. Uses the custom {@code STORM_GAS} particle (no friction, no
 * collision) so the gas actually streaks across the view in the wind direction instead of floating
 * in place; colour + fade are handled inside the particle. The density also obscures the view.
 */
public final class StormParticles {

    private StormParticles() {}

    public static void tick(LocalPlayer player) {
        float intensity = ClientStormData.intensity();
        if (intensity <= 0) return;
        // Gas only blows at the surface — none deep underground.
        float exposure = ClientStormData.skyExposure();
        if (exposure <= 0.05f) return;

        Level level = player.level();
        RandomSource rnd = player.getRandom();
        long time = level.getGameTime();

        // Wind direction matches StormWind's slowly drifting base angle.
        double angle = (time * 0.0015) % (Math.PI * 2);
        double wx = Math.cos(angle);
        double wz = Math.sin(angle);

        int count = (int) ((8 + intensity * 26) * exposure);
        if (count <= 0) return;
        double px = player.getX();
        double py = player.getEyeY();
        double pz = player.getZ();

        for (int i = 0; i < count; i++) {
            // Bias the spawn up-wind so the gas streams toward and past the player.
            double ox = (rnd.nextDouble() - 0.5) * 30.0 - wx * 12.0;
            double oy = (rnd.nextDouble() - 0.5) * 14.0;
            double oz = (rnd.nextDouble() - 0.5) * 30.0 - wz * 12.0;
            // No friction on the particle, so this velocity carries for its whole life -> fast streaks.
            double speed = (0.9 + rnd.nextDouble() * 1.4) * (0.6f + intensity);

            level.addParticle(ModParticleTypes.STORM_GAS.get(), true, false,
                px + ox, py + oy, pz + oz,
                wx * speed, (rnd.nextDouble() - 0.5) * 0.12, wz * speed);
        }
    }
}
