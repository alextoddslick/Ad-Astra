package earth.terrarium.adastra.client.renderers.ti69.apps;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public interface Ti69App {

    /**
     * Renders the contents of the app
     *
     * @param pose         The pose stack
     * @param bufferSource The buffer source
     * @param matrix4f     The matrix
     * @param font         The font
     * @param level        The client level
     * @param rightHanded  Whether the player is right-handed
     */
    void render(PoseStack pose, MultiBufferSource bufferSource, Matrix4f matrix4f, Font font, ClientLevel level, boolean rightHanded);

    /**
     * @return The background color of the app
     */
    int color();

    /**
     * Renders an icon.
     *
     * TODO: 1.21.11 - RenderSystem.setShaderTexture() and BufferUploader.drawWithShader() are removed.
     * This method needs to be reimplemented using the new rendering pipeline (e.g., using
     * MultiBufferSource to get a VertexConsumer for the appropriate RenderType, or using
     * a RenderPass with a custom RenderPipeline). For now, this method is a no-op stub.
     */
    default void renderIcon(Matrix4f matrix4f, Identifier icon, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        // Stubbed - RenderSystem.setShaderTexture() and BufferUploader.drawWithShader() are removed in 1.21.11
        // The icon rendering needs to use the new pipeline (MultiBufferSource + RenderType or RenderPass)
    }

    /**
     * Renders the time
     *
     * @param bufferSource The buffer source
     * @param matrix4f     The matrix
     * @param font         The font
     * @param level        The client level
     */
    default void renderTime(MultiBufferSource bufferSource, Matrix4f matrix4f, Font font, ClientLevel level) {
        double ratio = 1000.0 / 60.0;
        int dayTime = (int) ((level.getOverworldClockTime() + 6000L) % 12000L);
        boolean isPm = (int) ((level.getOverworldClockTime() + 6000L) % 24000L) >= 12000;
        int hours = dayTime / 1000 == 0 ? 12 : dayTime / 1000;
        int minutes = (int) ((dayTime % 1000) / ratio);
        String timeText = hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + (isPm ? " PM" : " AM");
        font.drawInBatch(timeText, 0.0f, 5.0f, 0xFFFFFF, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0xFFFFFF, 15728880);
    }
}
