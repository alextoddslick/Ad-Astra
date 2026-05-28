package earth.terrarium.adastra.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import earth.terrarium.adastra.common.config.info.AdAstraConfigInfo;

@Config(
    value = "ad_astra-client",
    categories = {
        RadioConfig.class
    }
)
@ConfigInfo.Provider(AdAstraConfigInfo.class)
public final class AdAstraConfigClient {

    @ConfigEntry(
        id = "showOxygenDistributorArea",
        translation = "config.ad_astra.showOxygenDistributorArea"
    )
    public static boolean showOxygenDistributorArea;

    @ConfigEntry(
        id = "showGravityNormalizerArea",
        translation = "config.ad_astra.showGravityNormalizerArea"
    )
    public static boolean showGravityNormalizerArea;

    @ConfigEntry(
        id = "jetSuitEnabled",
        translation = "config.ad_astra.jetSuitEnabled"
    )
    public static boolean jetSuitEnabled = true;

    @ConfigEntry(
        id = "oxygenBarX",
        translation = "config.ad_astra.oxygenBarX"
    )
    public static int oxygenBarX = 5;

    @ConfigEntry(
        id = "oxygenBarY",
        translation = "config.ad_astra.oxygenBarY"
    )
    public static int oxygenBarY = 25;

    @ConfigEntry(
        id = "oxygenBarScale",
        translation = "config.ad_astra.oxygenBarScale"
    )
    public static float oxygenBarScale = 1;

    @ConfigEntry(
        id = "energyBarX",
        translation = "config.ad_astra.energyBarX"
    )
    public static int energyBarX = 11;

    @ConfigEntry(
        id = "energyBarY",
        translation = "config.ad_astra.energyBarY"
    )
    public static int energyBarY = 95;

    @ConfigEntry(
        id = "energyBarScale",
        translation = "config.ad_astra.energyBarScale"
    )
    public static float energyBarScale = 1;

    @ConfigEntry(
        id = "spaceMuffler",
        translation = "config.ad_astra.spaceMuffler"
    )
    @Comment("Reduce volume and increase pitch in space.")
    public static boolean spaceMuffler = true;
}
