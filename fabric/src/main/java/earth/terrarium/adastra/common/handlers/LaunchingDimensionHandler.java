package earth.terrarium.adastra.common.handlers;

import com.mojang.serialization.Codec;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class LaunchingDimensionHandler extends SavedData {

    private static final Codec<LaunchingDimensionHandler> CODEC = CompoundTag.CODEC.xmap(
        tag -> {
            LaunchingDimensionHandler handler = new LaunchingDimensionHandler();
            handler.loadData(tag);
            return handler;
        },
        handler -> {
            CompoundTag tag = new CompoundTag();
            handler.saveData(tag);
            return tag;
        }
    );

    private static final SavedDataType<LaunchingDimensionHandler> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("adastra", "launching_dimensions"),
        LaunchingDimensionHandler::new,
        CODEC,
        null
    );

    private final Map<UUID, LaunchedDimensions> data = new HashMap<>();

    private void loadData(CompoundTag tag) {
        tag.keySet().forEach(key -> {
            UUID uuid = UUID.fromString(key);
            CompoundTag planetsTag = tag.getCompoundOrEmpty(key);
            Map<ResourceKey<Level>, GlobalPos> planets = new HashMap<>();
            for (String allKey : planetsTag.keySet()) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, planetsTag.getCompoundOrEmpty(allKey))
                    .result()
                    .ifPresent(pos -> {
                        Identifier planet = Identifier.tryParse(allKey);
                        planets.put(ResourceKey.create(Registries.DIMENSION, Objects.requireNonNull(planet)), pos);
                    });
            }
            this.data.put(uuid, new LaunchedDimensions(planets));
        });
    }

    private void saveData(CompoundTag tag) {
        this.data.forEach((uuid, data) -> {
            CompoundTag planetsTag = new CompoundTag();
            for (var entry : data.dimensions.entrySet()) {
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .result()
                    .ifPresent(pos -> planetsTag.put(entry.getKey().identifier().toString(), pos));
            }
            tag.put(uuid.toString(), planetsTag);
        });
    }

    public static LaunchingDimensionHandler read(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static LaunchedDimensions get(Player player, ServerLevel level, boolean create) {
        var data = read(level).data;
        if (data.containsKey(player.getUUID())) {
            return data.get(player.getUUID());
        } else if (create) {
            var dimensions = new LaunchedDimensions();
            data.put(player.getUUID(), dimensions);
            return dimensions;
        }
        return null;
    }

    public static Optional<GlobalPos> getSpawningLocation(Player player, ServerLevel level, Planet planet) {
        LaunchedDimensions dimensions = get(player, level, false);
        if (dimensions == null) return Optional.empty();
        GlobalPos pos = dimensions.dimensions.get(planet.dimension());
        if (pos != null) {
            boolean validPlanet = planet.additionalLaunchDimensions().contains(pos.dimension()) || planet.dimension().equals(pos.dimension());
            if (validPlanet) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    public static Collection<GlobalPos> getAllSpawnLocations(ServerPlayer player) {
        Set<GlobalPos> positions = new HashSet<>();
        LaunchedDimensions dimensions = get(player, (ServerLevel) player.level(), false);
        if (dimensions == null) return positions;
        for (var entry : dimensions.dimensions.entrySet()) {
            positions.add(entry.getValue());
        }
        return positions;
    }

    public static void addSpawnLocation(Player player, ServerLevel level) {
        LaunchedDimensions dimensions = get(player, level, true);
        if (dimensions == null) return;
        ResourceKey<Level> planet = AdAstraData.getPlanetLocation(level.dimension());
        if (planet == null) return;
        dimensions.dimensions.put(planet, GlobalPos.of(level.dimension(), player.blockPosition()));
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private record LaunchedDimensions(Map<ResourceKey<Level>, GlobalPos> dimensions) {

        public LaunchedDimensions() {
            this(new HashMap<>());
        }
    }
}
