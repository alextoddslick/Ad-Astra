package earth.terrarium.adastra.common.menus.base;

import com.teamresourceful.resourcefullib.common.menu.MenuContent;
import com.teamresourceful.resourcefullib.common.menu.MenuContentSerializer;
import earth.terrarium.adastra.common.handlers.base.SpaceStation;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PlanetsMenuContent(
    Set<Identifier> disabledPlanets,
    Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStations,
    Set<GlobalPos> spawnLocations
) implements MenuContent<PlanetsMenuContent> {

    public static final MenuContentSerializer<PlanetsMenuContent> SERIALIZER = new MenuContentSerializer<>() {
        @Override
        public @Nullable PlanetsMenuContent from(FriendlyByteBuf buffer) {
            Set<Identifier> disabledPlanets = PlanetsMenuProvider.createDisabledPlanetsFromBuf(buffer);
            Map<ResourceKey<Level>, Map<UUID, Set<SpaceStation>>> spaceStations = PlanetsMenuProvider.createSpaceStationsFromBuf(buffer);
            Set<GlobalPos> spawnLocations = PlanetsMenuProvider.createSpawnLocationsFromBuf(buffer);
            return new PlanetsMenuContent(disabledPlanets, spaceStations, spawnLocations);
        }

        @Override
        public void to(FriendlyByteBuf buffer, PlanetsMenuContent content) {
            PlanetsMenuProvider.writeToBuffer(buffer, content);
        }
    };

    @Override
    public MenuContentSerializer<PlanetsMenuContent> serializer() {
        return SERIALIZER;
    }
}
