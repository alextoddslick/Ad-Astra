package earth.terrarium.adastra.common.planets;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.planets.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AdAstraData extends SimpleJsonResourceReloadListener<Planet> {

    private static final Map<ResourceKey<Level>, Planet> PLANETS = new HashMap<>();
    private static final Map<ResourceKey<Level>, ResourceKey<Level>> DIMENSIONS_TO_PLANETS = new HashMap<>();

    public AdAstraData() {
        super(Planet.CODEC, FileToIdConverter.json("planets"));
    }

    @Override
    protected void apply(Map<Identifier, Planet> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        PLANETS.clear();
        DIMENSIONS_TO_PLANETS.clear();
        object.forEach((key, planet) -> {
            PLANETS.put(planet.dimension(), planet);
            DIMENSIONS_TO_PLANETS.put(planet.dimension(), planet.dimension());
            for (ResourceKey<Level> dimension : planet.additionalLaunchDimensions()) {
                DIMENSIONS_TO_PLANETS.put(dimension, planet.dimension());
            }
        });
    }

    public static void encodePlanets(FriendlyByteBuf buf) {
        List<Planet> planetList = planets().values().stream().toList();
        DataResult<JsonElement> result = Planet.CODEC.listOf().encodeStart(JsonOps.INSTANCE, planetList);
        result.result().ifPresentOrElse(
            json -> buf.writeUtf(json.toString()),
            () -> {
                result.error().ifPresent(e -> AdAstra.LOGGER.error(e.message()));
                buf.writeUtf("[]");
            }
        );
    }

    public static Collection<Planet> decodePlanets(FriendlyByteBuf buf) {
        String json = buf.readUtf();
        JsonElement element = com.google.gson.JsonParser.parseString(json);
        DataResult<List<Planet>> result = Planet.CODEC.listOf().parse(JsonOps.INSTANCE, element);
        result.error().ifPresent(e -> AdAstra.LOGGER.error(e.message()));
        return result.result().orElse(Collections.emptyList());
    }

    public static ResourceKey<Level> getPlanetLocation(ResourceKey<Level> dimension) {
        return DIMENSIONS_TO_PLANETS.get(dimension);
    }

    @Nullable
    public static Planet getPlanet(ResourceKey<Level> location) {
        return PLANETS.get(location);
    }

    public static boolean isPlanet(ResourceKey<Level> location) {
        return PLANETS.containsKey(location);
    }

    public static boolean isSpace(ResourceKey<Level> location) {
        return isPlanet(location) && PLANETS.get(location).isSpace();
    }

    public static boolean canLaunchFrom(ResourceKey<Level> dimension) {
        return DIMENSIONS_TO_PLANETS.containsKey(dimension);
    }

    public static Map<ResourceKey<Level>, Planet> planets() {
        return PLANETS;
    }

    public static Set<Identifier> solarSystems() {
        return PLANETS.values().stream().map(Planet::solarSystem).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * All planets that declare the given dimension as their parent body via
     * {@code moon_of} (e.g. the Moon for the overworld, Europa for Jupiter),
     * sorted by tier then name for stable display order.
     */
    public static List<Planet> moonsOf(ResourceKey<Level> parent) {
        return PLANETS.values().stream()
            .filter(p -> p.moonOf().isPresent() && p.moonOf().get().equals(parent))
            .sorted(Comparator.<Planet>comparingInt(Planet::tier)
                .thenComparing(p -> p.dimension().identifier().getPath()))
            .toList();
    }

    public static boolean hasMoons(ResourceKey<Level> parent) {
        return PLANETS.values().stream()
            .anyMatch(p -> p.moonOf().isPresent() && p.moonOf().get().equals(parent));
    }

    /**
     * All planet dimensions that opt into the dynamic storm system via {@code has_storms}.
     * The server scheduler ticks exactly these dimensions.
     */
    public static List<Planet> stormPlanets() {
        return PLANETS.values().stream()
            .filter(Planet::hasStormsEnabled)
            .toList();
    }

    /**
     * Whether the given dimension runs the storm system. Works on both server and client
     * (planet data is synced via {@link earth.terrarium.adastra.common.network.packets.ClientboundSyncPlanetsPacket}).
     */
    public static boolean hasStorms(ResourceKey<Level> dimension) {
        Planet planet = getPlanet(dimension);
        return planet != null && planet.hasStormsEnabled();
    }

    public static void setPlanets(Map<ResourceKey<Level>, Planet> planets) {
        PLANETS.clear();
        PLANETS.putAll(planets);
    }
}
