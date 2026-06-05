package earth.terrarium.adastra.client.renderers.ti69.apps;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public interface Ti69App {

    int FULL_BRIGHT = 15728880;

    /**
     * Renders the contents of the app.
     *
     * @param pose        the pose stack (already positioned at the screen origin)
     * @param collector   the 26.1 submit-node collector that batches geometry/text for the frame
     * @param level       the client level
     * @param rightHanded whether the player is right-handed
     */
    void render(PoseStack pose, SubmitNodeCollector collector, ClientLevel level, boolean rightHanded);

    /**
     * @return the background color of the app
     */
    int color();

    /**
     * Renders an icon from an icon atlas as a textured quad via the submit collector.
     */
    default void renderIcon(PoseStack pose, SubmitNodeCollector collector, Identifier icon, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        float u0 = uOffset / (float) textureWidth;
        float u1 = (uOffset + uWidth) / (float) textureWidth;
        float v0 = vOffset / (float) textureHeight;
        float v1 = (vOffset + vHeight) / (float) textureHeight;
        collector.submitCustomGeometry(pose, RenderTypes.text(icon), (p, vertex) -> {
            vertex.addVertex(p.pose(), x, y + vHeight, 0.0F).setColor(255, 255, 255, 255).setUv(u0, v1).setUv2(240, 240);
            vertex.addVertex(p.pose(), x + uWidth, y + vHeight, 0.0F).setColor(255, 255, 255, 255).setUv(u1, v1).setUv2(240, 240);
            vertex.addVertex(p.pose(), x + uWidth, y, 0.0F).setColor(255, 255, 255, 255).setUv(u1, v0).setUv2(240, 240);
            vertex.addVertex(p.pose(), x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u0, v0).setUv2(240, 240);
        });
    }

    /**
     * Renders the in-game time as the TI-69 clock readout.
     */
    default void renderTime(PoseStack pose, SubmitNodeCollector collector, ClientLevel level) {
        double ratio = 1000.0 / 60.0;
        int dayTime = (int) ((level.getOverworldClockTime() + 6000L) % 12000L);
        boolean isPm = (int) ((level.getOverworldClockTime() + 6000L) % 24000L) >= 12000;
        int hours = dayTime / 1000 == 0 ? 12 : dayTime / 1000;
        int minutes = (int) ((dayTime % 1000) / ratio);
        String timeText = hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + (isPm ? " PM" : " AM");
        collector.submitText(pose, 0.0f, 5.0f, FormattedCharSequence.forward(timeText, Style.EMPTY), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, 0xFFFFFFFF, 0, 0);
    }
}
