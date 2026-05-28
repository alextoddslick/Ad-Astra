package earth.terrarium.adastra.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Radio")
public final class RadioConfig {

    @ConfigEntry(
        id = "volume",
        translation = "config.ad_astra.volume"
    )
    @ConfigOption.Range(min = 0, max = 100)
    public static int volume = 50;

    @ConfigEntry(
        id = "favorites"
    )
    @ConfigOption.Hidden
    public static String[] favorites = new String[0];
}
