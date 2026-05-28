package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.utils.radio.StationLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public record ServerboundRequestStationsPacket() implements Packet<ServerboundRequestStationsPacket> {

    public static final ServerboundPacketType<ServerboundRequestStationsPacket> TYPE = new Type();

    @Override
    public PacketType<ServerboundRequestStationsPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Server<ServerboundRequestStationsPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "request_stations"),
                ByteCodec.unit(ServerboundRequestStationsPacket::new)
            );
        }

        @Override
        public Consumer<Player> handle(ServerboundRequestStationsPacket packet) {
            return player -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSendStationsPacket(StationLoader.stations()), serverPlayer);
                }
            };
        }
    }
}
