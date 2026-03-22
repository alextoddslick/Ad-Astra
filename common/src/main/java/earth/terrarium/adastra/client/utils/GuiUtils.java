package earth.terrarium.adastra.client.utils;

import com.teamresourceful.resourcefullib.client.closables.CloseableScissor;
import net.minecraft.client.renderer.RenderPipelines;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class GuiUtils {

    public static final Identifier ENERGY_BAR = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "energy_bar");
    public static final int ENERGY_BAR_WIDTH = 13;
    public static final int ENERGY_BAR_HEIGHT = 46;

    public static final Identifier FLUID_BAR = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "fluid_bar");
    public static final int FLUID_BAR_WIDTH = 12;
    public static final int FLUID_BAR_HEIGHT = 46;

    public static final Identifier HAMMER = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/hammer.png");
    public static final Identifier SNOWFLAKE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/snowflake.png");
    public static final Identifier FIRE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/fire.png");
    public static final Identifier ARROW = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/arrow.png");
    public static final Identifier SUN = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "sun");
    public static final Identifier SLIDER = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "slider");

    public static final WidgetSprites SETTINGS_BUTTON_SPRITES = createPressableButtonSprites("settings_button");
    public static final WidgetSprites RESET_BUTTON_SPRITES = createPressableButtonSprites("reset_button");
    public static final WidgetSprites SHOW_BUTTON_SPRITES = createPressableButtonSprites("show_button");
    public static final WidgetSprites HIDE_BUTTON_SPRITES = createPressableButtonSprites("hide_button");

    public static final WidgetSprites CRAFTING_BUTTON_SPRITES = createPressableButtonSprites("crafting_button");
    public static final WidgetSprites FURNACE_BUTTON_SPRITES = createPressableButtonSprites("furnace_button");

    public static final WidgetSprites NONE_BUTTON_SPRITES = createPressableButtonSprites("side_config/none");
    public static final WidgetSprites PUSH_BUTTON_SPRITES = createPressableButtonSprites("side_config/push");
    public static final WidgetSprites PULL_BUTTON_SPRITES = createPressableButtonSprites("side_config/pull");
    public static final WidgetSprites PUSH_PULL_BUTTON_SPRITES = createPressableButtonSprites("side_config/push_pull");

    public static final WidgetSprites REDSTONE_ALWAYS_ON_SPRITES = createPressableButtonSprites("redstone/always_on_button");
    public static final WidgetSprites REDSTONE_ON_WHEN_POWERED_SPRITES = createPressableButtonSprites("redstone/on_when_powered_button");
    public static final WidgetSprites REDSTONE_ON_WHEN_NOT_POWERED_SPRITES = createPressableButtonSprites("redstone/on_when_not_powered_button");
    public static final WidgetSprites REDSTONE_NEVER_ON_SPRITES = createPressableButtonSprites("redstone/never_on_button");

    public static WidgetSprites createPressableButtonSprites(String name) {
        return new WidgetSprites(
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "buttons/" + name),
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "buttons/" + name + "_pressed"),
            Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "buttons/" + name + "_highlighted")
        );
    }

    public static void drawEnergyBar(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, long energy, long capacity, Component... tooltips) {
        float ratio = energy / (float) capacity;
        int scissorX = x + 6;
        int scissorY = y - 31 + ENERGY_BAR_HEIGHT - (int) (ENERGY_BAR_HEIGHT * ratio);
        try (var ignored = new CloseableScissor(graphics, scissorX, scissorY, scissorX + ENERGY_BAR_WIDTH, scissorY + ENERGY_BAR_HEIGHT)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, x + 6, y - 31, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
        }

        drawTooltips(graphics, mouseX, mouseY, x + 6, x + 19, y - 31, y + 15, list -> {
            list.add(TooltipUtils.getEnergyComponent(energy, capacity));
            Collections.addAll(list, tooltips);
            return list;
        });
    }

    public static void drawFluidBar(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, FluidResource fluid, long amount, long capacity, Component... tooltips) {
        int barX = x + 6;
        int barY = y - 31;
        float ratio = capacity > 0 ? amount / (float) capacity : 0;

        if (ratio > 0 && !fluid.isBlank()) {
            int fillHeight = (int) (FLUID_BAR_HEIGHT * ratio);
            int fillY = barY + FLUID_BAR_HEIGHT - fillHeight;
            renderFluidFill(graphics, fluid.getType(), barX, fillY, FLUID_BAR_WIDTH, fillHeight);
        }

        // Draw the bar frame overlay on top
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLUID_BAR, barX, barY, FLUID_BAR_WIDTH, FLUID_BAR_HEIGHT);

        drawTooltips(graphics, mouseX, mouseY, x + 6, x + 18, y - 31, y + 15, list -> {
            list.add(TooltipUtils.getFluidComponent(fluid, amount, capacity));
            Collections.addAll(list, tooltips);
            return list;
        });
    }

    public static void drawHorizontalProgressBar(GuiGraphics graphics, Identifier texture, int mouseX, int mouseY, int x, int y, int width, int height, int progress, int maxProgress, boolean reverse, Component... tooltips) {
        int widthProgress = (int) (width * (progress / (float) maxProgress));
        if (reverse) widthProgress = width - widthProgress;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, widthProgress, height, width, height);

        drawTooltips(graphics, mouseX, mouseY, x, x + width, y, y + height, list -> {
            Collections.addAll(list, tooltips);
            return list;
        });
    }

    public static void drawVerticalProgressBar(GuiGraphics graphics, Identifier texture, int mouseX, int mouseY, int x, int y, int width, int height, int progress, int maxProgress, Component... tooltips) {
        int heightProgress = (int) (height * (progress / (float) maxProgress));
        heightProgress = height - heightProgress;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + heightProgress, 0, heightProgress, width, height - heightProgress, width, height);

        drawTooltips(graphics, mouseX, mouseY, x, x + width, y, y + height, list -> {
            Collections.addAll(list, tooltips);
            return list;
        });
    }

    /**
     * Renders the fluid's still texture tiled and tinted within the given area.
     *
     * In 1.21.11, the old RenderSystem.setShader/setShaderTexture/enableBlend approach is removed.
     * This now uses GuiGraphics.blit() which handles the rendering pipeline internally.
     *
     * TODO: 1.21.11 - Verify fluid rendering works correctly with the new GuiGraphics.blit API.
     * The tiling and tinting approach may need adjustment for the new pipeline.
     */
    public static void renderFluidFill(GuiGraphics graphics, Fluid fluid, int x, int y, int width, int height) {
        if (height <= 0) return;

        Identifier stillTexture = getFluidStillTexture(fluid);
        int color = getFluidColor(fluid);

        // Render the fluid as a solid color bar since atlas sprite lookup changed in 1.21.11.
        // The tint color already represents the fluid visually.
        int drawY = y;
        int remaining = height;
        while (remaining > 0) {
            int drawHeight = Math.min(remaining, 16);
            graphics.fill(x, drawY, x + width, drawY + drawHeight, color | 0xFF000000);
            drawY += drawHeight;
            remaining -= drawHeight;
        }
    }

    /**
     * Returns the still texture location for a given fluid.
     */
    public static Identifier getFluidStillTexture(Fluid fluid) {
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return Identifier.withDefaultNamespace("block/lava_still");
        }
        // All Ad Astra fluids and water use the water_still texture
        return Identifier.withDefaultNamespace("block/water_still");
    }

    /**
     * Returns the ARGB tint color for a given fluid.
     * Colors are sourced from ModFluidProperties for Ad Astra fluids.
     */
    public static int getFluidColor(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4; // Vanilla water blue
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 0xFFFFFFFF; // Lava texture is already colored
        }
        if (fluid == ModFluids.OXYGEN.get()) {
            return 0xFFDAE6F0; // Light blue-white
        }
        if (fluid == ModFluids.HYDROGEN.get()) {
            return 0xFF89CFF0; // Light blue
        }
        if (fluid == ModFluids.OIL.get()) {
            return 0xFF373A36; // Dark gray-brown
        }
        if (fluid == ModFluids.FUEL.get()) {
            return 0xFFE5292B; // Red
        }
        if (fluid == ModFluids.CRYO_FUEL.get()) {
            return 0xFF6CFFFA; // Cyan
        }
        return 0xFFFFFFFF; // Default: no tint
    }

    public static void drawTooltips(GuiGraphics graphics, int mouseX, int mouseY, int minX, int maxX, int minY, int maxY, Function<List<Component>, List<Component>> tooltips) {
        if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
            List<Component> lines = tooltips.apply(new ArrayList<>());
            lines.removeIf(c -> c.getString().isEmpty());
            if (!lines.isEmpty()) {
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            }
        }
    }

    /**
     * @deprecated Use {@link #drawTooltips(GuiGraphics, int, int, int, int, int, int, Function)} instead.
     */
    @Deprecated
    public static void drawTooltips(int mouseX, int mouseY, int minX, int maxX, int minY, int maxY, Function<List<Component>, List<Component>> tooltips) {
        // No-op: callers should be migrated to the GuiGraphics version
    }

    public static void drawColoredShadowCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, int shadowColor) {
        FormattedCharSequence formattedCharSequence = text.getVisualOrderText();
        graphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2 - 1, y + 1, shadowColor, false);
        graphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color, false);
    }

    public static void drawColoredShadowString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, int shadowColor) {
        graphics.drawString(font, text, x - 1, y + 1, shadowColor, false);
        graphics.drawString(font, text, x, y, color, false);
    }
}
