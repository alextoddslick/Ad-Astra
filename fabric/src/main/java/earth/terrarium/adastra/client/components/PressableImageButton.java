package earth.terrarium.adastra.client.components;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PressableImageButton extends ImageButton {

    private WidgetSprites sprites;

    public PressableImageButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
        this.sprites = sprites;
    }

    public PressableImageButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress, Component message) {
        super(x, y, width, height, sprites, onPress, message);
        this.setTooltip(Tooltip.create(message));
        this.sprites = sprites;
    }

    public void setSprites(WidgetSprites sprites) {
        this.sprites = sprites;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = sprites.get(isActive(), isHoveredOrFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), width, height);
    }
}
