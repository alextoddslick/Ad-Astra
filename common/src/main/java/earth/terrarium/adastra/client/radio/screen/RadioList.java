package earth.terrarium.adastra.client.radio.screen;

import com.google.common.collect.Sets;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.config.RadioConfig;
import earth.terrarium.adastra.client.radio.audio.RadioHandler;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundSetStationPacket;
import earth.terrarium.adastra.common.utils.radio.StationInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class RadioList extends AbstractWidget {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "textures/radio/ui.png");

    private List<StationInfo> stations = new ArrayList<>();
    private String playing = null;
    private final List<RadioEntry> entries = new ArrayList<>();
    private final int itemHeight;
    private final Consumer<RadioEntry> onSelection;
    private final boolean relativeClicks;
    private RadioEntry selected;
    private double scrollAmount;

    @Nullable
    private final BlockPos pos;

    public RadioList(int x, int y, @Nullable BlockPos pos) {
        super(x, y, 91, 41, Component.empty());
        this.itemHeight = 12;
        this.pos = pos;
        this.relativeClicks = true;
        this.onSelection = entry -> {
            if (entry != null) {
                StationInfo info = entry.info;
                if (info == null) {
                    NetworkHandler.CHANNEL.sendToServer(new ServerboundSetStationPacket("", pos));
                } else if (!Objects.equals(RadioHandler.getPlaying(), info.url())) {
                    NetworkHandler.CHANNEL.sendToServer(new ServerboundSetStationPacket(info.url(), pos));
                }
            }
        };
    }

    public void update(List<StationInfo> stations, String url) {
        this.stations = stations;
        this.playing = url;
        update();
    }

    private void update() {
        RadioEntry selectedEntry = null;
        List<RadioEntry> newEntries = new ArrayList<>();
        Set<String> favoriteUrls = Sets.newHashSet(RadioConfig.favorites);
        List<StationInfo> favorites = new ArrayList<>();
        List<StationInfo> others = new ArrayList<>();

        for (StationInfo station : this.stations) {
            if (favoriteUrls.contains(station.url())) {
                favorites.add(station);
            } else {
                others.add(station);
            }
        }
        favorites.sort(Comparator.comparing(StationInfo::title));
        others.sort(Comparator.comparing(StationInfo::title));

        newEntries.add(new RadioEntry(null));

        for (StationInfo favorite : favorites) {
            RadioEntry entry = new RadioEntry(favorite);
            if (favorite.url().equals(this.playing)) selectedEntry = entry;
            entry.favorite = true;
            newEntries.add(entry);
        }

        for (StationInfo other : others) {
            RadioEntry entry = new RadioEntry(other);
            if (other.url().equals(this.playing)) selectedEntry = entry;
            newEntries.add(entry);
        }

        this.entries.clear();
        this.entries.addAll(newEntries);
        if (selectedEntry != null) {
            this.selected = selectedEntry;
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        int scrollOffset = (int) scrollAmount;
        for (int i = 0; i < entries.size(); i++) {
            RadioEntry entry = entries.get(i);
            int entryTop = getY() + i * itemHeight - scrollOffset;
            int entryBottom = entryTop + itemHeight;

            if (entryBottom < getY() || entryTop > getY() + getHeight()) continue;

            boolean hovered = mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= entryTop && mouseY < entryTop + itemHeight;
            boolean isSelected = entry == selected;

            entry.render(graphics, i, getX(), entryTop, getWidth(), itemHeight, mouseX - getX(), mouseY - entryTop, hovered, partialTick, isSelected);
        }

        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean bl) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!isMouseOver(mouseX, mouseY)) return false;

        int scrollOffset = (int) scrollAmount;
        for (int i = 0; i < entries.size(); i++) {
            RadioEntry entry = entries.get(i);
            int entryTop = getY() + i * itemHeight - scrollOffset;

            if (mouseY >= entryTop && mouseY < entryTop + itemHeight) {
                double relX = relativeClicks ? mouseX - getX() : mouseX;
                double relY = relativeClicks ? mouseY - entryTop : mouseY;
                if (entry.mouseClicked(relX, relY, event.button())) {
                    return true;
                }
                selected = entry;
                onSelection.accept(entry);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int maxScroll = Math.max(0, entries.size() * itemHeight - getHeight());
        scrollAmount = Mth.clamp(scrollAmount - scrollY * itemHeight / 2.0, 0, maxScroll);
        return true;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }

    public void setSelected(RadioEntry entry) {
        this.selected = entry;
    }

    public class RadioEntry {

        @Nullable
        private final StationInfo info;
        private boolean favorite = false;

        public RadioEntry(@Nullable StationInfo info) {
            this.info = info;
        }

        protected void render(@NotNull GuiGraphics graphics, int id, int left, int top, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, boolean selected) {
            int v = selected ? 66 : hovered ? 42 : 54;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left + 1, top, 253, v, 89, 12, 512, 256);
            int textStart = left + 3;
            if ((favorite || hovered) && this.info != null) {
                String text = favorite ? "\u2605" : "\u2606";
                graphics.drawString(Minecraft.getInstance().font, text, textStart, top + 3, favorite ? 0xFFFFAA00 : 0xFFFFFFFF);
                textStart += Minecraft.getInstance().font.width(text) + 2;
            }
            graphics.drawString(Minecraft.getInstance().font, getName(), textStart, top + 3, 0xFFFFFFFF);

            if (hovered) {
                if (info != null) {
                    graphics.setTooltipForNextFrame(Component.literal(info.name()), mouseX + left, mouseY + top);
                }
                CursorScreen.Cursor.POINTER.apply(graphics);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int width = Minecraft.getInstance().font.width(favorite ? "\u2605" : "\u2606");
            if (info != null && mouseX > 0 && mouseX < width + 3 && mouseY > 0 && mouseY < 12) {
                List<String> favorites = new ArrayList<>(Arrays.asList(RadioConfig.favorites));
                if (favorite) {
                    favorites.remove(info.url());
                } else {
                    favorites.add(info.url());
                }
                RadioConfig.favorites = favorites.toArray(new String[0]);
                AdAstra.CONFIGURATOR.saveConfig(AdAstraConfigClient.class);
                update();
                return true;
            }
            return false;
        }

        private Component getName() {
            return info == null ? Component.translatable("text.ad_astra.radio.none") : Component.literal(info.title());
        }
    }
}
