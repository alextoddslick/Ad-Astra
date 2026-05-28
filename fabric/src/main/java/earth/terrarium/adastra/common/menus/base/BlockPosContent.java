package earth.terrarium.adastra.common.menus.base;

import com.teamresourceful.resourcefullib.common.menu.MenuContent;
import com.teamresourceful.resourcefullib.common.menu.MenuContentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public record BlockPosContent(BlockPos pos) implements MenuContent<BlockPosContent> {

    public static final MenuContentSerializer<BlockPosContent> SERIALIZER = new MenuContentSerializer<>() {
        @Override
        public @Nullable BlockPosContent from(FriendlyByteBuf buffer) {
            return new BlockPosContent(buffer.readBlockPos());
        }

        @Override
        public void to(FriendlyByteBuf buffer, BlockPosContent content) {
            buffer.writeBlockPos(content.pos());
        }
    };

    @Override
    public MenuContentSerializer<BlockPosContent> serializer() {
        return SERIALIZER;
    }
}
