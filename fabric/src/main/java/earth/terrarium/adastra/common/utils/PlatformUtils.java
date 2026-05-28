package earth.terrarium.adastra.common.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.function.Supplier;

public class PlatformUtils {

    public static Entity teleportToDimension(Entity entity, ServerLevel level, TeleportTransition teleportTransition) {
        return entity.teleport(teleportTransition);
    }

    public static Supplier<Item> createSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int primaryColor, int secondaryColor, Item.Properties properties) {
        // 1.21.11: SpawnEggItem constructor takes only Item.Properties.
        // Entity type is set via Item.Properties.spawnEgg() which adds the ENTITY_DATA component.
        // Colors are now baked into the spawn egg texture (no longer data-driven tints).
        return () -> new SpawnEggItem(properties.spawnEgg(type.get()));
    }
}
