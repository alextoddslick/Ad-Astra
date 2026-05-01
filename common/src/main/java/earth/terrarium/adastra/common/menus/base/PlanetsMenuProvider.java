package earth.terrarium.adastra.common.menus.base;

import com.teamresourceful.resourcefullib.common.menu.ContentMenuProvider;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.handlers.LaunchingDimensionHandler;
import earth.terrarium.adastra.common.handlers.SpaceStationHandler;
import earth.terrarium.adastra.common.handlers.base.SpaceStation;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.planets.AdAstraData;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class PlanetsMenuProvider implements ContentMenuProvider<PlanetsMenuContent> {

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PlanetsMenu(containerId, inventory, Set.of(), Map.of(), Set.of());
    }

    @Override
    public PlanetsMenuContent createContent(ServerPlayer player) {
        Set<Identifier> disabledPlanets = new HashSet<>();
        String[] planets = AdAstraConfig.disabledPlanets.split(",");
        for (var planet : planets) {
            if (!planet.isBlank()) {
                disabledPlanets.add(Identifier.tryParse(planet));
            }
        }

        Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStationsMap = new HashMap<>();
        AdAstraData.planets().values().forEach(planet -> {
            // Space stations are stored in the orbit dimension, not the planet dimension.
            // The data itself lives on the overworld now (see SpaceStationHandler), so we
            // only need the orbit dim's ResourceKey, not its ServerLevel — but we still
            // ask for the ServerLevel because the existing migration path inside
            // SpaceStationHandler.read() consumes any legacy per-orbit-dim file when the
            // orbit dim is present.
            ResourceKey<Level> orbitDimension = planet.orbitIfPresent();
            var stations = SpaceStationHandler.getAllSpaceStations(player.level().getServer(), orbitDimension);
            // Snapshot the per-dimension data so a later mutation of the live SavedData map
            // (e.g. another construct on the server before the buffer is written) cannot
            // race with serialization.
            Map<UUID, Set<SpaceStation>> snapshot = new HashMap<>();
            stations.forEach((uuid, set) -> snapshot.put(uuid, new HashSet<>(set)));
            spaceStationsMap.put(orbitDimension, snapshot);
        });

        List<GlobalPos> locations = new ArrayList<>();
        AdAstraData.planets().forEach((dimension, planet) ->
            LaunchingDimensionHandler.getSpawningLocation(player, player.level(), planet).ifPresent(locations::add));

        return new PlanetsMenuContent(
            Collections.unmodifiableSet(disabledPlanets),
            Collections.unmodifiableMap(spaceStationsMap),
            Collections.unmodifiableSet(new HashSet<>(locations))
        );
    }

    public static void writeToBuffer(FriendlyByteBuf buffer, PlanetsMenuContent content) {
        buffer.writeUtf(String.join(",", content.disabledPlanets().stream().map(Identifier::toString).toList()));

        buffer.writeVarInt(content.spaceStations().size());
        content.spaceStations().forEach((dimension, stationGroups) -> {
            buffer.writeResourceKey(dimension);
            buffer.writeVarInt(stationGroups.size());
            stationGroups.forEach((id, stations) -> {
                buffer.writeVarInt(stations.size());
                stations.forEach(station -> {
                    buffer.writeUtf(componentToJson(station.name()));
                    buffer.writeChunkPos(station.position());
                });
                buffer.writeUUID(id);
            });
        });

        buffer.writeVarInt(content.spawnLocations().size());
        content.spawnLocations().forEach(globalPos -> {
            buffer.writeResourceKey(globalPos.dimension());
            buffer.writeBlockPos(globalPos.pos());
        });
    }

    public static Set<Identifier> createDisabledPlanetsFromBuf(FriendlyByteBuf buf) {
        Set<Identifier> disabledPlanets = new HashSet<>();
        String[] planets = buf.readUtf().split(",");
        for (var planet : planets) {
            if (!planet.isBlank()) {
                disabledPlanets.add(Identifier.tryParse(planet));
            }
        }
        return Collections.unmodifiableSet(disabledPlanets);
    }

    public static Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> createSpaceStationsFromBuf(FriendlyByteBuf buf) {
        Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStationsMap = new HashMap<>();

        int planetsSize = buf.readVarInt();
        for (int i = 0; i < planetsSize; i++) {
            ResourceKey<Level> planetKey = buf.readResourceKey(Registries.DIMENSION);
            int spaceStationsSize = buf.readVarInt();

            Map<UUID, Set<SpaceStation>> spaceStationGroupMap = new HashMap<>();
            for (int j = 0; j < spaceStationsSize; j++) {
                int stationGroupSize = buf.readVarInt();
                Set<SpaceStation> spaceStations = new HashSet<>();

                for (int k = 0; k < stationGroupSize; k++) {
                    Component stationName = componentFromJson(buf.readUtf());
                    ChunkPos stationPos = buf.readChunkPos();
                    spaceStations.add(new SpaceStation(stationPos, stationName));
                }

                UUID id = buf.readUUID();
                spaceStationGroupMap.put(id, spaceStations);
            }

            spaceStationsMap.put(planetKey, spaceStationGroupMap);
        }

        return Collections.unmodifiableMap(spaceStationsMap);
    }

    public static Set<GlobalPos> createSpawnLocationsFromBuf(FriendlyByteBuf buf) {
        Set<GlobalPos> locations = new HashSet<>();
        int locationCount = buf.readVarInt();
        for (int i = 0; i < locationCount; i++) {
            ResourceKey<Level> dimension = buf.readResourceKey(Registries.DIMENSION);
            BlockPos pos = buf.readBlockPos();
            locations.add(GlobalPos.of(dimension, pos));
        }
        return Collections.unmodifiableSet(locations);
    }

    private static String componentToJson(Component component) {
        RegistryOps<JsonElement> ops = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.encodeStart(ops, component)
            .getOrThrow()
            .toString();
    }

    private static Component componentFromJson(String json) {
        RegistryOps<JsonElement> ops = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.decode(ops, JsonParser.parseString(json))
            .getOrThrow()
            .getFirst();
    }
}
