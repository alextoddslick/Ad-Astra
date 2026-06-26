package earth.terrarium.adastra.client.utils;

import earth.terrarium.adastra.AdAstra;
import net.minecraft.resources.Identifier;

import java.util.List;

public class DimensionRenderingUtils {

    public static final Identifier BACKLIGHT = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/backlight.png");

    public static final Identifier SUN = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/sun.png");
    public static final Identifier BLUE_SUN = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/blue_sun.png");
    public static final Identifier RED_SUN = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/red_sun.png");

    public static final Identifier EARTH = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/earth.png");
    public static final Identifier MOON = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/moon.png");
    public static final Identifier MARS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/mars.png");
    public static final Identifier VENUS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/venus.png");
    public static final Identifier MERCURY = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/mercury.png");
    public static final Identifier GLACIO = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/glacio.png");

    public static final Identifier PHOBOS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/phobos.png");
    public static final Identifier DEIMOS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/deimos.png");
    public static final Identifier VICINUS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/vicinus.png");

    public static final Identifier ACID_RAIN = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/acid_rain.png");
    public static final Identifier VENUS_CLOUDS = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/environment/venus_clouds.png");

    public static final List<Identifier> SOLAR_SYSTEM_TEXTURES = List.of(
        DimensionRenderingUtils.MERCURY,
        DimensionRenderingUtils.VENUS,
        DimensionRenderingUtils.EARTH,
        DimensionRenderingUtils.MARS
    );
}
