# Render Stubs Research — #07–#11

Pre-work for #U06 (decide whether stubs ship as-is or get fixed in this branch). Each entry: what's stubbed, what changed in MC, where to plug back in, complexity estimate.

---

## #07 — `ModSkyRenderer.render()` empty

**File:** `common/src/main/java/earth/terrarium/adastra/client/dimension/ModSkyRenderer.java`

**Stub state:** `render(...)` body is empty. `generateStarMesh()` still works — the data model is intact, only the actual draw call path is missing.

**What changed (per source comments lines 22–38):**
- `VertexBuffer` → `GpuBuffer`
- `RenderSystem.enableBlend/disableBlend/setShader/setShaderTexture` — all removed
- `BufferUploader.drawWithShader()` — removed
- `FogRenderer.levelFogColor()/setupNoFog()` — removed
- New rendering: frame graph with `RenderPipeline` objects (`RenderPipelines.STARS`, `.CELESTIAL`, `.SKY`, `.SUNRISE_SUNSET`)
- Draw calls go through `RenderPass` from the frame graph

**Hooks to find:** `net.minecraft.client.renderer.RenderPipelines` and the frame-graph entry point in `LevelRenderer` / `SkyRenderer`. The vanilla `SkyRenderer` class is the new authoritative reference — copying its draw pattern is the path.

**Complexity:** **High.** Five separate sub-renders (stars, sun, moon, planets, sunrise) each need `GpuBuffer` lifecycle (allocate → upload → draw → free) plus `RenderPass` setup. Easily 200–400 LOC rewrite. Not blocked by anything; can start whenever.

**User-visible cost of leaving stubbed:** Black sky on Moon/Mars/Venus/Mercury/Glacio. No stars, no sun, no celestial bodies. Day/night transition still works (handled by vanilla), just no custom backdrop.

---

## #08 — `ModDimensionSpecialEffects` no longer registered

**File:** `common/src/main/java/earth/terrarium/adastra/client/dimension/ModDimensionSpecialEffects.java`

**Stub state:** Class still exists with all helper methods (`getBrightnessDependentFogColor`, `isFoggyAt`, `getSunriseColor`). What's missing is the **registration path** — `DimensionRenderingRegistry` was removed from Fabric API in 1.21.11.

**What changed (per source comments lines 9–22):**
- `DimensionSpecialEffects` is removed in MC.
- Dimension rendering now goes through `DimensionType.Skybox` enum (`NONE`, `OVERWORLD`, `END`).
- New entry-point classes: `SkyRenderer`, `CloudRenderer`, `WeatherEffectRenderer` (all on the client renderer side).
- `FogRenderer` moved to `net.minecraft.client.renderer.fog.FogRenderer`.

**Hooks to find:** `LevelRenderer.setSkyRenderer(...)` / equivalent — Fabric's removed registry has to be replaced with a mixin into one of the renderer classes, or a `LevelRenderer` field redirect.

**Complexity:** **Medium.** The data is preserved — only the wiring is gone. Most of the work is finding the right injection point and writing a thin mixin. Probably 50–150 LOC across 1–2 mixin classes.

**Coupled with #07.** Sky rendering depends on the dimension being recognized as "custom Ad Astra dimension"; that recognition lives here. Better to do #07 + #08 together as one piece.

---

## #09 — Ti69 handheld renderer

**File:** `common/src/main/java/earth/terrarium/adastra/mixins/client/ItemInHandRendererMixin.java` (and `Ti69Renderer.java`)

**Stub state:** Mixin exists and intercepts `ItemInHandRenderer.renderArmWithItem` correctly. The actual draw call (`Ti69Renderer.renderTi69(...)`) is commented out — line 30 says `// TODO: Ti69Renderer needs updating for SubmitNodeCollector`.

**What changed:**
- `MultiBufferSource` → `SubmitNodeCollector` for first-person item rendering.
- `Ti69Renderer.renderTi69` signature must change from `(PoseStack, MultiBufferSource, ...)` to `(PoseStack, SubmitNodeCollector, ...)`.

**Complexity:** **Low–Medium.** The mixin already has the correct hook + the Ti69Renderer file just needs to be migrated to the new collector API. Probably 20–80 LOC. Risk is mostly in figuring out the right `SubmitNodeCollector` calls for the geometry the Ti69 wants to draw.

**User-visible cost of leaving stubbed:** Holding a Ti69 device renders the player arm with no overlay/UI. The device is functionally invisible in first person.

---

## #10 — Space suit hand renderer

**File:** `common/src/main/java/earth/terrarium/adastra/mixins/client/PlayerRendererMixin.java`

**Stub state:** Mixin is **completely empty** — class body has no methods. The original implementation extended `LivingEntityRenderer` and shadowed `setupRotations`/`setModelProperties` to swap player hands for space-suit hands.

**What changed (per source comments lines 7–18):**
- `PlayerRenderer` → `AvatarRenderer` (rename).
- `LivingEntityRenderer` now takes **3** type parameters: `(Entity, RenderState, Model)`.
- `renderHand` signature changed to `(PoseStack, SubmitNodeCollector, int, Identifier, ModelPart, boolean)`.
- The player entity is no longer available inside `renderHand` — only render state is.
- The old hooks (`setupRotations`/`setModelProperties`) no longer fit the new API surface.

**Complexity:** **High.** This is the most invasive of the five — needs a fresh design. The old approach (extend, shadow methods) was based on the old type hierarchy; the new pattern is closer to render layers. May require splitting space-suit rendering into a separate `RenderLayer` registered against `AvatarRenderer`.

**User-visible cost of leaving stubbed:** Wearing the space suit, the first-person hands and third-person arms render as default player skin instead of the space-suit gloves. Functional but visually wrong.

---

## #11 — `BatterySlot` custom texture

**File:** `common/src/main/java/earth/terrarium/adastra/common/menus/slots/BatterySlot.java`

**Stub state:** Class extends `ImageSlot` — passes the icon via constructor. Comment line 15: `getSlotTexture() was removed in 1.21.11`.

**What changed:** `Slot.getSlotTexture()` is removed; slot art is now driven by `Sprites` data + GUI atlas registration in 1.21.11+.

**Complexity:** **Low.** This is the smallest of the stubs. Likely a one-class-plus-one-resource change: register the battery icon sprite via the new GUI sprite system, then reference it from the slot's render path. Maybe 20–50 LOC + 1 JSON.

**User-visible cost of leaving stubbed:** Battery slot in machine GUIs shows no icon (or default empty slot art) when empty. Not a functional problem.

---

## Bundling recommendation

If you want to fix any during this port:
- **#11** is a freebie — small, isolated, low risk. Recommend doing it.
- **#07 + #08** must be done together (sky depends on dimension wiring). Big chunk; defer unless visual parity matters for the 1.26.1 ship.
- **#09** is moderate and isolated — possible "nice to have" if morale needs a win.
- **#10** is the deepest. Skip unless you specifically want space-suit cosmetic parity.

Recommended scope for #U06 reply:
- **Minimum effort, maximum win:** fix #11 only, leave the rest stubbed.
- **Visual port:** #07 + #08 + #11.
- **Full parity:** all five (significant time investment).
