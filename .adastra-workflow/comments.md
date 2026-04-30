# Ad Astra — Comments (user → AI inbox)

**This file is for you (the user) to talk to the AI about specific tasks.** Drop bullets under any task heading. The AI reads this every session and prioritises the **most-recent comments first**.

- Reference a task by `#NN` (numbers match `tasks.md` and `INDEX.md`).
- One section per task. Add a section if it doesn't exist (copy the format).
- Date each bullet (e.g. `- 2026-04-30: …`) so the AI knows what's freshest.
- After the AI addresses a comment, it appends an indented `> AI <date>: …` reply directly underneath. ✓ = resolved, `?` = needs clarification, `→ #NN` = spun off a new task.
- Keep it light — just bullets. No need to repeat task notes; the AI reads `tasks.md` for those.

---

## #9 — Energizer item position fine-tune
- *(no comments yet)*

## #10 — Rocket held in hand — exact position over head
- 2026-04-30 12:32: this is still an issue and we need to make sure that we have the same functionality as the landing pad does for when the item is in the hand of the player.
- 2026-04-30 12:57: still needs work. Player raises hands above head but model doesnt match where hands are.
  > AI 2026-04-30: ⤴ **reopened** (was done). Dispatching sub-agent to align the in-hand transform with the launch-pad orientation and bind it to the actual hand bone position. Will tune `RocketSpecialRenderer.applyContextTransform` THIRD_PERSON_* cases.

## #11 — Sky / dimension rendering on space dimensions
- 2026-04-30 12:58: For example when I am in space station above earth it doesnt show the skybox properly to show earth and the other things like stars and such — there should be some legacy docs and stuff to add here.
  > AI 2026-04-30: ✓ noted. Dispatching sub-agent to research the legacy Ad Astra skybox setup (older versions, wiki) and port it to 1.21's `DimensionSpecialEffects` API. Earth space station view of earth + stars is the explicit acceptance criterion.
  > AI 2026-04-30: ✓ shipped. Two-part fix: (1) earlier sub-agent migrated dimension_type JSONs to 1.21.11 schema → orbits use `skybox: end` so vanilla End-skybox stars + black sky render. (2) This pass added planet-disc rendering: new `PlanetSkyRenderer` builds a `POSITION_TEX` GpuBuffer once and submits per `SkyRenderable` via `RenderPipelines.CELESTIAL`; `SkyRendererMixin` injects at `TAIL` of `renderSunMoonAndStars` so discs sit over stars. Geometry mirrors legacy 1.20 (translate to y=100, scale by SkyRenderable.scale, optional backlight glow). Earth in `earth_orbit.json` has scale=80 → big disc opposite the sun. Fabric `DIMENSION_RENDERERS` map wired (was a no-op) so the loaded `PlanetRenderer` JSON data finally reaches the renderer. Both fabric + neoforge `./gradlew build` pass. **In-game check needed**: load the earth space station, look down — should see Earth as a large textured disc, plus sun, plus moon, plus stars. If discs are upside-down or sideways, that's a quad-winding / rotation-axis issue we can iterate on. If the overworld sky changed at all, mixin-scope is wrong (it shouldn't — early-returns when no `ad_astra` planet renderer for the dimension).
- 2026-04-30: User reports sky still showing only End skybox at earth space station — Earth disc never appears.
  > AI 2026-04-30: ✓ **root-cause fixed.** Decompiled `LevelRenderer.method_62215` (the sky dispatcher) confirmed that when `DimensionType.skybox == END`, vanilla calls `SkyRenderer.renderEndSky()` and **never** `renderSunMoonAndStars`. Our previous mixin only hooked `renderSunMoonAndStars` → injection target was dead code in orbit dimensions, so `PlanetSkyRenderer.render` never fired. Added a second `@Inject(method = "renderEndSky", at = TAIL)` in `SkyRendererMixin` that creates a fresh PoseStack (End sky has no time-of-day rotation to unwind) and calls `PlanetSkyRenderer.render` with a day fraction derived from `level.getDayTime() % 24000 / 24000.0f`. Both branches (overworld-style and end-style skies) now render the planet discs. `PlanetSkyRenderer` still early-returns for any dimension without a registered Ad Astra renderer, so vanilla End/overworld are untouched. Kept `skybox: end` on the orbit JSONs (vs switching to `none`) because `addSkyPass` short-circuits at the very top for `Skybox.NONE` — there'd be no hook point at all without redirecting into `LevelRenderer` itself, which is much more fragile. Build green on fabric + neoforge.
