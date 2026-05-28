package earth.terrarium.adastra.common.menus.base;

import com.teamresourceful.resourcefullib.common.menu.MenuContent;
import com.teamresourceful.resourcefullib.common.menu.MenuContentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public record EntityIdContent(int entityId) implements MenuContent<EntityIdContent> {

    public static final MenuContentSerializer<EntityIdContent> SERIALIZER = new MenuContentSerializer<>() {
        @Override
        public @Nullable EntityIdContent from(FriendlyByteBuf buffer) {
            return new EntityIdContent(buffer.readVarInt());
        }

        @Override
        public void to(FriendlyByteBuf buffer, EntityIdContent content) {
            buffer.writeVarInt(content.entityId());
        }
    };

    @Override
    public MenuContentSerializer<EntityIdContent> serializer() {
        return SERIALIZER;
    }
}
