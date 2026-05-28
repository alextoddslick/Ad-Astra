package earth.terrarium.adastra.common.menus;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.common.compat.argonauts.ArgonautsIntegration;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.handlers.base.SpaceStation;
import earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundConstructSpaceStationPacket;
import earth.terrarium.adastra.common.planets.AdAstraData;
import earth.terrarium.adastra.common.recipes.SpaceStationRecipe;
import earth.terrarium.adastra.common.recipes.base.IngredientHolder;
import earth.terrarium.adastra.common.registry.ModMenus;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.*;

public class PlanetsMenu extends AbstractContainerMenu {

    protected final int tier;
    protected final Inventory inventory;
    protected final Player player;
    protected final Level level;
    protected final Set<Identifier> disabledPlanets;
    protected final Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStations;
    protected final Map<ResourceKey<Level>, List<Pair<ItemStack, Integer>>> ingredients;
    protected final Object2BooleanMap<ResourceKey<Level>> claimedChunks = new Object2BooleanOpenHashMap<>();
    protected final Set<GlobalPos> spawnLocations;

    public PlanetsMenu(int containerId,
                       Inventory inventory,
                       Set<Identifier> disabledPlanets,
                       Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStations,
                       Set<GlobalPos> spawnLocations
    ) {
        super(ModMenus.PLANETS.get(), containerId);
        this.inventory = inventory;
        player = inventory.player;
        level = player.level();
        tier = player.getVehicle() instanceof Rocket vehicle ? vehicle.tier() : 100;
        this.disabledPlanets = disabledPlanets;
        this.spaceStations = spaceStations;
        this.ingredients = getSpaceStationRecipes();
        this.spawnLocations = spawnLocations;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public int tier() {
        return tier;
    }

    public Player player() {
        return player;
    }

    public Set<Identifier> disabledPlanets() {
        return disabledPlanets;
    }

    public Map<ResourceKey<Level>, List<Pair<ItemStack, Integer>>> ingredients() {
        return ingredients;
    }

    public void clearClaimedChunks() {
        claimedChunks.clear();
    }

    public void setClaimedChunk(ResourceKey<Level> dimension, boolean claimed) {
        claimedChunks.put(dimension, claimed);
    }

    public boolean isClaimed(ResourceKey<Level> dimension) {
        return claimedChunks.getBoolean(dimension);
    }

    public boolean canConstruct(ResourceKey<Level> dimension) {
        if (isClaimed(dimension)) return false;
        var recipe = SpaceStationRecipe.getSpaceStation(level, dimension).orElse(null);
        if (recipe == null) return false;
        if (player.isCreative()) return true;
        return SpaceStationRecipe.hasIngredients(player, level, recipe.value());
    }

    @SuppressWarnings("unchecked")
    private Map<ResourceKey<Level>, List<Pair<ItemStack, Integer>>> getSpaceStationRecipes() {
        var server = level.getServer();
        // On the client in 1.21.11, Level.getServer() returns null.
        // Fall back to the integrated server for singleplayer.
        if (server == null && level.isClientSide()) {
            try {
                server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            } catch (Exception ignored) {}
        }
        if (server == null) return new HashMap<>();
        List<SpaceStationRecipe> spaceStationRecipes = server.getRecipeManager().getRecipes().stream()
            .filter(holder -> holder.value().getType() == ModRecipeTypes.SPACE_STATION_RECIPE.get())
            .map(holder -> ((RecipeHolder<SpaceStationRecipe>) (RecipeHolder<?>) holder).value())
            .toList();
        Map<ResourceKey<Level>, List<Pair<ItemStack, Integer>>> recipes = new HashMap<>(spaceStationRecipes.size());
        for (var recipe : spaceStationRecipes) {
            for (IngredientHolder holder : recipe.ingredients()) {
                int count = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    var stack = inventory.getItem(i);
                    if (holder.ingredient().test(stack)) {
                        count += stack.getCount();
                    }
                }
                ItemStack displayStack = holder.ingredient().items().findFirst()
                    .map(h -> new ItemStack(h, holder.count()))
                    .orElse(ItemStack.EMPTY);
                recipes.computeIfAbsent(recipe.dimension(), k -> new ArrayList<>()).add(new Pair<>(displayStack, count));
            }
        }
        return recipes;
    }