- 2026-04-30: User: "the end sounds and visuals. We need to make sure that doesnt occur. make sure this is done with other planets for space. earth / good"
  > AI 2026-04-30: ✓ **shipped.** Earth orbit was rendering correctly because we'd fluked into the renderEndSky-TAIL hook firing; the rest of space still showed the End purple void texture *and* played the End/cave drone. Three-part fix: (1) **all** space `dimension_type` JSONs (orbits + moon/mercury/mars/venus/glacio) now use `"skybox": "none"` — vanilla draws zero End visuals. (2) New `LevelRendererSkyPassMixin` injects HEAD of `LevelRenderer.addSkyPass` (cancellable). When the current dim has an Ad Astra renderer registered, we schedule our own `FrameGraphBuilder` pass that reuses vanilla's `SkyRenderer.renderStars` (via new `SkyRendererAccessor` invoker mixin) for the star field, then calls `PlanetSkyRenderer.render` for the planet/sun/moon discs, and cancels the vanilla method so it doesn't NONE-early-return. Non-Ad-Astra dims fall through unchanged. Old `SkyRendererMixin` deleted (its TAIL injections were dead code under the new scheme). (3) Stripped `mood_sound: minecraft:ambient.cave` from all 10 Ad Astra space biomes — that's the cave drone that doubles as the End ambient sound the user was hearing. Build green on fabric + neoforge. **In-game verify**: each space dim should show stars + planet discs over a black sky, no End purple, no cave drone.

## #12 — Cryo freezer — take-out and bucket flow
- 2026-04-30 12:00: Still an issue.
  > AI 2026-04-30: ? Code review: slots 1, 2, 3 all allow pickup. Bucket fill needs the internal tank to have ≥81 000 of cryo fuel.
- 2026-04-30 12:35: ice shards still won't go into the recipe slot
  > AI 2026-04-30: ✓ **Found and fixed.** `PredicateSlot.ofRecipeInput` was looking up the recipe manager via `level.getServer().getRecipeManager()` — that returns null on the client, so the client-side `mayPlace` check rejected every click before it reached the server. Affected **every** machine slot using this pattern (cryo freezer, etc.). Switched to accept-any-item; server-side recipe match handles validity. Rebuilt; please retest. Recipes for ice / packed ice / blue ice / ice shard all exist (`cryo_fuel_from_cryo_freezing_*.json`).

## #13 — Jet boost cancels W-key forward momentum
- 2026-04-30 12:00: still an issue
  > AI 2026-04-30: ✓ design drafted in tasks.md `#13`. Plan: move the velocity push to client-side player tick (preserves client X/Z naturally), keep energy validation server-side via a small `BoostEnergyPacket`. Awaiting your go-ahead before implementing — it adds two new files and removes the existing server-side push.
- 2026-04-30: User confirmed jet suit stuff is fully fixed.
  > AI 2026-04-30: ✓ marked done. Resolved via in-space physics tweaks (turn assist, lateral damping, brake-on-contact, wall-impact damage, debug command) rather than the originally-drafted client-side packet refactor. The packet-refactor design stays archived in tasks.md history if we ever need to revisit.

