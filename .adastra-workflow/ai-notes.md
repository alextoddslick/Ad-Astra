# Ad Astra — AI Notes (where we left off)

Append-only log written by the AI assistant after each working session.
Most recent entry is at the **top**. The HTML UI reads this file and shows
the latest entry in the Notifications drawer.

Format per entry:
```
## YYYY-MM-DD HH:MM — Short headline
- bullet of what was completed
- bullet of what's open / next
- references task #NN where relevant
```

---

## 2026-04-30 — #22 [P3] [done] sliding doors second-panel rendered pure white

The simple sliding doors (iron/steel/desh/ostrum/calorite) shared the parent `block/sliding_door.json` which only defines geometry+UVs for the right half of the door slot, sourcing pixels from the left half of a 64x64 texture (right half is fully blank, alpha=0). The renderer's "no flipped model" path applied a 180° Y rotation + Z translate and re-rendered the same model — but the post-rotation faces ended up reading the texture in a way that produced a blank (white) panel.

Fix (Option A, renderer-side): added a new parent `block/sliding_door_flipped.json` with mirrored geometry (`from [4,-16,0] to [28,32,3]`) and U-mirrored face UVs (north/south swapped, east/west swapped) so the second panel reads valid texture pixels and renders as a mirror image of the first. Added five per-variant flipped models (`{iron,steel,desh,ostrum,calorite}_sliding_door_flipped.json`) — 3-line files binding `0` + `particle` to each variant's texture. Updated `SlidingDoorBlockEntityRenderer.submit` to *always* try `block/<id>_flipped` first via `ClientPlatformUtils.getModel`, and only fall back to the rotation path when no flipped model is registered. Airlock and reinforced_door already shipped with their own `_flipped` JSONs and are unaffected. Build green for fabric + neoforge.

---

## 2026-04-30 14:45 — Parallel sub-agent sweep: 3 closed, 1 partial

User exported localStorage as JSON and dropped six lingering comments in via `comments.md`. Mapped them onto canonical `#NN` and dispatched 4 agents in parallel.

**✅ #10 closed — Rocket-in-hand transform aligned with hand bone.**
`RocketSpecialRenderer.applyContextTransform` THIRD_PERSON_* and FIRST_PERSON_* now use `translate(0.5, 0.3, 0.5) + XP -90° + scale 0.18`. The previous Y=1.4 was the bug — special-renderer transforms run *inside* the hand transform stack, so the big Y push shoved the rocket above the head instead of anchoring it. Build green.

**🔧 #11 partial — Orbit/space dimensions now have dark sky + stars; Earth disc still TBD.**
Real root cause was that all 11 `dimension_type/*.json` files still used the dropped 1.20 schema (`effects`, `bed_works`, `natural`, etc.). 1.21.11 made `DimensionType` a record with a `skybox` enum (`none|overworld|end`) and an `attributes` map. Codec was rejecting them, dimensions silently fell back to overworld defaults — that's why the user saw a regular blue sky in earth orbit. Rewrote all 11 JSONs:
- Orbits + airless surfaces (`*_orbit`, `moon`, `mercury`) → `skybox: end` → vanilla End renderer = black sky + stars.
- `mars`, `venus`, `glacio` → `skybox: none` + planet-tinted sky/fog colors.
What's still missing: **Earth-as-large-disc** rendering. `DimensionRenderingRegistry` is gone in 1.21.11, replaced by a `SkyRenderer` + `RenderPipelines` + `GpuBuffer` pipeline. Need a Mixin into `SkyRenderer.renderSunMoonAndStars` to draw the existing `SkyRenderable` records (which `ModDimensionSpecialEffects` already loads from `planet_renderers/*.json`) as textured quads via `RenderPipelines.CELESTIAL`. Build green; #11 stays open with a clear next-step plan in `tasks.md`.

**✅ #14 closed — Advancement migration verified clean.**
Sub-agent scanned **562** advancement JSONs. Zero legacy `"item":` (singular) or `"nbt":` keys. The two files matching `"item":` use the correct 1.20.5+ trigger schema (`using_item` / `filled_bucket`). All 562 parse. `:common:build` SUCCESSFUL. Re-marked done.

**✅ #20 closed — Earth→space teleport y-height clamp.**
Hardened `ModUtils.land()`: when `PlanetApi.API.isSpace(targetLevel)`, arrival Y is now `targetLevel.getMaxY() - 10` regardless of caller. Root cause was `ServerboundConstructSpaceStationPacket` and `ServerboundLandOnSpaceStationPacket` forwarding the *source* Y. Lander entity also spawned at the same top-of-build Y. Non-space destinations untouched. Build green. Single commit `c412752f` (not pushed).

