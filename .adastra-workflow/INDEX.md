# Ad Astra — Workflow Index

**This is the dashboard.** Read this first. Everything else lives in:
- `tasks.md` — full task records (notes / what was tried)
- `comments.md` — user → AI inbox (your input here)
- `todo.webui.html` — visual UI (browser localStorage; opaque to AI)
- `README.md` — how the system works

---

## Counts

- **5 open** — 1 P1 · 1 P2 · 3 P3
- **1 in-progress** (sub-agent dispatched: #11)
- **1 blocked + backlog** (#15)
- **2 backlog** (#15, #16)
- **12 done** (see `tasks.md`)

## ◐ Currently in progress (sub-agents working)

- **#11** — Sky / dimension rendering: research legacy skybox + port to `DimensionSpecialEffects`

## Open by priority

### P1
| #  | Status      | Title                                              |
|----|-------------|----------------------------------------------------|
| 11 | in-progress | Sky / dimension rendering on space dimensions      |

### P2
| #  | Status      | Title                                              |
|----|-------------|----------------------------------------------------|
| 12 | todo        | Cryo freezer — take-out / bucket flow              |

### P3
| #  | Status            | Title                                                       |
|----|-------------------|-------------------------------------------------------------|
|  9 | todo              | Energizer item position fine-tune                           |
| 15 | blocked · backlog | Cadmus / Argonauts / Athena / Patchouli compat              |
| 16 | todo · backlog    | Convention / legacy tag warnings                            |

## ⛔ Blocked
- **#15** Cadmus / Argonauts / Athena / Patchouli compat — waiting on upstream 1.21.11 ports.

## 💬 User comments awaiting AI review (most recent first)

> The AI prioritises this list above the priority-based "next focus" below.
> When the user drops a dated bullet in `comments.md`, the AI surfaces it here.
> Format: `- <date> #NN — short summary` · `✓` after AI replies in `comments.md`.

- 2026-04-30 #20 — earth→space teleport sometimes places player at y≈1000 instead of top of space ✓ done

## Suggested next focus (priority-based, used when no fresh comments)

1. **#11** (P1) — biggest visible win; only P1 left.
2. **#12** (P2) — verify cryo freezer take-out / bucket flow now that the slot fix landed.
3. **#16** (P3) — re-dispatch sub-agent for tag-conventions migration (previous attempt stalled).

---

## Maintenance

- **AI on completion**: move task in `tasks.md` from `## Open` → `## Done`, update this file's tables and counts.
- **AI when adding new task**: append to `tasks.md`, add a row to the right table here.
- **User adding a comment**: just edit `comments.md`. The AI will pick it up next session and update the "User comments awaiting AI review" list above when it processes them.