## #14 — Advancement JSON format migration
- 2026-04-30 12:00: work on this with a sub agent.
  > AI 2026-04-30: ✓ **resolved.** Sub-agent migrated all advancement JSONs. Verified in runclient.log at 12:44 — `Loaded 145 advancements` with **zero** parse errors. Player is now awarded the previously-broken achievements (To Infinity and Beyond, Electric Boogaloo, Make Newton Proud, Green Energy, Ice Ice Baby).

## #14 — Advancement JSON format migration
- 2026-04-30 12:58: I am not entirely sure this is done.
  > AI 2026-04-30: ⤴ **reopened for verification**. Sub-agent will rescan all `data/**/advancement/**.json` for any remaining pre-1.20.5 patterns (`"item":` / `"nbt":` / legacy `inventory_changed` predicate shapes) and run the parser to confirm zero errors.
  > AI 2026-04-30: ✓ **verified clean & re-marked done.** Scanned all **562** advancement JSONs under `common/src/main/generated/resources/data/ad_astra/advancement/`. Zero legacy `"item":"x"` (singular form) or `"nbt":` keys remain. The two files matching `"item":` (`zip_gun.json`, `ocean_cleanup.json`) use the correct `using_item` / `filled_bucket` trigger schema — `item` there is the trigger's condition field, with the post-1.20.5 `{"items":[…]}` predicate inside. `inventory_changed` files use `conditions.items: [{items|tag: …}]` (1.20.5+ shape). All 562 parse as valid JSON; `./gradlew :common:build` SUCCESSFUL. Migration is complete — nothing further to fix.

## #15 — Cadmus / Argonauts / Athena / Patchouli compat
- 2026-04-30 12:59: add this to a backlog folder / tag or something
  > AI 2026-04-30: ✓ tagged as **backlog** in `tasks.md` notes. Stays blocked on upstream 1.21.11 ports — will check periodically.

## #16 — Convention / legacy tag warnings
- 2026-04-30 12:01: work on this with a sub agent
  > AI 2026-04-30: ✓ dispatching general-purpose sub-agent to scan tag usage and migrate v1 → v2 conventions where applicable.
- 2026-04-30 12:59: add this to backlog as well
  > AI 2026-04-30: ✓ tagged as **backlog**. No functional impact (cosmetic startup warnings only). Will batch with other low-priority cleanups when the active work calms down.

## #21 — Earth y≥1000 auto-teleport to space station
- 2026-04-30: when the player gets to y 1000 on earth it should teleport them to the spacestation at y -100.
  > AI 2026-04-30: ✓ shipped. New `AtmosphereLeaveTicker` (server-tick) tracks per-player previous-tick Y; on the upward crossing of y=1000 in the overworld it teleports the player to `ad_astra:earth_orbit` at `(x, -100, z)`. Added `ModUtils.landAt(...)` so the explicit y=-100 isn't stomped by the top-of-space clamp introduced in #20 — `ModUtils.land(...)` (rocket / jet-suit path) keeps the clamp, the new path opts out. Crossing-only check (not `>= 1000`) prevents repeat fires and login-at-altitude misfires. Both fabric + neoforge `./gradlew build` SUCCESSFUL.

## #20 — Earth → space y-height issue
- 2026-04-30: when going to space after y 1000, it sometimes sends the user to space at y 1000, it should send user to the very bottom of the space +10.
  > AI 2026-04-30: 📥 captured from webui screenshot earlier. Dispatching sub-agent now to inspect dimension-transfer arrival-Y in `ModUtils` / planet-teleport flow.
  > AI 2026-04-30: ✅ fixed in `common/.../utils/ModUtils.java#land`. Hardened the central teleport choke-point: when `PlanetApi.API.isSpace(targetLevel)` is true, the arrival Y is now overridden to `targetLevel.getMaxY() - 10` regardless of what the caller passes (some callers, like the space-station packets, were forwarding the *source* Y verbatim — that's how y=1000 leaked through). The lander entity is also spawned at the same overridden top-of-build Y so it falls toward the surface as expected. Non-space destinations (returning to overworld) are unaffected. `./gradlew build` SUCCESSFUL. Marked done.

