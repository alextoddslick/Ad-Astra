package earth.terrarium.adastra.common.blockentities;

import earth.terrarium.adastra.client.radio.audio.RadioHandler;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ClientboundPlayStationPacket;
import earth.terrarium.adastra.common.registry.ModBlockEntityTypes;
import earth.terrarium.adastra.common.utils.radio.RadioHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RadioBlockEntity extends BlockEntity implements RadioHolder {

    private String station = "";

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.RADIO.get(), pos, state);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("Station").ifPresent(station -> {
            this.station = station;

            if (this.level == null) return;
            if (!this.level.isClientSide()) return;
            if (this.station.isBlank()) return;
            RadioHandler.play(this.station, this.level.getRandom(), this.worldPosition);
        });
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("Station", this.station);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull String getRadioUrl() {
        return this.station;
    }

    @Override
    public void setRadioUrl(@NotNull String url) {
        this.station = url;
        if (this.level == null) return;
        NetworkHandler.CHANNEL.sendToPlayersInRange(
            new ClientboundPlayStationPacket(url, this.worldPosition),
            this.level, this.worldPosition, RadioHolder.RANGE
        );
    }
}
