# Ad Astra — Tasks (canonical record)

Full task data. **Do not delete done tasks** — they're useful history.
For comments, see `comments.md`. For at-a-glance open list, see `OPEN.md`.

Status values: `todo` · `in-progress` · `blocked` · `done`
Priority values: `P1` (urgent) · `P2` (important) · `P3` (nice-to-have)
`⚑` marker = needs AI follow-up

---

## Open

### #9 [P3] [todo] Energizer item position fine-tune
**Folder:** Rendering / Models
**Notes:** Currently `poseStack.translate(0.5, 1.7 + yOffset, 0.5)` in `EnergizerBlockEntityRenderer`. Last visual check looked OK. Re-verify if any other renderer change happens around the energizer.

### #11 [P1] [todo] Sky / dimension rendering on space dimensions
**Folder:** Rendering / Models
**Notes:** Largest outstanding visual item in the 1.21.11 port. Affects moon / mars / mercury / venus / glacio dimensions and the **earth space station** (user reports skybox doesn't show earth, stars, etc.).

User 2026-04-30: *"For example when I am in space station above earth it doesnt show the skybox properly to show earth and the other things like stars and such — there should be some legacy docs and stuff to add here."*

Likely needs porting around the new `DimensionSpecialEffects` API in 1.21. Inspect `ModDimensionSpecialEffects`, `ad_astra/planet_renderers/*.json`, and `ad_astra/sky_renderers/*` legacy assets if they were dropped during the port. The user's mention of "legacy docs" suggests upstream Ad Astra wiki / older mod versions have a documented skybox config we can mine.

### #12 [P2] [todo] Cryo freezer — take-out and bucket flow not working
**Folder:** Gameplay
**Notes:** Slot layout: `slot 1`=recipe input, `slot 2`=empty bucket in, `slot 3`=filled bucket out (`CustomSlot.noPlace`).

User reported "still an issue" 2026-04-30. Code review:
- Slot 1 uses `PredicateSlot.ofRecipeInput` — allows pickup by default. Take-out should work.
- Slot 2 is plain `Slot` — accepts any item and allows pickup.
- Slot 3 is `CustomSlot.noPlace` — `canTake=true`, so pickup works; just no place.
- Filling logic in `FluidUtils.moveContainerToItem` requires the internal tank to have ≥81000 (1 bucket worth) of fluid before it'll fill. So if the tank is empty, the bucket sits unconsumed — could read as "bucket placement isn't working".

**Hypothesis**: user may be inserting a bucket before the recipe has produced any cryo fuel. Recipe needs valid input in slot 1 (water bucket / ice / snow per recipe defs) AND energy to run.

**Verification asked of user**: confirm slot 1 has a valid recipe ingredient and the recipe progress bar advances. If yes and it still doesn't fill, it's a real bug to chase. If no, the UX might just need a tooltip/hint.

### #15 [P3] [blocked] [BACKLOG] Cadmus / Argonauts / Athena / Patchouli compat
**Folder:** 1.21.11 Port
**Notes:** Dependencies disabled in `build.gradle.kts` because the libraries are not yet available for 1.21.11. Re-enable once upstream ports land. Tagged **BACKLOG** per user 2026-04-30 — not actively chased; periodic upstream check only.

### #16 [P3] [todo] [BACKLOG] Convention / legacy tag warnings
**Folder:** 1.21.11 Port
**Notes:** `fabric-tag-conventions-v1` logs legacy tag usage at startup. Migrate tags from v1 to v2. Cosmetic — no functional impact. Tagged **BACKLOG** per user 2026-04-30 — bundle with other low-priority cleanups when active work calms down.

---

## Done

### #1 [P1] [done] Mixin classpath crash on runClient
**Folder:** Done
**Notes:** Fabric runClient was crashing on `IAdviceProvider`. Forced `sponge-mixin` `0.17.0 → 0.17.2` via resolutionStrategy in `build.gradle.kts`.

### #2 [P2] [done] Lander floats above launch pad
**Folder:** Done
**Notes:** `LanderRenderer` applied an extra `translate(-1.501)` after the Z-rotation, doubling the Y offset. Replaced rotateZ + double-translate with `translate + scale(-1,-1,1)` matching the rocket renderer.

### #3 [P2] [done] Jet boots stationary while legs walked
**Folder:** Done
**Notes:** Boots were children of root, so leg rotations didn't cascade. Reparented `right_boot`/`left_boot` under the legs in `SpaceSuitModel`. Set `legs.skipDraw=true` in the FEET slot so leg cubes don't render but their transforms still apply to the boot children.

### #4 [P2] [done] Rockets in inventory clipped / zoomed wrong (T1–T4)
**Folder:** Done
**Notes:** Items use `minecraft:special` with custom Java type `ad_astra:rocket` — model JSON `display` block was being ignored. Added per-`ItemDisplayContext` rotation + scale + translate inside `RocketSpecialRenderer.applyContextTransform`. Normalised T3 and T4 `items/` pointers to use the same pure-special-renderer approach as T1/T2.

### #5 [P3] [done] Rocket held horizontal above player's head
**Folder:** Done
**Notes:** Adjusted FIRST_PERSON / THIRD_PERSON cases in `RocketSpecialRenderer.applyContextTransform` — XP rotation -90° + translate up + scale 0.18–0.22.

### #10 [P2] [done] Rocket held in hand — anchor to hand bone
**Folder:** Done
**Notes:** Re-fixed 2026-04-30. The previous THIRD_PERSON Y translate of 1.4 pushed the model far above the hand stack so it visibly floated above the head when the arm raised. Special-renderer transforms execute inside the hand transform stack already, so a small translate is enough to anchor the rocket to the hand. Updated `RocketSpecialRenderer.applyContextTransform` THIRD_PERSON_* and FIRST_PERSON_* both to: `translate(0.5, 0.3, 0.5) + Axis.XP.rotationDegrees(-90) + scale(0.18)`. Rotation matches launch-pad horizontal orientation (model is upright in entity space; XP -90° tips it onto its side along the hand). Build passed.

### #6 [P2] [done] Energizer hovering item rendered as a white cube
**Folder:** Done
**Notes:** Refactored `EnergizerBlockEntityRenderer` to take a `BlockEntityRendererProvider.Context` in its constructor and use `context.itemModelResolver()` (not `Minecraft.getInstance().getItemModelResolver()`). Switched to `ItemDisplayContext.FIXED` and added explicit poseStack scale 0.5 — matches the vanilla `BrushableBlockRenderer` pattern. Texture binds correctly now.

### #7 [P2] [done] Energizer ghost item after take-out
**Folder:** Done
**Notes:** Root-cause fix that helps **every** machine BE: `ContainerHelper.saveAllItems` skips empty slots and `loadAllItems` doesn't reset the list. So when the server emptied a slot and synced to the client, the client kept the old value forever. Fix: clear the items list at the top of `ContainerMachineBlockEntity.loadAdditional` before calling `loadAllItems`. Affects all machines — energizer, cryo freezer, compressor, refinery, etc.

### #13 [P2] [done] Jet suit space movement (turn assist + brake + impact + commands)
**Folder:** Done
**Notes:** Originally reported as "jet boost cancels W-key forward momentum". Resolved via a series of in-space physics tweaks rather than the originally-planned client-side packet refactor:
- `fullFlight` in space redirects 18 % of existing momentum toward look angle each tick + 0.085 forward thrust + 1.8 cap. Steering now actually steers.
- `upwardsFlight` in space damps lateral X/Z by 80 %/tick rather than zeroing instantly — feels like attitude thrusters.
- `applySpaceBrake` (sneak) only acts when touching a block (`onGround || horizontalCollision || verticalCollision`); instant `setDeltaMovement(Vec3.ZERO)` when gated.
- New `VelocityDebugTicker` deals fly-into-wall damage and zeros velocity on impact in space.
- `/adastra debug velocity` toggles a per-player action-bar readout `(vx, vy, vz, |v|, [H-COL]/[V-COL])`.

User confirmed working on 2026-04-30.

### #14 [P2] [done] Advancement JSON format migration
**Folder:** Done
**Notes:** Sub-agent migrated all advancement JSONs from pre-1.20.5 `ItemPredicate` format `{"item":"x","nbt":"..."}` to post-1.20.5 `{"items":["x"]}` (with `nbt` keys removed since durability lives in components in 1.21+).

**Re-verified 2026-04-30** (sub-agent):
- Scanned **562** advancement JSON files under `common/src/main/generated/resources/data/ad_astra/advancement/` (only namespace `ad_astra`; nothing in `common/src/main/resources`).
- Grepped for legacy patterns: `"item":"…"` (singular pre-1.20.5 form), `"nbt":`, and ItemPredicate-shape leftovers (`durability`, `count`, `enchantments`, `potion`). **Zero hits.**
- The only matches for `"item":` are the trigger-condition field name on `using_item` / `filled_bucket` triggers (e.g. `zip_gun.json`, `ocean_cleanup.json`) — `"item": { "items": [ "ad_astra:..." ] }` is the correct post-1.20.5 schema for those triggers (the inner `items` is the `ItemPredicate.items` allow-list).
- `inventory_changed` criterion uses `conditions.items: [ { items|tag: ... } ]` — correct for 1.20.5+.
- All 562 files parse as valid JSON (`python3 json.load`, 0 errors).
- `./gradlew :common:build` **BUILD SUCCESSFUL** with no resource validation complaints.
- Trigger types in use: `changed_dimension`, `filled_bucket`, `inventory_changed`, `recipe_unlocked`, `using_item` — all with current schemas.

Verdict: migration is complete and clean. No further fixes needed.

### #17 [P2] [done] Cryo freezer recipe slot rejected all items
**Folder:** Done
**Notes:** Root-cause for the long-running cryo-freezer report. `PredicateSlot.ofRecipeInput` captured `level.getServer().getRecipeManager()` at slot construction; on the client `getServer()` returns null, so the predicate always returned false and the client-side `mayPlace` rejected every drop before it reached the server. In 1.21+ `RecipeManager` is server-only — clients only see a synced `RecipeAccess`/`RecipePropertySet` subset. Fix: drop the recipe-manager lookup; the slot now accepts any item, and the server's `recipeTick` simply doesn't fire for non-matching items (same UX as Mekanism / AE2 input slots). Affected every machine using `ofRecipeInput`.

### #8 [P2] [done] Cables not pulling power from energizer
**Folder:** Done
**Notes:** `CableBlockEntity.isProducer` only listed `SolarPanelBlockEntity` and `CoalGeneratorBlockEntity`. The energizer was being treated as a consumer, so cables never extracted from it. Added `EnergizerBlockEntity` to the producer set.

### #20 [P2] [done] Earth → space teleport sometimes drops player at y≈1000 instead of top
**Folder:** Gameplay
**Notes:** Fixed in `common/src/main/java/earth/terrarium/adastra/common/utils/ModUtils.java#land`. Previously the arrival `Vec3.y` was set by callers (`atmosphereLeave=600` for rockets, but a few space-station callers passed source-Y verbatim). When the player launched from very high (y≈1000) the source-Y in some paths leaked through to the destination set-pos. Hardened the central `land()` choke-point: when `PlanetApi.API.isSpace(targetLevel)` is true, override the arrival Y to `targetLevel.getMaxY() - 10` regardless of what the caller passed. Lander entity is also spawned at the same overridden Y. Non-space destinations (back to overworld) are unaffected. Build: `./gradlew build` SUCCESSFUL.

---

## How to add a new task

1. Append to `## Open` with the next sequential `#NN`.
2. Mirror the format: `### #NN [Pri] [status] Title`, `**Folder:**`, `**Notes:**`.
3. Add a one-line entry to the right table in `OPEN.md`.
4. If it needs user feedback or commentary, add an empty section in `comments.md` so they know it's there.

When a task moves to `done`:
1. Move its block from `## Open` to `## Done` here.
2. Remove its line from `OPEN.md`.
3. Optionally, leave its section in `comments.md` for historical context (or trim if cleanup is desired).
