package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.utils.ModUtils;
import earth.terrarium.common_storage_lib.fluid.util.FluidProvider;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import earth.terrarium.common_storage_lib.storage.base.StorageSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public record ServerboundClearFluidTankPacket(
    BlockPos machine, int tank
) implements Packet<ServerboundClearFluidTankPacket> {

    public static final ServerboundPacketType<ServerboundClearFluidTankPacket> TYPE = new Type();

    @Override
    public PacketType<ServerboundClearFluidTankPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Server<ServerboundClearFluidTankPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "clear_fluid_tank"),
                ObjectByteCodec.create(
                    ExtraByteCodecs.BLOCK_POS.fieldOf(ServerboundClearFluidTankPacket::machine),
                    ByteCodec.INT.fieldOf(ServerboundClearFluidTankPacket::tank),
                    ServerboundClearFluidTankPacket::new
                )
            );
        }

        @Override
        public Consumer<Player> handle(ServerboundClearFluidTankPacket packet) {
            return player -> ModUtils.getMachineFromMenuPacket(packet.machine(), player, player.level()).ifPresent(
                machine -> {
                    if (machine instanceof FluidProvider.BlockEntity fluidProvider) {
                        CommonStorage<FluidResource> storage = fluidProvider.getFluids(null);
                        if (storage != null && packet.tank() >= 0 && packet.tank() < storage.size()) {
                            StorageSlot<FluidResource> slot = storage.get(packet.tank());
                            slot.extract(slot.getResource(), slot.getAmount(), false);
                            machine.setChanged();
                        }
                    }
                }
            );
        }
    }
}
