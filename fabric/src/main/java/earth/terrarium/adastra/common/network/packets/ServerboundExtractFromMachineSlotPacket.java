package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.blockentities.base.ContainerMachineBlockEntity;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Bespoke server-bound extraction packet that bypasses vanilla's container click flow entirely
 * for menus where the click never reaches {@code AbstractContainerMenu#clicked()} (Compressor,
 * Etrionic Blast Furnace, Cryo Freezer). The client sends this whenever the user empty-cursor
 * left-clicks a machine slot in one of those screens; the server takes the items directly from
 * the BE container and adds them to the player's inventory.
 */
public record ServerboundExtractFromMachineSlotPacket(
    BlockPos machine, int slotIndex, boolean fullStack
) implements Packet<ServerboundExtractFromMachineSlotPacket> {

    public static final ServerboundPacketType<ServerboundExtractFromMachineSlotPacket> TYPE = new Type();

    private static final Logger LOG = Logger.getLogger("ADASTRA-EXTRACT-PACKET");

    @Override
    public PacketType<ServerboundExtractFromMachineSlotPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Server<ServerboundExtractFromMachineSlotPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "extract_from_machine_slot"),
                ObjectByteCodec.create(
                    ExtraByteCodecs.BLOCK_POS.fieldOf(ServerboundExtractFromMachineSlotPacket::machine),
                    ByteCodec.INT.fieldOf(ServerboundExtractFromMachineSlotPacket::slotIndex),
                    ByteCodec.BOOLEAN.fieldOf(ServerboundExtractFromMachineSlotPacket::fullStack),
                    ServerboundExtractFromMachineSlotPacket::new
                )
            );
        }

        @Override
        public Consumer<Player> handle(ServerboundExtractFromMachineSlotPacket packet) {
            return player -> ModUtils.getMachineFromMenuPacket(packet.machine(), player, player.level()).ifPresent(
                machine -> extract(packet, player, machine)
            );
        }

        private static void extract(ServerboundExtractFromMachineSlotPacket packet, Player player, ContainerMachineBlockEntity machine) {
            int slot = packet.slotIndex();
            if (slot < 0 || slot >= machine.getContainerSize()) {
                LOG.info("[ExtractPacket] rejected: slot " + slot + " out of bounds (size=" + machine.getContainerSize() + ")");
                return;
            }
            ItemStack inSlot = machine.getItem(slot);
            if (inSlot.isEmpty()) {
                LOG.info("[ExtractPacket] slot " + slot + " is empty, nothing to do");
                return;
            }
            int take = packet.fullStack() ? inSlot.getCount() : (inSlot.getCount() + 1) / 2;
            ItemStack picked = inSlot.copy();
            picked.setCount(take);
            ItemStack remaining = inSlot.copy();
            remaining.setCount(inSlot.getCount() - take);
            machine.setItem(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            machine.setChanged();

            // Add to player inventory; drop overflow on the ground.
            boolean added = player.getInventory().add(picked);
            if (!added || !picked.isEmpty()) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), picked);
            }

            LOG.info("[ExtractPacket] extracted " + take + "x from slot " + slot + " of " + machine.getBlockPos()
                + "; remaining=" + machine.getItem(slot));

            // Force-resync the open container so the client sees the new state immediately.
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu != null) {
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }
}
