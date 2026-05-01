# Ad Astra — Workflow Dashboard

**Active port:** Fabric **1.21.11 → 1.26.1** · branch `1.26.1`

> ⚠️ **If the webui (`todo.webui.html`) shows stale tasks**, the browser is reading old `localStorage` from the 1.21.11 cycle. The `STORAGE_KEY` was bumped to `adastra-workflow-v4` so a fresh seed loads automatically — **just hard-reload the page** (Cmd-Shift-R). If you still see old tasks, open DevTools → Application → Local Storage and delete entries under `adastra-workflow-v3` (and older).

---

## Counts

- **Open:** 12 port tasks + 6 needs-from-user
- **In progress:** 0
- **Blocked:** 11 port tasks (chain on #01) + everything blocked on user input
- **Done:** 0

---

## Currently in progress

_None._ Suggested next focus: **#01** (CSL bump — gates everything else).

---

## Open by priority

### P0 — Port blockers (must land in order)
| # | Title | Status | Blocked by |
|---|------|--------|-----------|
| #01 | Bump CSL to 1.26.1 + publish locally | todo | — |
| #02 | Wire Ad Astra build files to 1.26.1 | todo | #01 |
| #03 | Vanilla API breakage sweep (`common/`) | todo | #02 |
| #04 | Mixin signature audit | todo | #03 |
| #05 | Compile-clean smoke test | todo | #03, #04 |
| #06 | Runtime boot + world-load test | todo | #05 |

### P1 — Rendering carry-overs from 1.21.11
| # | Title | Status | Blocked by |
|---|------|--------|-----------|
| #07 | ModSkyRenderer rewrite (RenderPipeline) | todo | #06 |
| #08 | ModDimensionSpecialEffects registration | todo | #06 |

### P2 — Rendering carry-overs (cosmetic)
| # | Title | Status | Blocked by |
|---|------|--------|-----------|
| #09 | Ti69 handheld renderer (SubmitNodeCollector) | todo | #06 |
| #10 | Space suit hand (AvatarRenderer migration) | todo | #06 |

### P3 — Polish / deferred
| # | Title | Status | Blocked by |
|---|------|--------|-----------|
| #11 | BatterySlot custom texture | todo | #06 |
| #12 | NeoForge 1.26.1 port | todo | #06 |

---

## 🙋 Needs from user (blocks everything else)

| # | What we need | Blocks |
|---|--------------|--------|
| #U01 | Core toolchain versions (parchment, loom, architectury, mixin extras, sponge-mixin) | #01, #02 |
| #U02 | Fabric loader + Fabric API for 1.26.1 | #02 |
| #U03 | resourcefulLib + resourcefulConfig 1.26.1 versions | #02 |
| #U04 | Decisions/versions for optional deps (Patchouli, Shimmer, Athena, Cadmus, Argonauts, jLAYER, CommonATS, REI, JEI, ModMenu) | #02 (runtime), #06 (testing) |
| #U05 | NeoForge dep versions — only if NeoForge is reactivated | #12 |
| #U06 | Sign-off on carry-over render stubs (#07–#11) staying stubbed | port acceptance bar |

See `tasks.md → ## Needs From User` for full detail and source URLs.

## 💬 User comments awaiting AI review

_None yet — `comments.md` is freshly seeded with empty placeholders for #01–#12 and #U01–#U06._

---

## Suggested next focus

**#01 — Bump CSL to 1.26.1.** Nothing else can compile until the CSL artifact for `1.26.1` is in the local Maven cache. See `tasks.md#01` for steps.

Before starting, the user needs to fill in version pins listed in **Phase 0b** of `~/.claude/plans/we-are-going-to-floofy-plum.md` (parchment, fabric loader/API, etc.).

---

## File reference

- `tasks.md` — task records (open + done sections, technical notes per task).
- `comments.md` — user → AI inbox (one section per task; AI replies inline).
- `ai-notes.md` — append-only session log; newest entry at top is "where we left off".
- `preflight-1.26.1.md` — exact-string find-replace catalog for the 1.26.1 bump (waiting on #U01–#U03 pins).
- `render-stubs-research.md` — per-stub analysis (#07–#11) with complexity + bundling recommendation for #U06.
- `todo.webui.html` — browser UI; reads markdown + maintains its own localStorage state.
- `~/.claude/plans/we-are-going-to-floofy-plum.md` — full migration plan with phase breakdown.
