package earth.terrarium.adastra.client.screens;

import com.mojang.datafixers.util.Pair;
import com.teamresourceful.resourcefullib.client.closables.CloseableScissor;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.client.events.AdAstraClientEvents;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.client.components.LabeledImageButton;
import earth.terrarium.adastra.client.utils.DimensionRenderingUtils;
import earth.terrarium.adastra.common.compat.cadmus.CadmusIntegration;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.constants.PlanetConstants;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.handlers.base.SpaceStation;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundLandOnSpaceStationPacket;
import earth.terrarium.adastra.common.network.packets.ServerboundLandPacket;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetsScreen extends AbstractContainerScreen<PlanetsMenu> {

    public static final Identifier SELECTION_MENU = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/selection_menu");
    public static final Identifier SMALL_SELECTION_MENU = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/small_selection_menu");

    public static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/button"),
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/button_highlighted")
    );

    public static final WidgetSprites BACK_BUTTON_SPRITES = new WidgetSprites(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/back_button"),
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/back_button_highlighted")
    );

    public static final WidgetSprites PLUS_BUTTON_SPRITES = new WidgetSprites(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/plus_button"),
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/plus_button_highlighted")
    );

    public static final WidgetSprites MOONS_BUTTON_SPRITES = new WidgetSprites(
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/moons_button"),
        Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "planets/moons_button_highlighted")
    );

    private final List<Button> buttons = new ArrayList<>();
    private Button backButton;
    private double scrollAmount;

    private final List<Button> spaceStationButtons = new ArrayList<>();
    private Button addSpaceStatonButton;
    private double spaceStationScrollAmount;

    @Nullable
    private Button landButton;

    private final boolean hasMultipleSolarSystems;
    private int pageIndex;
    @Nullable
    private Identifier selectedSolarSystem = PlanetConstants.SOLAR_SYSTEM;

    @Nullable
    private Planet selectedPlanet;

    // When non-null, the left list shows the moons of this parent body instead
    // of the solar system's planets (entered via the Moons side button).
    @Nullable
    private Planet moonsParent;
    // Inline "view moons" buttons keyed by their planet-row button (only for
    // planets that have moons, when not already in the moons sub-list).
    private final Map<Button, Button> rowMoonsButtons = new HashMap<>();

    public PlanetsScreen(PlanetsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 0, 0);

        var planets = AdAstraData.planets().values().stream()
            .filter(planet -> !menu.disabledPlanets().contains(planet.dimension().identifier()))
            .filter(planet -> menu.tier() >= planet.tier()).toList();
        hasMultipleSolarSystems = planets.stream().map(Planet::solarSystem).distinct().count() > 1;
        pageIndex = hasMultipleSolarSystems ? 0 : 1;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        rowMoonsButtons.clear();
        spaceStationButtons.clear();
        spaceStationScrollAmount = 0;
        landButton = null;

        switch (pageIndex) {
            case 0 -> createSolarSystemButtons();
            case 1, 2 -> {
                createPlanetButtons();
                if (pageIndex == 2 && selectedPlanet != null) {
                    createSelectedPlanetButtons();
                }
            }
        }

        backButton = addRenderableWidget(new LabeledImageButton(10, height / 2 - 85, 12, 12, BACK_BUTTON_SPRITES, b -> goBack()));

        addSpaceStatonButton = addRenderableWidget(new LabeledImageButton(114, height / 2 - 41, 12, 12, PLUS_BUTTON_SPRITES, b -> {
            if (selectedPlanet == null) return;
            ResourceKey<Level> orbit = selectedPlanet.orbitIfPresent();
            // If a station the player can access already exists at this chunk
            // (within 2 chunks), the "Construct" button can't fire — but rather
            // than disabling outright we route the click to LAND on that station.
            var accessible = menu.getAccessibleStationNearPlayer(orbit);
            if (accessible != null) {
                landOnSpaceStation(orbit, accessible.position());
                return;
            }
            int ownedSpaceStationCount = menu.getOwnedAndTeamSpaceStations(orbit).size();
            Component name = Component.translatable("text.ad_astra.text.space_station_name", ownedSpaceStationCount + 1);
            menu.constructSpaceStation(selectedPlanet.dimension(), name);
            close();
        }));
        if (selectedPlanet != null) {
            ResourceKey<Level> orbit = selectedPlanet.orbitIfPresent();
            var accessible = menu.getAccessibleStationNearPlayer(orbit);
            if (accessible != null) {
                // Owned (or team) station exists in range: button lands on it.
                addSpaceStatonButton.setTooltip(getExistingStationLandTooltip(orbit, accessible));
                addSpaceStatonButton.active = true;
            } else {
                addSpaceStatonButton.setTooltip(getSpaceStationRecipeTooltip(orbit));
                // Only block construct when something is in the way that the
                // player can't land on. isInSpaceStation() reports true for any
                // owner; if it's not theirs the button stays disabled.
                addSpaceStatonButton.active = menu.canConstruct(orbit) && !menu.isInSpaceStation(orbit);
            }
        }

        backButton.visible = moonsParent != null || pageIndex > (hasMultipleSolarSystems ? 0 : 1);
        addSpaceStatonButton.visible = pageIndex == 2 && selectedPlanet != null;
    }

    private void createSolarSystemButtons() {
        selectedSolarSystem = null;

        List<Identifier> solarSystems = new ArrayList<>(AdAstraData.solarSystems());
        solarSystems.sort(Comparator.comparing(Identifier::getPath));
        solarSystems.forEach(solarSystem -> {
            var button = addWidget(new LabeledImageButton(10, 0, 99, 20, BUTTON_SPRITES, b -> {
                pageIndex = 1;
                selectedSolarSystem = solarSystem;
                rebuildWidgets();
            }, Component.translatableWithFallback("solar_system.%s.%s".formatted(solarSystem.getNamespace(), solarSystem.getPath()), title(solarSystem.getPath()))));
            buttons.add(button);
        });
    }

    private void createPlanetButtons() {
        // When a parent is selected via the Moons button, list its moons instead
        // of the solar system's planets.
        List<Planet> source = moonsParent != null
            ? AdAstraData.moonsOf(moonsParent.dimension())
            : menu.getSortedPlanets();
        for (var planet : source) {
            if (CadmusIntegration.cadmusLoaded()) {
                CadmusIntegration.addClientListeners(planet.dimension());
            }
            if (planet.isSpace()) continue;
            if (menu.tier() < planet.tier()) continue;
            if (menu.disabledPlanets().contains(planet.dimension().identifier())) continue;
            if (moonsParent == null) {
                // Main list: hide moons (they live under their parent) and keep the
                // current solar system only.
                if (planet.moonOf().isPresent()) continue;
                if (!planet.solarSystem().equals(selectedSolarSystem)) continue;
            }
            final var fp = planet;
            // Inline "view moons" button on the right of the row, for planets that
            // have moons (and only in the main list, not when already viewing moons).
            // Added BEFORE the planet button so it wins clicks on its 16px region.
            Button moonsBtn = null;
            if (moonsParent == null && AdAstraData.hasMoons(planet.dimension())) {
                moonsBtn = addWidget(new LabeledImageButton(95, 0, 12, 12, MOONS_BUTTON_SPRITES, b -> {
                    moonsParent = fp;
                    this.scrollAmount = 0;
                    rebuildWidgets();
                }));
                moonsBtn.setTooltip(Tooltip.create(Component.translatable("tooltip.ad_astra.view_moons",
                    menu.getPlanetName(fp.dimension())).withStyle(ChatFormatting.AQUA)));
            }
            Button planetBtn = addWidget(new LabeledImageButton(10, 0, 99, 20, BUTTON_SPRITES, b -> {
                AdAstra.LOGGER.info("[ad_astra] Planet button pressed: {}", fp.dimension().identifier());
                pageIndex = 2;
                selectedPlanet = fp;
                rebuildWidgets();
            }, menu.getPlanetName(planet.dimension())));
            buttons.add(planetBtn);
            if (moonsBtn != null) rowMoonsButtons.put(planetBtn, moonsBtn);
        }
    }

    private void createSelectedPlanetButtons() {
        if (selectedPlanet == null) return;
        BlockPos pos = menu.getLandingPos(selectedPlanet.dimension(), true);
        AdAstra.LOGGER.info("[ad_astra] PlanetsScreen.createSelectedPlanetButtons() Land button @ ({},{}) for dim={}",
            114, height / 2 - 77, selectedPlanet.dimension().identifier());
        landButton = addRenderableWidget(new LabeledImageButton(
            114, height / 2 - 77, 99, 20, BUTTON_SPRITES,
            b -> {
                AdAstra.LOGGER.info("[ad_astra] LAND button pressed, selectedPlanet={}", selectedPlanet == null ? "null" : selectedPlanet.dimension().identifier());
                land(selectedPlanet.dimension());
            }, ConstantComponents.LAND));
        landButton.setTooltip(Tooltip.create(Component.translatable("tooltip.ad_astra.land",
            menu.getPlanetName(selectedPlanet.dimension()), pos.getX(), pos.getZ()).withStyle(ChatFormatting.AQUA)));

        addSpaceStationButtons(selectedPlanet.orbitIfPresent());
    }

    /**
     * Back navigation that understands the moons sub-view. From a moon's page it
     * returns to the moons list; from the moons list it returns to the parent
     * planet's page; otherwise it walks the normal page stack.
     */
    private void goBack() {
        if (moonsParent != null) {
            if (pageIndex == 2) {
                // viewing a specific moon -> back to the moons list
                selectedPlanet = null;
                this.scrollAmount = 0;
                pageIndex = 1;
            } else {
                // viewing the moons list -> back to the planet list (page stays 1)
                moonsParent = null;
                this.scrollAmount = 0;
            }
            rebuildWidgets();
            return;
        }
        if (pageIndex != 2) this.scrollAmount = 0;
        pageIndex--;
        rebuildWidgets();
    }

    private void addSpaceStationButtons(ResourceKey<Level> dimension) {
        // List all stations in the dim, not just owned/team. Dev/single-player sessions
        // often have stations recorded under a UUID that no longer matches the current
        // dev player (Loom regenerates it per-run). Server-side packet still enforces
        // ownership in multiplayer; in single-player it accepts any landing.
        menu.getAllSpaceStations(dimension).forEach(station -> {
            ChunkPos pos = station.getSecond().position();
            var button = addWidget(new LabeledImageButton(114, height / 2, 99, 20, BUTTON_SPRITES, b ->
                landOnSpaceStation(dimension, pos), station.getSecond().name()));
            button.setTooltip(getSpaceStationLandTooltip(dimension, pos, station.getFirst()));
            spaceStationButtons.add(button);
        });
    }

    public Tooltip getSpaceStationLandTooltip(ResourceKey<Level> dimension, ChunkPos pos, String owner) {
        return Tooltip.create(CommonComponents.joinLines(
            Component.translatable("tooltip.ad_astra.space_station_land", menu.getPlanetName(dimension), pos.getMiddleBlockX(), pos.getMiddleBlockZ()).withStyle(ChatFormatting.AQUA),
            Component.translatable("tooltip.ad_astra.space_station_owner", owner).withStyle(ChatFormatting.GOLD)
        ));
    }

    /**
     * Tooltip shown on the "+" button when it has been retargeted to land on a
     * station the player already owns at this location.
     */
    public Tooltip getExistingStationLandTooltip(ResourceKey<Level> dimension, SpaceStation station) {
        ChunkPos pos = station.position();
        return Tooltip.create(CommonComponents.joinLines(
            ConstantComponents.SPACE_STATION_ALREADY_EXISTS,
            Component.translatable("tooltip.ad_astra.space_station_land", menu.getPlanetName(dimension), pos.getMiddleBlockX(), pos.getMiddleBlockZ()).withStyle(ChatFormatting.AQUA)
        ));
    }

    public Tooltip getSpaceStationRecipeTooltip(ResourceKey<Level> planet) {
        List<Component> tooltip = new ArrayList<>();
        BlockPos pos = menu.getLandingPos(planet, false);
        tooltip.add(Component.translatable("tooltip.ad_astra.construct_space_station_at", menu.getPlanetName(planet), pos.getX(), pos.getZ()).withStyle(ChatFormatting.AQUA));

        if (menu.isInSpaceStation(planet) || menu.isClaimed(planet)) {
            tooltip.add(ConstantComponents.SPACE_STATION_ALREADY_EXISTS);
            // Server-side check (SpaceStationHandler.isInSpaceStation) rejects construct
            // for ANY existing station at this chunk regardless of owner. If we reached
            // this branch and the player has no accessible station here, it means
            // someone else's station is in the way — make that explicit, and point them
            // at the Land button on the right panel for their own stations.
            if (menu.getAccessibleStationNearPlayer(planet) == null) {
                tooltip.add(Component.literal("Owned by another player — use the Land button for your own stations, or pick a different chunk.")
                    .withStyle(ChatFormatting.YELLOW));
            }
            return Tooltip.create(CommonComponents.joinLines(tooltip));
        } else {
            tooltip.add(ConstantComponents.CONSTRUCTION_COST.copy().withStyle(ChatFormatting.AQUA));
        }

        List<Pair<ItemStack, Integer>> ingredients = menu.ingredients().get(planet);
        if (ingredients == null) return Tooltip.create(CommonComponents.joinLines(tooltip));
        for (var ingredient : ingredients) {
            var stack = ingredient.getFirst();
            int amountOwned = ingredient.getSecond();
            boolean hasEnough = menu.player().isCreative() || menu.player().isSpectator() || amountOwned >= stack.getCount();
            tooltip.add(Component.translatable("tooltip.ad_astra.requirement", amountOwned, stack.getCount(), stack.getHoverName()
                    .copy().withStyle(ChatFormatting.DARK_AQUA))
                .copy().withStyle(hasEnough ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        return Tooltip.create(CommonComponents.joinLines(tooltip));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        renderButtons(graphics, mouseX, mouseY, partialTick);
        backButton.visible = moonsParent != null || pageIndex > (hasMultipleSolarSystems ? 0 : 1);
        addSpaceStatonButton.visible = pageIndex == 2 && selectedPlanet != null;

        // Prevent buttons from being pressed when outside view area.
        buttons.forEach(button -> {
            boolean inView = button.getY() > height / 2 - 63 && button.getY() < height / 2 + 88;
            button.active = inView;
            Button moonsBtn = rowMoonsButtons.get(button);
            if (moonsBtn != null) moonsBtn.active = inView;
        });
        // Also activate space station buttons within view area.
        spaceStationButtons.forEach(button -> button.active = button.getY() > height / 2 - 22 && button.getY() < height / 2 + 88);

        // In 26.1.x, AbstractContainerScreen.extractContents is the path that draws
        // addRenderableWidget()-registered widgets (via super.extractRenderState -> Screen).
        // We override extractContents to a no-op (to suppress vanilla label/slot drawing),
        // so we must explicitly render our renderable widgets here.
        if (backButton.visible) {
            backButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (addSpaceStatonButton.visible) {
            addSpaceStatonButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (landButton != null && pageIndex == 2 && selectedPlanet != null) {
            landButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int scrollPixels = (int) scrollAmount;

        try (var ignored = new CloseableScissor(graphics, 0, height / 2 - 43, 112, height / 2 - 43 + 131)) {
            // Position FIRST, then render. Otherwise the first frame draws every
            // button at its init() y and they overlap until the next frame.
            for (int i = 0; i < buttons.size(); i++) {
                var button = buttons.get(i);
                button.setY((i * 24 - scrollPixels) + (height / 2 - 41));
                Button moonsBtn = rowMoonsButtons.get(button);
                if (moonsBtn != null) {
                    moonsBtn.setX(95);
                    moonsBtn.setY(button.getY() + 4);
                }
            }
            for (var button : buttons) {
                button.extractRenderState(graphics, mouseX, mouseY, partialTick);
                Button moonsBtn = rowMoonsButtons.get(button);
                if (moonsBtn != null) {
                    moonsBtn.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }
            }
        }

        if (pageIndex == 2 && selectedPlanet != null) {
            int spaceStationScrollPixels = (int) spaceStationScrollAmount;

            try (var ignored = new CloseableScissor(graphics, 112, height / 2 - 2, 224, height / 2 + 88)) {
                // Same fix here: lay them out before drawing so existing-station
                // entries don't all stack at height/2 on the opening frame.
                for (int i = 0; i < spaceStationButtons.size(); i++) {
                    var button = spaceStationButtons.get(i);
                    button.setY((i * 24 - spaceStationScrollPixels) + (height / 2));
                }
                for (var button : spaceStationButtons) {
                    button.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xff000419);

        // Draw subtle diamond grid pattern
        int gridSize = 30;
        int gridColor = 0x20ffffff;
        for (int x = 0; x < width; x += gridSize) {
            for (int y = 0; y < height; y += gridSize) {
                graphics.fill(x, y, x + 1, y + 1, gridColor);
            }
        }

        AdAstraClientEvents.RenderSolarSystemEvent.fire(graphics, selectedSolarSystem, width, height);
        renderSelectionMenu(graphics);
    }

    protected void renderSelectionMenu(GuiGraphicsExtractor graphics) {
        if (pageIndex == 2) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_MENU, 7, height / 2 - 88, 209, 177);
            graphics.centeredText(font, ConstantComponents.SPACE_STATION, 163, height / 2 - 15, 0xFFffffff);
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SMALL_SELECTION_MENU, 7, height / 2 - 88, 105, 177);
        }

        if (pageIndex == 2 && selectedPlanet != null) {
            var title = Component.translatableWithFallback("planet.%s.%s".formatted(selectedPlanet.dimension().identifier().getNamespace(), selectedPlanet.dimension().identifier().getPath()), title(selectedPlanet.dimension().identifier().getPath()));
            graphics.centeredText(font, title, 57, height / 2 - 60, 0xFFffffff);
        } else if (pageIndex == 1 && moonsParent != null) {
            var title = Component.translatable("text.ad_astra.text.moons_of", menu.getPlanetName(moonsParent.dimension()));
            graphics.centeredText(font, title, 57, height / 2 - 60, 0xFFffffff);
        } else if (pageIndex == 1 && selectedSolarSystem != null) {
            var title = Component.translatableWithFallback("solar_system.%s.%s".formatted(selectedSolarSystem.getNamespace(), selectedSolarSystem.getPath()), title(selectedSolarSystem.getPath()));
            graphics.centeredText(font, title, 57, height / 2 - 60, 0xFFffffff);
        } else {
            graphics.centeredText(font, ConstantComponents.CATALOG, 57, height / 2 - 60, 0xFFffffff);
        }
    }

    public static void drawCircles(GuiGraphicsExtractor graphics, int start, int count, int color, int width, int height) {
        for (int i = 1 + start; i < count + start + 1; i++) {
            drawCircle(graphics, width / 2f, height / 2f, 30 * i, 120, color);
        }
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, double x, double y, double radius, int sides, int color) {
        for (int i = 0; i < sides; i++) {
            double angle = i * 2.0 * Math.PI / sides;
            int px = (int) Math.round(x + radius * Math.cos(angle));
            int py = (int) Math.round(y + radius * Math.sin(angle));
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        AdAstra.LOGGER.info("[ad_astra] PlanetsScreen.mouseClicked btn={} pos=({},{}) handled={} page={} screen=({}x{}) planetButtons={} stationButtons={} backVisible={} addStationVisible={}",
            event.button(), event.x(), event.y(), handled, pageIndex, width, height, buttons.size(), spaceStationButtons.size(),
            backButton != null && backButton.visible, addSpaceStatonButton != null && addSpaceStatonButton.visible);
        // Diagnose: always dump children on page 2 so we can verify Land button placement
        if (pageIndex == 2) {
            for (var child : children()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget aw) {
                    boolean over = aw.isMouseOver(event.x(), event.y());
                    String msg = aw.getMessage() == null ? "" : aw.getMessage().getString();
                    AdAstra.LOGGER.info("[ad_astra]   child '{}' {} bounds=({}..{}, {}..{}) visible={} active={} over={}",
                        msg, aw.getClass().getSimpleName(),
                        aw.getX(), aw.getX() + aw.getWidth(), aw.getY(), aw.getY() + aw.getHeight(),
                        aw.visible, aw.active, over);
                }
            }
        }
        return handled;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < 112 && mouseX > 6 && mouseY > height / 2f - 43 && mouseY < height / 2f + 88) {
            setScrollAmount(scrollAmount - scrollY * 16 / 2f);
        } else if (mouseX > 112 && mouseX < 224 && mouseY > height / 2f - 2 && mouseY < height / 2f + 88) {
            setSpaceStationScrollAmount(spaceStationScrollAmount - scrollY * 16 / 2f);
        }
        return true;
    }

    @Override
    public void onClose() {
        if (moonsParent != null || pageIndex > 0) {
            goBack();
            return;
        }
        Player player = menu.player();
        if (player.isCreative() || player.isSpectator()) super.onClose();
        else if (!(player.getVehicle() instanceof Rocket)) super.onClose();
    }

    protected void close() {
        pageIndex = 0;
        moonsParent = null;
        onClose();
    }

    protected void setScrollAmount(double amount) {
        scrollAmount = Mth.clamp(amount, 0.0, Math.max(0, buttons.size() * 24 - 131));
    }

    protected void setSpaceStationScrollAmount(double amount) {
        spaceStationScrollAmount = Mth.clamp(amount, 0.0, Math.max(0, spaceStationButtons.size() * 24 - 90));
    }

    public void land(ResourceKey<Level> dimension) {
        AdAstra.LOGGER.info("[ad_astra] PlanetsScreen.land() called for dimension={}", dimension.identifier());
        NetworkHandler.CHANNEL.sendToServer(new ServerboundLandPacket(dimension, true));
        close();
    }

    public void landOnSpaceStation(ResourceKey<Level> dimension, ChunkPos pos) {
        AdAstra.LOGGER.info("[ad_astra] PlanetsScreen.landOnSpaceStation() called dim={} pos={}", dimension.identifier(), pos);
        NetworkHandler.CHANNEL.sendToServer(new ServerboundLandOnSpaceStationPacket(dimension, pos));
        close();
    }

    // StringUtils only replaces the first word so WordUtils is needed
    @SuppressWarnings("deprecation")
    public static String title(String string) {
        return WordUtils.capitalizeFully(string.replace("_", " "));
    }

    @Override
    public void removed() {
        super.removed();
        if (CadmusIntegration.cadmusLoaded()) {
            menu.getSortedPlanets().forEach(planet -> CadmusIntegration.removeClientListeners(planet.dimension()));
        }
    }

    static {
        AdAstraClientEvents.RenderSolarSystemEvent.register((graphics, solarSystem, width, height) -> {
            if (PlanetConstants.SOLAR_SYSTEM.equals(solarSystem)) {
                drawCircles(graphics, 0, 4, 0xff24327b, width, height);

                graphics.blit(RenderPipelines.GUI_TEXTURED, DimensionRenderingUtils.SUN, width / 2 - 8, height / 2 - 8, 0, 0, 16, 16, 16, 16);
                float rotation = Util.getMillis() / 100f;
                for (int i = 1; i < 5; i++) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(width / 2f, height / 2f);
                    graphics.pose().rotate((float) Math.toRadians(rotation * (5 - i) / 2));
                    graphics.pose().translate(31 * i - 10, 0);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, DimensionRenderingUtils.SOLAR_SYSTEM_TEXTURES.get(i - 1), 0, 0, 0, 0, 12, 12, 12, 12);
                    graphics.pose().popMatrix();
                }
            }
        });

        AdAstraClientEvents.RenderSolarSystemEvent.register((graphics, solarSystem, width, height) -> {
            if (PlanetConstants.PROXIMA_CENTAURI.equals(solarSystem)) {
                drawCircles(graphics, 1, 1, 0xff008080, width, height);

                graphics.blit(RenderPipelines.GUI_TEXTURED, DimensionRenderingUtils.BLUE_SUN, width / 2 - 8, height / 2 - 8, 0, 0, 16, 16, 16, 16);
                float rotation = Util.getMillis() / 100f % 360f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(width / 2f, height / 2f);
                graphics.pose().rotate((float) Math.toRadians(rotation));
                graphics.pose().translate(53, 0);
                graphics.blit(RenderPipelines.GUI_TEXTURED, DimensionRenderingUtils.GLACIO, 0, 0, 0, 0, 12, 12, 12, 12);
                graphics.pose().popMatrix();
            }
        });
    }
}
