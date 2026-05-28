package earth.terrarium.adastra.client.renderers.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.AdAstra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class FlagUrlTexture extends ReloadableTexture {

    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier DEFAULT_FLAG = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/block/flag/warning_flag.png");

    private final HttpRequest request;
    private boolean loaded;
    private CompletableFuture<?> loader;

    public FlagUrlTexture(String url) {
        super(DEFAULT_FLAG);
        this.request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Ad Astra (Minecraft Mod)")
            .build();
    }

    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        // Start async download if not already started
        if (this.loader == null) {
            this.loader = CompletableFuture.runAsync(() -> {
                try {
                    HttpResponse<InputStream> data = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (data.statusCode() / 100 == 2) {
                        NativeImage image = loadTexture(data.body());
                        if (image != null) {
                            Minecraft.getInstance().execute(() -> {
                                this.loaded = true;
                                this.doLoad(image);
                            });
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("Couldn't download http texture", e);
                }
            }, Util.backgroundExecutor());
        }

        // Return default texture contents while loading
        return TextureContents.load(manager, DEFAULT_FLAG);
    }

    @Nullable
    private NativeImage loadTexture(InputStream stream) {
        NativeImage nativeImage = null;

        try {
            nativeImage = NativeImage.read(stream);
        } catch (Exception var4) {
            LOGGER.warn("Error while loading the skin texture", var4);
        }

        return nativeImage;
    }
}
