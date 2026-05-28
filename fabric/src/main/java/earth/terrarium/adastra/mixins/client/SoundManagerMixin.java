package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.utils.ClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void adastra$play(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (adastra$play(sound, 0)) {
            // Re-routed through soundEngine.play() with muffled volume; from the caller's POV
            // the sound did start. Returning null here crashes vanilla's MusicManager.startPlaying
            // in 1.21.11 because it calls .ordinal() on this without a null check.
            cir.setReturnValue(SoundEngine.PlayResult.STARTED);
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void adastra$playDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        if (adastra$play(sound, delay)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean adastra$play(SoundInstance sound, int delay) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        if (!AdAstraConfigClient.spaceMuffler) return false;
        SoundSource source = sound.getSource();
        if (source == SoundSource.MASTER) return false;
        // Don't re-route MUSIC or RECORDS through soundEngine.play() directly. Doing so
        // bypassed SoundManager tracking, so vanilla MusicManager thought no music was
        // playing and immediately queued another track — producing simultaneous duplicate
        // music tracks in space dimensions. Let MC handle music/records normally; the
        // muffler is for ambient/block/entity sounds.
        if (source == SoundSource.MUSIC || source == SoundSource.RECORDS) return false;
        if (!PlanetApi.API.isSpace(level)) return false;
        if (sound instanceof TickableSoundInstance) return false;
        if (ClientData.getLocalData() != null && ClientData.getLocalData().oxygen()) {
            return false;
        }

        Minecraft.getInstance().execute(() -> {
            SoundInstance newSound = new SimpleSoundInstance(sound.getIdentifier(), source,
                0.1f, 0.1f,
                RandomSource.create(), sound.isLooping(), delay,
                sound.getAttenuation(), sound.getX(),
                sound.getY(), sound.getZ(), sound.isRelative()
            );

            if (delay == 0) {
                soundEngine.play(newSound);
            } else {
                soundEngine.playDelayed(newSound, delay);
            }
        });
        return true;
    }
}
