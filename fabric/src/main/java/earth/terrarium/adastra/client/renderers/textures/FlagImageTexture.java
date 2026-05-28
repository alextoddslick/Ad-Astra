package earth.terrarium.adastra.client.renderers.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.blockentities.flag.FlagColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;

public class FlagImageTexture extends ReloadableTexture {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier DEFAULT_FLAG = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/block/flag/warning_flag.png");

    private final FlagColor[] colors;
    private boolean loaded;

    public FlagImageTexture(byte[] data) {
        super(DEFAULT_FLAG);
        this.colors = FlagColor.fromBytes(data);
    }

    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        // Load the image from color data and apply it
        Minecraft.getInstance().execute(() -> {
            NativeImage image = loadTexture(colors);
            if (image != null) {
                this.loaded = true;
                this.doLoad(image);
            }
        });

        // Return default texture contents as fallback
        return TextureContents.load(manager, DEFAULT_FLAG);
    }

    private NativeImage loadTexture(FlagColor[] colors) {
        try {
            NativeImage nativeImage = new NativeImage(22, 16, false);
            for (int x = 0; x < 22; x++) {
                for (int y = 0; y < 16; y++) {
                    FlagColor color = colors[x + y * 22];
                    if (color == FlagColor.NONE) {
                        nativeImage.setPixelABGR(x, y, 0x00000000);
                    } else {
                        nativeImage.setPixelABGR(x, y, color.color() | 0xFF000000);
                    }
                }
            }
            return nativeImage;
        } catch (Exception e) {
            LOGGER.warn("Failed to load texture: {}", this.resourceId(), e);
            return null;
        }
    }
}
