package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla clouds on airless Ad Astra planets/moons. Vanilla still draws its cloud deck on
 * custom dimensions whose dimension-type effects allow it, which looks wrong on the Moon, Mars,
 * Mercury, Glacio, Jupiter and Europa (and on Europa the clouds sit between the player and the giant
 * Jupiter). We cancel cloud rendering whenever the current dimension is an Ad Astra planet whose
 * renderer has {@code custom_clouds: false}; atmosphere worlds (e.g. Venus, custom_clouds true) keep
 * their clouds.
 */
@Mixin(CloudRenderer.class)
public class CloudRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void adastra$suppressCloudsOnAirlessPlanets(int color, CloudStatus cloudStatus, float height,
                                                        int colorModifier, Vec3 cameraPos, long ticks,
                                                        float partialTick, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        ModDimensionSpecialEffects fx = ClientPlatformUtils.getPlanetRenderers().get(level.dimension());
        if (fx != null && !fx.renderer().customClouds()) {
            ci.cancel();
        }
    }
}
