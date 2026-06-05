package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.utils.ClientStormData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Broadcasts the current storm state of a dimension to the players in it. Sent by
 * {@code PlanetStormHandler} on state change and on a periodic heartbeat (so freshly
 * joined / dimension-changed players sync within a second). The client stores it in
 * {@link ClientStormData} and gates wind + HUD by comparing {@code dimension} to the
 * player's current level.
 *
 * @param phase 0 = none, 1 = storm approaching, 2 = skies clearing (see {@link ClientStormData}).
 */
public record ClientboundSyncStormPacket(
    ResourceKey<Level> dimension,
    boolean storming,
    float intensity,
    byte phase
) implements Packet<ClientboundSyncStormPacket> {

    public static final byte PHASE_NONE = 0;
    public static final byte PHASE_APPROACHING = 1;
    public static final byte PHASE_CLEARING = 2;

    public static final ClientboundPacketType<ClientboundSyncStormPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundSyncStormPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Client<ClientboundSyncStormPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "sync_storm"),
                ObjectByteCodec.create(
                    ExtraByteCodecs.DIMENSION.fieldOf(ClientboundSyncStormPacket::dimension),
                    ByteCodec.BOOLEAN.fieldOf(ClientboundSyncStormPacket::storming),
                    ByteCodec.FLOAT.fieldOf(ClientboundSyncStormPacket::intensity),
                    ByteCodec.BYTE.fieldOf(ClientboundSyncStormPacket::phase),
                    ClientboundSyncStormPacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundSyncStormPacket packet) {
            return () -> ClientStormData.update(packet.dimension(), packet.storming(), packet.intensity(), packet.phase());
        }
    }
}
