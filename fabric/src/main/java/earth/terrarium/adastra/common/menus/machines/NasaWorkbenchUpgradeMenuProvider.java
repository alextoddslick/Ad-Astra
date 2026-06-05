package earth.terrarium.adastra.common.menus.machines;

import com.teamresourceful.resourcefullib.common.menu.ContentMenuProvider;
import earth.terrarium.adastra.common.blockentities.machines.NasaWorkbenchBlockEntity;
import earth.terrarium.adastra.common.menus.base.BlockPosContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the focused {@link NasaWorkbenchUpgradeMenu} view for an existing NASA Workbench block
 * entity. Used by {@code ServerboundOpenNasaWorkbenchMenuPacket} to (re)open the upgrade view
 * server-side so the shared container stays in sync in multiplayer.
 */
public record NasaWorkbenchUpgradeMenuProvider(NasaWorkbenchBlockEntity entity)
    implements ContentMenuProvider<BlockPosContent> {

    @Override
    public BlockPosContent createContent(ServerPlayer player) {
        return new BlockPosContent(entity.getBlockPos());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new NasaWorkbenchUpgradeMenu(id, inventory, entity);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.ad_astra.suit_upgrades");
    }
}
