package earth.terrarium.adastra.common.handlers;

import com.mojang.serialization.Codec;
import earth.terrarium.adastra.common.handlers.base.SpaceStation;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Unified registry of every space station, keyed by orbit dimension and then by owner UUID.
 * <p>
 * Historically this data lived as per-{@link ServerLevel} {@link SavedData} on each orbit
 * dimension's data storage. That meant the planets menu had to ask each orbit dim for its
 * own copy, and on 1.21.x orbit dims may be lazily initialized — when the user opened the
 * menu from a non-orbit dim before that orbit dim had been visited, the saved data was not
 * yet populated and the list came back empty. Now everything is stored on the overworld
 * (which is always loaded), under {@code adastra_space_station_data_v2}, with the orbit
 * dimension as a top-level key. Legacy per-orbit files at
 * {@code adastra_space_station_data} are read once and merged forward; they are not
 * deleted, so a downgrade can still find them.
 */
public class SpaceStationHandler extends SavedData {

    /** New-format file name. Distinct from the legacy name so old per-orbit files survive. */
    private static final String FILE_NAME = "adastra_space_station_data_v2";
    /** Legacy per-orbit-dim file name. Read once during migration. */
    private static final String LEGACY_FILE_NAME = "adastra_space_station_data";

    private static final Codec<SpaceStationHandler> CODEC = CompoundTag.CODEC.xmap(
        tag -> {
            SpaceStationHandler handler = new SpaceStationHandler();
            handler.loadData(tag);
            return handler;
        },
        handler -> {
            CompoundTag tag = new CompoundTag();
            handler.saveData(tag);
            return tag;
        }
    );

    private static final SavedDataType<SpaceStationHandler> TYPE = new SavedDataType<>(
        FILE_NAME,
        SpaceStationHandler::new,
        CODEC,
        null
    );

    /** Map: orbit dimension -> (owner UUID -> stations). */
    private final Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> stationsByDim = new HashMap<>();

    /** Persisted so we don't re-run the legacy merge on every load. */
    private boolean legacyMigrated = false;