**Counts now:** 5 open (1 P1 · 1 P2 · 3 P3) · 1 in-progress (#11 disc work) · 1 blocked+backlog (#15) · 2 backlog (#15, #16) · 12 done.

**Webui fixes shipped this session:**
- `🔗 Link folder` / `📋 Copy comments.md` button always visible in toolbar (no more "Grant access not visible" issue).
- Safari/Firefox sync modal: shows full `comments.md` content with one-click "Copy to clipboard". Replaces the dead-end "Browser unsupported" toast.

**Suggested next session:**
1. **#11 finish** — write `SkyRendererMixin` to render planet discs / Earth via `RenderPipelines.CELESTIAL`.
2. **#12** — cryo freezer pickup retest now that user can confirm in-game.
3. **#9** — energizer fine-tune (low priority).

---

## 2026-04-30 14:05 — New P2 #20 logged (earth→space y-height bug) + webui Grant access fix

- User reported via webui task #20 (visible in screenshot only — folder access wasn't linked, so the localStorage entry hadn't auto-saved): *"when going to space after y 1000, it sometimes sends the user to space at y 1000, it should send user to the very bottom of the space +10."* Captured into `tasks.md` / `comments.md` / `INDEX.md` as **#20 [P2] [todo]**.
- Webui fix: replaced the silent "○ not linked" indicator with (a) a clickable underlined indicator and (b) a prominent **🔗 Grant access** button in the toolbar that auto-hides once the folder is linked. Previous behaviour: the only entry point was a notifications-drawer card that some users dismissed or couldn't see. New entry point is always present in the top toolbar until linked.

Counts now: **7 open** (1 P1, 2 P2, 4 P3), 1 blocked, 10 done.

Next session pick:
1. **#20** — inspect dimension-transfer arrival-Y. Files: `common/.../utils/dimension/ModUtils.java`, `common/.../registry/ModDimensions.java`. Hypothesis: arrival Vec3 reuses source-dimension Y instead of clamping to `destLevel.getMaxY() - 10`.
2. **#11** (P1 sky rendering) — still the biggest visual.
3. **#16** with a fresh sub-agent.

---

## 2026-04-30 13:30 — #13 closed (jet suit confirmed)

User confirmed: jet suit stuff is fully fixed. Marked **#13 done**.

Counts now: **6 open** (1 P1, 1 P2, 4 P3), 1 blocked, 10 done.

What's left (no blockers from user comments):
- **#11** P1 — sky / dimension rendering (biggest open visual issue)
- **#12** P2 — cryo freezer pickup awaiting verification (slot fix landed; suspected side-config widget interaction is unconfirmed)
- **#9, #10** P3 — small rendering tweaks
- **#15** P3 blocked — upstream port deps
- **#16** P3 — tag-conventions sub-agent stalled previously, needs fresh dispatch

Next session pick:
1. Tackle **#11** (sky rendering) — highest impact remaining.
2. Or quick-win **#16** with a fresh sub-agent.
3. Or chase **#12** if user reports back on the cryo retest.

---

## 2026-04-30 13:00 — Jet-suit space movement + collision damage + debug command

Just landed:
- **#13 (jet boost)** — added an in-space turn-assist (looks redirect 18 % of momentum each tick toward the look angle) + sprint+jump thrust bumped to 0.085 + sneak space-brake (8 %/tick velocity dampening). User said "close" — knobs ready to tune.
- **Wall-impact damage**: while in space and wearing a jet suit, hitting a wall with `|v|` ≥ 0.5 deals scaled damage and zeroes velocity instantly (no more "stuck in a corner with phantom momentum").
- **`/adastra debug velocity`** chat command: toggles a per-tick action-bar readout of `(vx, vy, vz, |v|)` plus `[H-COL]`/`[V-COL]` flags so the user can diagnose stuck-velocity issues.

Where we are:
- ✅ #14 advancement JSON migration verified clean (`Loaded 145 advancements`, zero parse errors, achievements being awarded again).
- ✅ #17 cryo freezer recipe slot fixed — root cause was `level.getServer().getRecipeManager()` being null on the client. Slot now accepts any item; server-side recipe match handles validity.
- 🔧 #13 jet boost — turn-assist + brake landed; user says "close", needs tuning. Available knobs: `turnRate (0.18)`, `thrust (0.085)`, `maxSpeed (1.8)`, brake multiplier (`0.92`, lower = harsher).
- 🛠 #16 tag conventions — sub-agent stalled previously; needs a fresh dispatch.
- ❓ #12 cryo freezer pickup — strong suspect is the side-config widget disabling player inventory slots while open; awaiting user confirmation that closing the side config restores pickup.

Suggested next focus when the user comes back:
1. Fine-tune jet-suit knobs based on user feedback.
2. Tackle **#11** (sky / dimension rendering) — only P1 left.
3. Or batch-attack **#16** (tag conventions) with a fresh sub-agent.
