package earth.terrarium.adastra.common.blockentities.flag.content;

import com.google.common.hash.HashCode;
import earth.terrarium.adastra.AdAstra;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface FlagContent {

    HashCode hash();

    String type();

    Tag toTag();

    default Identifier toTexture() {
        return Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "flagtextures/" + type() + "/" + hash());
    }

    default CompoundTag toFullTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type());
        tag.put("content", toTag());
        return tag;
    }

    static FlagContent fromTag(CompoundTag tag) {
        String type = tag.getStringOr("type", "");
        Tag content = tag.get("content");
        if (content == null) return null;
        return switch (type) {
            case ImageContent.TYPE -> ImageContent.of(content.asString().orElse(""));
            case UrlContent.TYPE -> UrlContent.of(content.asString().orElse(""));
            default -> null;
        };
    }

    @Nullable
    static FlagContent fromTypeAndContent(String type, String content) {
        return switch (type) {
            case ImageContent.TYPE -> ImageContent.of(content);
            case UrlContent.TYPE -> UrlContent.of(content);
            default -> null;
        };
    }
}
