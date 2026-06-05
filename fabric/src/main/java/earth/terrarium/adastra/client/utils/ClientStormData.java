package earth.terrarium.adastra.client.utils;

import earth.terrarium.adastra.common.network.packets.ClientboundSyncStormPacket;
import earth.terrarium.adastra.common.utils.UpgradeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side cache of the local dimension's storm state, fed by {@link ClientboundSyncStormPacket}.
 * All getters are gated by {@link #appliesHere()} so stale data from a dimension the player has
 * since left never drives wind or the HUD.
 */
public class ClientStormData {

    @Nullable
    private static ResourceKey<Level> dimension;
    private static boolean storming;
    private static float intensity;
    private static byte phase = ClientboundSyncStormPacket.PHASE_NONE;

    public static void update(ResourceKey<Level> dim, boolean stormingNow, float intensityNow, byte phaseNow) {
        dimension = dim;
        storming = stormingNow;
        intensity = intensityNow;
        phase = phaseNow;
    }

    /** True only when the cached state belongs to the dimension the player is currently in. */
    private static boolean appliesHere() {
        Level level = Minecraft.getInstance().level;
        return level != null && dimension != null && dimension.equals(level.dimension());
    }

    public static boolean isStorming() {
        return appliesHere() && storming;
    }

    public static float intensity() {
        return appliesHere() ? intensity : 0f;
    }

    public static byte phase() {
        return appliesHere() ? phase : ClientboundSyncStormPacket.PHASE_NONE;
    }

    /**
     * How exposed to the open sky the local player is: 1.0 under open sky, fading to 0 the deeper
     * underground they are. Used to fade storm wind/particles/sound so the storm only affects the
     * surface, not players sheltering underground.
     */
    public static float skyExposure() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 1.0f;
        int skyLight = mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition());
        return Mth.clamp(skyLight / 13.0f, 0.0f, 1.0f);
    }

    /** True if the local player wears an Analysis Visor helmet (NASA Workbench upgrade). */
    public static boolean localHasAnalysisVisor() {
        var player = Minecraft.getInstance().player;
        return player != null && UpgradeUtils.hasAnalysisVisor(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    /**
     * Storm intensity for visual obstruction (fog + world darkening). The Analysis Visor's optics
     * see through the gas, so a visor wearer experiences only a fraction of the fog/darkness.
     * Wind and gas particles still use the raw {@link #intensity()} (the gas is still there).
     */
    public static float effectiveStormIntensity() {
        float i = intensity();
        return localHasAnalysisVisor() ? i * 0.3f : i;
    }
}