    /**
     * Checks if the player is in a space station at or within 2 chunks of the player's current position.
     *
     * @param dimension The dimension to check.
     * @return True if the player is in a space station, false otherwise.
     */
    public boolean isInSpaceStation(ResourceKey<Level> dimension) {
        var pos = player.chunkPosition();
        var allStations = spaceStations.get(dimension);
        if (allStations == null) return false;
        for (var stations : allStations.values()) {
            for (var station : stations) {
                if (station.position().getChessboardDistance(pos) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds an accessible station (owned by this player or by a teammate) at or
     * within 2 chunks of the player's current chunk position. The lookup uses
     * the same proximity rule as {@link #isInSpaceStation(ResourceKey)} so the
     * "already exists at this location" case can offer a Land affordance when
     * the existing station is one the player is allowed to land on.
     *
     * @param dimension orbit dimension to search in
     * @return the nearest accessible station, or {@code null} if none
     */
    @org.jetbrains.annotations.Nullable
    public SpaceStation getAccessibleStationNearPlayer(ResourceKey<Level> dimension) {
        var pos = player.chunkPosition();
        for (var pair : getOwnedAndTeamSpaceStations(dimension)) {
            SpaceStation station = pair.getSecond();
            if (station.position().getChessboardDistance(pos) <= 2) {
                return station;
            }
        }
        return null;
    }

    public List<Pair<String, SpaceStation>> getOwnedSpaceStations(ResourceKey<Level> dimension, GameProfile player) {
        var allStations = spaceStations.get(dimension);
        if (allStations == null) return List.of();
        Set<SpaceStation> stations = allStations.get(player.id());
        if (stations == null) return List.of();

        return stations.stream()
            .sorted(Comparator.comparing(station -> station.name().getString()))
            .map(station -> new Pair<>(player.name(), station)).toList();
    }

    public List<Pair<String, SpaceStation>> getOwnedAndTeamSpaceStations(ResourceKey<Level> dimension) {
        List<Pair<String, SpaceStation>> stations = new ArrayList<>(getOwnedSpaceStations(dimension, player.getGameProfile()));

        if (!ArgonautsIntegration.argonautsLoaded()) return stations;

        for (var member : ArgonautsIntegration.getClientPartyMembers(player.getUUID())) {
            if (member.equals(player.getGameProfile())) continue;
            stations.addAll(getOwnedSpaceStations(dimension, member));
        }
        for (var member : ArgonautsIntegration.getClientGuildMembers(player.getUUID())) {
            if (member.equals(player.getGameProfile())) continue;
            stations.addAll(getOwnedSpaceStations(dimension, member));
        }
        return stations;
    }

    /**
     * All stations in the dim, regardless of owner. Used to populate the Land list
     * in single-player / dev sessions where the player UUID may not match the station's
     * recorded owner (Loom regenerates the dev UUID from the username on each run).
     * The server's ServerboundLandOnSpaceStationPacket still gates landing on
     * ownership, except it allows any landing when the integrated server is the host.
     */
    public List<Pair<String, SpaceStation>> getAllSpaceStations(ResourceKey<Level> dimension) {
        var allStations = spaceStations.get(dimension);
        if (allStations == null) return List.of();
        List<Pair<String, SpaceStation>> out = new ArrayList<>();
        for (var entry : allStations.entrySet()) {
            String ownerLabel = entry.getKey().equals(player.getUUID()) ? player.getGameProfile().name() : entry.getKey().toString().substring(0, 8);
            for (var station : entry.getValue()) {
                out.add(new Pair<>(ownerLabel, station));
            }
        }
        return out;
    }

    public void constructSpaceStation(ResourceKey<Level> dimension, Component name) {
        NetworkHandler.CHANNEL.sendToServer(new ServerboundConstructSpaceStationPacket(dimension, name));
    }

    public BlockPos getLandingPos(ResourceKey<Level> dimension, boolean tryPreviousLocation) {
        boolean landingNormally = tryPreviousLocation && player.getVehicle() instanceof Rocket;
        if (!landingNormally) player.blockPosition();

        for (var pos : spawnLocations) {
            if (pos.dimension().equals(dimension)) {
                return pos.pos();
            }
        }

        return player.blockPosition();
    }

    public Component getPlanetName(ResourceKey<Level> dimension) {
        return Component.translatableWithFallback("planet.%s.%s".formatted(dimension.identifier().getNamespace(), dimension.identifier().getPath()), PlanetsScreen.title(dimension.identifier().getPath()));
    }

    public List<Planet> getSortedPlanets() {
        return AdAstraData.planets().values().stream()
            .filter(planet -> !disabledPlanets().contains(planet.dimension().identifier()))
            .filter(planet -> tier() >= planet.tier())
            .sorted(Comparator.comparingInt(Planet::tier).thenComparing(p -> getPlanetName(p.dimension()).getString()))
            .toList();
    }
}