    private void loadData(CompoundTag tag) {
        legacyMigrated = tag.getBooleanOr("LegacyMigrated", false);
        CompoundTag dimsTag = tag.getCompoundOrEmpty("Dimensions");
        for (String key : dimsTag.keySet()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, id);
            Map<UUID, Set<SpaceStation>> ownerMap = readOwnerMap(dimsTag.getCompoundOrEmpty(key));
            if (!ownerMap.isEmpty()) stationsByDim.put(dim, ownerMap);
        }
    }

    private static Map<UUID, Set<SpaceStation>> readOwnerMap(CompoundTag ownerTag) {
        Map<UUID, Set<SpaceStation>> ownerMap = new HashMap<>();
        for (String idKey : ownerTag.keySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(idKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ListTag stationsTag = ownerTag.getListOrEmpty(idKey);
            Set<SpaceStation> stations = readStationList(stationsTag);
            if (!stations.isEmpty()) ownerMap.put(uuid, stations);
        }
        return ownerMap;
    }

    private static Set<SpaceStation> readStationList(ListTag stationsTag) {
        Set<SpaceStation> stations = new HashSet<>();
        stationsTag.forEach(stationTag -> {
            CompoundTag stationCompoundTag = (CompoundTag) stationTag;
            String nameJson = stationCompoundTag.getStringOr("Name", "\"\"");
            Component name = ComponentSerialization.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(nameJson))
                .result()
                .orElse(Component.empty());
            ChunkPos position = new ChunkPos(stationCompoundTag.getLongOr("Position", 0L));
            stations.add(new SpaceStation(position, name));
        });
        return stations;
    }

    private void saveData(CompoundTag tag) {
        tag.putBoolean("LegacyMigrated", legacyMigrated);
        CompoundTag dimsTag = new CompoundTag();
        stationsByDim.forEach((dim, ownerMap) -> {
            CompoundTag ownerTag = new CompoundTag();
            ownerMap.forEach((id, stations) -> {
                ListTag list = new ListTag();
                for (var station : stations) {
                    CompoundTag stationsTag = new CompoundTag();
                    String nameJson = ComponentSerialization.CODEC
                        .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, station.name())
                        .result()
                        .map(Object::toString)
                        .orElse("\"\"");
                    stationsTag.putString("Name", nameJson);
                    stationsTag.putLong("Position", station.position().toLong());
                    list.add(stationsTag);
                }
                ownerTag.put(id.toString(), list);
            });
            dimsTag.put(dim.identifier().toString(), ownerTag);
        });
        tag.put("Dimensions", dimsTag);
    }

    /**
     * Loads (and lazily migrates) the unified handler from the overworld data storage.
     * The orbit-dim {@link ServerLevel} that callers used to pass is now only used to
     * derive the {@link MinecraftServer}; the actual SavedData lives on
     * {@code server.overworld()}.
     */
    public static SpaceStationHandler read(MinecraftServer server) {
        SpaceStationHandler handler = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (!handler.legacyMigrated) {
            handler.migrateLegacyData(server);
        }
        return handler;
    }

    public static SpaceStationHandler read(ServerLevel level) {
        return read(level.getServer());
    }

    /**
     * One-time merge of the legacy per-orbit-dim {@code adastra_space_station_data} files.
     * Walks every known planet's orbit dimension, asks the server for the
     * {@link ServerLevel} (force-loading it if necessary), reads the legacy file, and
     * folds it under the orbit dim's key. Legacy files are NOT deleted.
     */
    private void migrateLegacyData(MinecraftServer server) {
        // Mark migrated even if nothing was found, so we don't repeat the scan every call.
        legacyMigrated = true;
        try {
            for (var planet : AdAstraData.planets().values()) {
                ResourceKey<Level> orbit = planet.orbitIfPresent();
                if (orbit == null) continue;
                ServerLevel orbitLevel = server.getLevel(orbit);
                if (orbitLevel == null) continue;

                Map<UUID, Set<SpaceStation>> legacy = readLegacyForLevel(orbitLevel);
                if (!legacy.isEmpty()) {
                    mergeDim(orbit, legacy);
                }
            }
        } finally {
            setDirty();
        }
    }

    /**
     * Reads the legacy flat-owner-map file from {@code orbitLevel}'s data storage. The old
     * shape was a root CompoundTag whose keys were owner-UUID strings and whose values were
     * ListTag of stations. We use a one-shot {@link SavedDataType} pointing at the legacy
     * file name so we don't disturb the new-format file.
     */
    private static Map<UUID, Set<SpaceStation>> readLegacyForLevel(ServerLevel orbitLevel) {
        SavedDataType<LegacyFlat> legacyType = new SavedDataType<>(
            LEGACY_FILE_NAME,
            LegacyFlat::new,
            LegacyFlat.CODEC,
            null
        );
        LegacyFlat flat = orbitLevel.getDataStorage().computeIfAbsent(legacyType);
        return flat.owners;
    }

    private void mergeDim(ResourceKey<Level> dim, Map<UUID, Set<SpaceStation>> incoming) {
        if (incoming.isEmpty()) return;
        Map<UUID, Set<SpaceStation>> existing = stationsByDim.computeIfAbsent(dim, k -> new HashMap<>());
        incoming.forEach((uuid, set) -> {
            Set<SpaceStation> target = existing.computeIfAbsent(uuid, k -> new HashSet<>());
            target.addAll(set);
        });
    }

    /** Snapshot for one orbit dim, fetched from the unified overworld store. */
    public static Map<UUID, Set<SpaceStation>> getAllSpaceStations(ServerLevel orbitLevel) {
        return getAllSpaceStations(orbitLevel.getServer(), orbitLevel.dimension());
    }

    public static Map<UUID, Set<SpaceStation>> getAllSpaceStations(MinecraftServer server, ResourceKey<Level> orbitDim) {
        return read(server).stationsByDim.getOrDefault(orbitDim, Map.of());
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public static void constructSpaceStation(ServerPlayer player, ServerLevel orbitLevel, Component name) {
        SpaceStationHandler handler = read(orbitLevel.getServer());
        Map<UUID, Set<SpaceStation>> ownerMap = handler.stationsByDim
            .computeIfAbsent(orbitLevel.dimension(), k -> new HashMap<>());
        Set<SpaceStation> stations = ownerMap.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        stations.add(new SpaceStation(player.chunkPosition(), name));
        handler.setDirty();
    }

    public static boolean isInSpaceStation(ServerPlayer player, ServerLevel orbitLevel) {
        Map<UUID, Set<SpaceStation>> ownerMap = read(orbitLevel.getServer())
            .stationsByDim.getOrDefault(orbitLevel.dimension(), Map.of());
        for (var stations : ownerMap.values()) {
            for (var station : stations) {
                if (station.position().getChessboardDistance(player.chunkPosition()) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<SpaceStation> getOwnedSpaceStations(ServerPlayer player, ServerLevel orbitLevel) {
        return getOwnedSpaceStations(player.getUUID(), orbitLevel);
    }

    public static Set<SpaceStation> getOwnedSpaceStations(UUID id, ServerLevel orbitLevel) {
        return read(orbitLevel.getServer())
            .stationsByDim.getOrDefault(orbitLevel.dimension(), Map.of())
            .getOrDefault(id, Set.of());
    }

    /** Removes a station owned by {@code id} at {@code chunk} in {@code orbitLevel}. */
    public static boolean removeStation(UUID id, ServerLevel orbitLevel, ChunkPos chunk) {
        SpaceStationHandler handler = read(orbitLevel.getServer());
        Map<UUID, Set<SpaceStation>> ownerMap = handler.stationsByDim.get(orbitLevel.dimension());
        if (ownerMap == null) return false;
        Set<SpaceStation> stations = ownerMap.get(id);
        if (stations == null) return false;
        boolean removed = stations.removeIf(s -> Objects.equals(s.position(), chunk));
        if (removed) handler.setDirty();
        return removed;
    }

    /**
     * Read-only adapter over the legacy flat owner-map shape. Only used during migration.
     */
    private static final class LegacyFlat extends SavedData {
        private static final Codec<LegacyFlat> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                LegacyFlat data = new LegacyFlat();
                for (String key : tag.keySet()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(key);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    Set<SpaceStation> stations = readStationList(tag.getListOrEmpty(key));
                    if (!stations.isEmpty()) data.owners.put(uuid, stations);
                }
                return data;
            },
            data -> {
                // We never write back to the legacy file. Return an empty tag so any
                // accidental flush is a no-op (we can't actually prevent a save from
                // occurring, but we leave the legacy data on disk untouched in practice
                // because we never call setDirty on this instance).
                return new CompoundTag();
            }
        );

        final Map<UUID, Set<SpaceStation>> owners = new HashMap<>();

        @Override
        public boolean isDirty() {
            return false;
        }
    }
}
