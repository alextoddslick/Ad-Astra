package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.menu.MenuContentHelper;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.blockentities.machines.NasaWorkbenchBlockEntity;
import earth.terrarium.adastra.common.menus.machines.NasaWorkbenchUpgradeMenuProvider;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

/**
 * Sent when the player toggles between the NASA Workbench's crafting view and its suit-upgrade view.
 * Opening a menu is server-authoritative, so the client button asks the server to (re)open the
 * appropriate menu for the same block entity ({@code upgrade=true} -> the focused upgrade view,
 * {@code upgrade=false} -> the standard crafting grid).
 */
public record ServerboundOpenNasaWorkbenchMenuPacket(
    BlockPos machine, boolean upgrade
) implements Packet<ServerboundOpenNasaWorkbenchMenuPacket> {

    public static final ServerboundPacketType<ServerboundOpenNasaWorkbenchMenuPacket> TYPE = new Type();

    @Override
    public PacketType<ServerboundOpenNasaWorkbenchMenuPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Server<ServerboundOpenNasaWorkbenchMenuPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "open_nasa_workbench_menu"),
                ObjectByteCodec.create(
                    ExtraByteCodecs.BLOCK_POS.fieldOf(ServerboundOpenNasaWorkbenchMenuPacket::machine),
                    ByteCodec.BOOLEAN.fieldOf(ServerboundOpenNasaWorkbenchMenuPacket::upgrade),
                    ServerboundOpenNasaWorkbenchMenuPacket::new
                )
            );
        }

        @Override
        public Consumer<Player> handle(ServerboundOpenNasaWorkbenchMenuPacket packet) {
            return player -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                ModUtils.getMachineFromMenuPacket(packet.machine(), player, player.level()).ifPresent(machine -> {
                    if (!(machine instanceof NasaWorkbenchBlockEntity workbench)) return;
                    if (packet.upgrade()) {
                        MenuContentHelper.open(serverPlayer, new NasaWorkbenchUpgradeMenuProvider(workbench));
                    } else {
                        // The block entity itself is the provider for the standard crafting view.
                        MenuContentHelper.open(serverPlayer, workbench);
                    }
                });
            };
        }
    }
}
