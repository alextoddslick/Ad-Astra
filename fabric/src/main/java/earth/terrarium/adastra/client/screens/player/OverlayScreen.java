package earth.terrarium.adastra.client.screens.player;

import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.systems.PlanetData;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.utils.ClientData;
import earth.terrarium.adastra.client.utils.ClientStormData;
import earth.terrarium.adastra.common.network.packets.ClientboundSyncStormPacket;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;

public class OverlayScreen {

    public static final Identifier BATTERY_EMPTY = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "overlay/battery_empty");
    public static final Identifier BATTERY = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/overlay/battery.png");
    public static final Identifier OXYGEN_TANK_EMPTY = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "overlay/oxygen_tank_empty");
    public static final Identifier OXYGEN_TANK = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/gui/sprites/overlay/oxygen_tank.png");
    public static final Identifier ROCKET_BAR = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "overlay/rocket_bar");
    public static final Identifier ROCKET = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "overlay/rocket");

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;
        var level = player.level();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getDebugOverlay().showDebugScreen()) return;
        var font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Matrix3x2fStack poseStack = graphics.pose();

        // Rocket overlay
        if (player.getVehicle() instanceof Rocket rocket) {
            int countdown = Mth.ceil(rocket.launchTicks() / 20f);
            if (rocket.isLaunching()) {
                poseStack.pushMatrix();
                poseStack.translate(width / 2f, height / 2f);
                poseStack.scale(4, 4);
                graphics.centeredText(font, String.valueOf(countdown), 0, -10, 0xFFe53253);
                poseStack.popMatrix();
            }

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROCKET_BAR, 0, height / 2, 16, 128);

            poseStack.pushMatrix();
            double y = Mth.clamp(rocket.getY(), 100, AdAstraConfig.atmosphereLeave);
            poseStack.translate(0.3f, (float) ((AdAstraConfig.atmosphereLeave - y - 500) / 4.5));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROCKET, 3, height / 2 + 113, 8, 11);
            poseStack.popMatrix();
        }

        // Oxygen overlay
        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (SpaceSuitItem.hasFullSet(player) && chestStack.getItem() instanceof SpaceSuitItem spaceSuit) {
            long amount = SpaceSuitItem.getOxygenAmount(player);
            var fc = spaceSuit.getFluidContainer(chestStack);
            long capacity = fc.get(0).getLimit(fc.get(0).getResource());
            double ratio = (double) amount / capacity;
            int barHeight = (int) (ratio * 52);

            int x = AdAstraConfigClient.oxygenBarX;
            int y = AdAstraConfigClient.oxygenBarY;
            float scale = AdAstraConfigClient.oxygenBarScale;

            poseStack.pushMatrix();
            poseStack.scale(scale, scale);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OXYGEN_TANK_EMPTY, x, y, 62, 52);
            graphics.blit(RenderPipelines.GUI_TEXTURED, OXYGEN_TANK, x, y + 52 - barHeight, 0, 52 - barHeight, 62, barHeight, 62, 52);

            var text = String.format("%.1f%%", ratio * 100);
            int textWidth = font.width(text);
            int color = ratio <= 0 ? 0xFFDC143C : 0xFFFFFFFF;
            PlanetData localData = ClientData.getLocalData();
            if (localData != null && localData.oxygen()) {
                color = 0xFF55ff55;
            }
            graphics.text(font, text, (int) (x + (62 - textWidth) / 2f), y + 52 + 3, color);
            poseStack.popMatrix();
        }

        // Battery overlay
        if (JetSuitItem.hasFullSet(player) && chestStack.getItem() instanceof JetSuitItem jetSuit) {
            long amount = jetSuit.getEnergyStorage(chestStack).getStoredAmount();
            long capacity = jetSuit.getEnergyStorage(chestStack).getCapacity();
            double ratio = (double) amount / capacity;
            int barWidth = (int) (ratio * 49);

            int x = AdAstraConfigClient.energyBarX;
            int y = AdAstraConfigClient.energyBarY;
            float scale = AdAstraConfigClient.energyBarScale;

            poseStack.pushMatrix();
            poseStack.scale(scale, scale);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BATTERY_EMPTY, x, y, 49, 27);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BATTERY, x, y, 0, 27, barWidth, 27, 49, 27);

            var text = String.format("%.1f%%", ratio * 100);
            int textWidth = font.width(text);
            int color = ratio <= 0 ? 0xFFDC143C : 0xFF55ffff;
            graphics.text(font, text, (int) (x + (49 - textWidth) / 2f), y + 27 + 3, color);
            poseStack.popMatrix();
        }

        if (player.getVehicle() instanceof Lander lander && level.getBlockState(lander.getOnPos().below(2)).isAir()) {
            int ground = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, lander.blockPosition()).getY();
            int distance = Math.max(0, lander.blockPosition().getY() - ground);

            poseStack.pushMatrix();
            poseStack.translate(width / 2f, height / 2f);
            poseStack.scale(1.4f, 1.4f);

            float alpha = Mth.clamp(0.1f - (float) (lander.getDeltaMovement().y() + 0.5), 0, 1);
            int textAlpha = (int) (alpha * 255) << 24;
            int textColor = 0xe53253 | textAlpha;
            graphics.centeredText(font,
                Component.translatable("message.ad_astra.lander.onboard", minecraft.options.keyJump.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT)),
                0, 60, textColor);

            int distanceColor = 0xFF55ff55;
            if (distance < 100) {
                distanceColor = 0xFFff5555;
            } else if (distance < 300) {
                distanceColor = 0xFFffff55;
            }
            graphics.centeredText(font,
                String.valueOf(distance),
                0, 30, distanceColor);

            poseStack.popMatrix();
        }

        // Storm overlay (Jupiter et al.). ClientStormData is gated to the player's current
        // dimension, so it only has data on storm planets.
        byte stormPhase = ClientStormData.phase();
        boolean storming = ClientStormData.isStorming();
        if (storming || stormPhase != ClientboundSyncStormPacket.PHASE_NONE) {
            long time = level.getGameTime();

            // Persistent status line at the top while a storm is active.
            if (storming) {
                int pct = Mth.clamp(Math.round(ClientStormData.intensity() * 100), 0, 100);
                poseStack.pushMatrix();
                poseStack.translate(width / 2f, 4);
                graphics.centeredText(font, Component.translatable("hud.ad_astra.storm", pct), 0, 0, 0xFFff5555);
                poseStack.popMatrix();
            }

            // Pulsing transition alert ("storm approaching" / "skies clearing").
            if (stormPhase != ClientboundSyncStormPacket.PHASE_NONE) {
                float pulse = Mth.clamp(0.55f + 0.45f * (float) Math.sin(time * 0.2), 0, 1);
                int alpha = (int) (pulse * 255) << 24;
                boolean approaching = stormPhase == ClientboundSyncStormPacket.PHASE_APPROACHING;
                Component alert = Component.translatable(approaching
                    ? "message.ad_astra.storm.approaching"
                    : "message.ad_astra.storm.clearing");
                int rgb = approaching ? 0xffaa00 : 0x55ff55;

                poseStack.pushMatrix();
                poseStack.translate(width / 2f, height / 4f);
                poseStack.scale(1.5f, 1.5f);
                graphics.centeredText(font, alert, 0, 0, rgb | alpha);
                poseStack.popMatrix();
            }
        }
    }
}
