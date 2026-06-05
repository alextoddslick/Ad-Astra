package earth.terrarium.adastra.mixins.common;

import earth.terrarium.adastra.common.handlers.PlanetStormHandler;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redirects {@code /weather} on storm dimensions (e.g. Jupiter) into the custom
 * {@link PlanetStormHandler} state instead of vanilla weather. Vanilla weather is effectively
 * global (non-overworld levels delegate to the overworld), so without this {@code /weather} run
 * on Jupiter would silently change the overworld's weather and do nothing to the storm.
 *
 * <p>Mapping: {@code clear} → calm, {@code rain} → storm (moderate), {@code thunder} → storm
 * (severe), each held for the command's duration before the auto-scheduler resumes. When the
 * command source is not on a storm dimension the injection no-ops and vanilla handles it.
 */
@Mixin(WeatherCommand.class)
public class WeatherCommandMixin {

    @Inject(method = "setClear", at = @At("HEAD"), cancellable = true)
    private static void adastra$setClear(CommandSourceStack source, int duration, CallbackInfoReturnable<Integer> cir) {
        adastra$redirect(source, duration, false, 0f, "commands.weather.set.clear", cir);
    }

    @Inject(method = "setRain", at = @At("HEAD"), cancellable = true)
    private static void adastra$setRain(CommandSourceStack source, int duration, CallbackInfoReturnable<Integer> cir) {
        adastra$redirect(source, duration, true, 0.6f, "commands.weather.set.rain", cir);
    }

    @Inject(method = "setThunder", at = @At("HEAD"), cancellable = true)
    private static void adastra$setThunder(CommandSourceStack source, int duration, CallbackInfoReturnable<Integer> cir) {
        adastra$redirect(source, duration, true, 1.0f, "commands.weather.set.thunder", cir);
    }

    // Vanilla passes -1 for "/weather clear|rain|thunder" with NO duration argument (it resolves the
    // real default inside the method we cancel). Without handling that, Math.max(1, -1) collapsed the
    // manual window to a single tick, so the scheduler re-stormed on the very next tick — i.e.
    // "/weather clear just changed the storm duration". Fall back to a full MC day instead.
    private static final int DEFAULT_MANUAL_TICKS = 24000; // 1 MC day

    private static void adastra$redirect(CommandSourceStack source, int duration, boolean storming, float intensity, String messageKey, CallbackInfoReturnable<Integer> cir) {
        ServerLevel level = source.getLevel();
        if (!AdAstraData.hasStorms(level.dimension())) return;
        int ticks = duration > 0 ? duration : DEFAULT_MANUAL_TICKS;
        PlanetStormHandler.setManual(level, storming, intensity, ticks);
        source.sendSuccess(() -> Component.translatable(messageKey), true);
        cir.setReturnValue(1); // vanilla setClear/setRain/setThunder return 1
    }
}
