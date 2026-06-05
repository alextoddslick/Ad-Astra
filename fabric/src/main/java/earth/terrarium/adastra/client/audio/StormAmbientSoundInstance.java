package earth.terrarium.adastra.client.audio;

import earth.terrarium.adastra.client.utils.ClientStormData;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Looping storm-wind ambience that follows the player while a storm is active. Listener-relative
 * (no attenuation), so it sits evenly around the player; its volume tracks
 * {@link ClientStormData#intensity()} and it self-stops the moment the storm ends or the player
 * leaves the storm dimension. Started/managed from {@code AdAstraClient.clientTick}.
 */
public class StormAmbientSoundInstance extends AbstractTickableSoundInstance {

    public StormAmbientSoundInstance(SoundEvent soundEvent, RandomSource random) {
        super(soundEvent, SoundSource.AMBIENT, random);
        this.looping = true;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.volume = volumeFor(ClientStormData.intensity());
    }

    private int ticks = 0;

    private static float volumeFor(float intensity) {
        return Mth.clamp(0.3f + 0.7f * intensity, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        if (!ClientStormData.isStorming()) {
            stop();
            return;
        }
        ticks++;
        // Slow gusting swell on top of the loop's own dynamics, plus a gentle pitch waver, so a
        // single looping sample doesn't read as obviously repetitive.
        float gust = 0.85f + 0.15f * (float) Math.sin(ticks * 0.05);
        // The storm rages OUTSIDE — muffle it by the player's exposure to the sky so it sounds
        // distant a little underground and fades to silence when deep below the surface.
        this.volume = volumeFor(ClientStormData.intensity()) * gust * ClientStormData.skyExposure();
        this.pitch = 0.9f + 0.12f * (float) Math.sin(ticks * 0.017 + 1.3);
    }
}
