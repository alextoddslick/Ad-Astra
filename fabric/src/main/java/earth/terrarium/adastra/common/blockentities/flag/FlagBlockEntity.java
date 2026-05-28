package earth.terrarium.adastra.common.blockentities.flag;

import com.mojang.authlib.GameProfile;
import earth.terrarium.adastra.common.blockentities.flag.content.FlagContent;
import earth.terrarium.adastra.common.blockentities.flag.content.UrlContent;
import earth.terrarium.adastra.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlagBlockEntity extends BlockEntity {

    @Nullable
    private GameProfile owner;

    @Nullable
    private FlagContent content;

    public FlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FLAG.get(), pos, state);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            ResolvableProfile resolvableProfile = ResolvableProfile.createResolved(owner);
            output.store("FlagOwner", ResolvableProfile.CODEC, resolvableProfile);
        }
        if (content != null) {
            CompoundTag contentTag = content.toFullTag();
            ValueOutput child = output.child("FlagContent");
            for (String key : contentTag.keySet()) {
                child.putString(key, contentTag.getStringOr(key, ""));
            }
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("FlagOwner", ResolvableProfile.CODEC)
            .ifPresent(profile -> setOwner(profile.partialProfile()));
        input.getString("FlagUrl")
            .ifPresent(url -> this.content = UrlContent.of("https://imgur.com/" + url));
        input.child("FlagContent").ifPresent(child -> {
            child.getString("type").ifPresent(type -> {
                child.getString("content").ifPresent(contentStr -> {
                    this.content = FlagContent.fromTypeAndContent(type, contentStr);
                });
            });
        });
    }

    @Nullable
    public GameProfile getOwner() {
        return this.owner;
    }

    public void setOwner(GameProfile profile) {
        synchronized (this) {
            this.owner = profile;
        }
        this.loadOwnerProperties();
    }

    private void loadOwnerProperties() {
        if (owner == null) return;
        // In 1.21.11, SkullBlockEntity.fetchGameProfile no longer exists.
        // Profile resolution is now handled through ResolvableProfile.resolveProfile().
        // For flags, we just keep the partial profile as-is since it has the name/UUID already.
        this.setChanged();
    }

    @Nullable
    public FlagContent getContent() {
        return this.content;
    }

    public void setContent(@Nullable FlagContent content) {
        this.content = content;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @SuppressWarnings("unused")
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).inflate(2);
    }
}
