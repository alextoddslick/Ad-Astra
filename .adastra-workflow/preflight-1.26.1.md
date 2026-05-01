# 1.26.1 Bump — Pre-flight Catalog

Every exact-string edit needed across CSL + Ad Astra to flip from 1.21.11 → 1.26.1. When the version pins for **#U01–#U03** arrive, this becomes a deterministic find-replace.

**Pin variables referenced below** (user supplies):
- `<MC>` — new Minecraft version (assumed `1.26.1`)
- `<PARCHMENT>` — new parchment data version (e.g. `2026.01.04` or whatever's published)
- `<PARCHMENT_DATA>` — parchment data artifact name (e.g. `parchment-1.26.1` if released, or the closest 1.26.x stub — currently `parchment-1.21.8`)
- `<FABRIC_LOADER>` — new Fabric loader version (e.g. `0.18.5`)
- `<FABRIC_API>` — new Fabric API version (the base, the suffix `+<MC>` is appended programmatically in Ad Astra's case)
- `<RLIB>` — new resourcefulLib version (probably `3.12.0` or so)
- `<RCFG>` — new resourcefulConfig version
- `<NEOFORGE>` — new NeoForge version (skip if NeoForge stays deferred per #U05)
- `<NEOFORGE_RANGE>` — NeoForge version range used in `mods.toml` (e.g. `[21.26,)`)

---

## Common Storage Lib (`/Users/alextodd/temp/Github-NOTSYNCED/Common-Storage-Lib`)

**Branch already named `1.26.1`. Working tree currently dirty with 0.0.8 equality-fix changes.** Bump version again to e.g. `0.0.9` after the 1.26.1 swap to keep artifacts distinguishable.

### `gradle.properties`
```
line  7  version=0.0.8                     →  0.0.9 (or user choice)
line 11  minecraftVersion=1.21.11          →  <MC>
line 12  parchmentVersion=2025.07.18       →  <PARCHMENT>
line 16  fabricLoaderVersion=0.18.4        →  <FABRIC_LOADER>
line 17  fabricApiVersion=0.141.3+1.21.11  →  <FABRIC_API>+<MC>
line 19  neoforgeVersion=21.11.38-beta     →  <NEOFORGE>   (skip if NeoForge deferred)
```

### `build.gradle.kts`
```
line 96  parchment-1.21.8                  →  <PARCHMENT_DATA>
```

### Per-submodule manifests (8 files)
All follow the same pattern. The **fabric.mod.json** files have one `"minecraft": ">=1.21.11"` line; the **neoforge.mods.toml** files have a `versionRange = "[1.21.11,)"` line and a `versionRange = "[21.11,)"` NeoForge line.

| File | Lines |
|------|-------|
| `core/fabric/src/main/resources/fabric.mod.json` | `>=1.21.11` → `>=<MC>` |
| `data/fabric/src/main/resources/fabric.mod.json` | `>=1.21.11` → `>=<MC>` |
| `lookup/fabric/src/main/resources/fabric.mod.json` | `>=1.21.11` → `>=<MC>` |
| `resources/fabric/src/main/resources/fabric.mod.json` | `>=1.21.11` → `>=<MC>` |
| `test/fabric/src/main/resources/fabric.mod.json` | `>=1.21.11` → `>=<MC>` |
| `core/neoforge/src/main/resources/META-INF/neoforge.mods.toml` | `[1.21.11,)` → `[<MC>,)` ; `[21.11,)` → `<NEOFORGE_RANGE>` |
| `data/neoforge/src/main/resources/META-INF/neoforge.mods.toml` | same as above |
| `lookup/neoforge/src/main/resources/META-INF/neoforge.mods.toml` | same |
| `resources/neoforge/src/main/resources/META-INF/neoforge.mods.toml` | same |
| `test/neoforge/src/main/resources/META-INF/neoforge.mods.toml` | same |

> NeoForge edits can be deferred — they don't block the Fabric build, and skipping them just means the NeoForge platform won't load until #U05 is answered.

### CSL build / publish sequence after edits
1. `./gradlew --no-daemon build` — expect compile errors against new MC mappings; fix them (this is the unbounded part).
2. `./gradlew --no-daemon publishToMavenLocal` — produces `common-storage-lib-fabric-<MC>` artifacts in `~/.m2`.

---

## Ad Astra (`/Users/alextodd/temp/Github-NOTSYNCED/Ad-Astra`)

### `gradle.properties`
```
line  6  version=1.16.6                  →  bump to 1.17.0 (or your choice)
line  9  minecraftVersion=1.21.11        →  <MC>
line 11  parchmentVersion=2025.07.18     →  <PARCHMENT>
line 25  commonStorageLibVersion=0.0.8   →  0.0.9 (matches CSL bump)
```
> `resourcefulLibVersion` (line 23) and `resourcefulConfigVersion` (line 24) also need to bump to <RLIB> / <RCFG> — currently `3.11.0` / `3.11.2`.

### `build.gradle.kts`
```
line 108  parchment-1.21.8                              →  <PARCHMENT_DATA>
line 113  resourcefullib-$modLoader-1.21.11             →  resourcefullib-$modLoader-<MC>
line 118  resourcefulconfig-$modLoader-1.21.11          →  resourcefulconfig-$modLoader-<MC>
line 123  common-storage-lib-$modLoader-1.21.11         →  common-storage-lib-$modLoader-<MC>
```
> The commented-out optional-dep blocks (Cadmus line 143, Athena line 168, etc.) reference `1.21.11` too. Update these in the same pass even though they stay commented — keeps the file consistent for the eventual re-enable.

### `fabric/gradle.properties`
```
line 1  fabricLoaderVersion=0.18.4        →  <FABRIC_LOADER>
line 2  fabricApiVersion=0.141.3          →  <FABRIC_API>
```
> Ad Astra builds the full Fabric API coordinate as `"$fabricApiVersion+$minecraftVersion"` in `fabric/build.gradle.kts:26`, so you only paste the base version here (no suffix).

### `fabric/src/main/resources/fabric.mod.json`
```
line 46  "minecraft": ">=1.21.11"   →   "minecraft": ">=<MC>"
```

> No edits needed in `common/build.gradle.kts` (no version pins) or `fabric/build.gradle.kts` (just references the property).

---

## Sequencing

1. **CSL first** — change all CSL files, fix any 1.26.1 mapping/API breakage, `publishToMavenLocal`. (Workflow #01.)
2. **Ad Astra next** — apply the catalog above. (Workflow #02.)
3. **Compile errors** — bulk of the work, unbounded. (Workflow #03–#04.)
4. **Verify boot** — runClient. (Workflow #05–#06.)

## NeoForge / optional deps (deferred — #U04 / #U05)

Holding off on:
- All `neoforge.mods.toml` files in CSL.
- `cadmus`, `argonauts`, `athena`, `patchouli`, `shimmer`, `jei`, `rei` version pins (still `cadmusVersion=2.0.0-alpha.5` etc. in Ad Astra `gradle.properties`).
- `neoForgeVersion`, `yabnVersion`, `byteCodecsVersion`, `botariumVersion` — only relevant if NeoForge port reactivated.

These are catalogued in the webui under #U04/#U05; updating them is mechanical once you decide.

---

## Quick string-substitution map

For when pins arrive — every literal that needs to swap:

| Find (literal) | Replace with | Files affected |
|----------------|--------------|----------------|
| `1.21.11` | `1.26.1` | gradle.properties (×2), build.gradle.kts (×4 in Ad Astra), fabric.mod.json (×2), all CSL submodule manifests (×10) |
| `parchment-1.21.8` | `parchment-<X>` | CSL build.gradle.kts:96, Ad Astra build.gradle.kts:108 |
| `2025.07.18` | `<PARCHMENT>` | gradle.properties (×2) |
| `0.18.4` (loader) | `<FABRIC_LOADER>` | CSL gradle.properties:16, Ad Astra fabric/gradle.properties:1 |
| `0.141.3` | `<FABRIC_API>` | CSL gradle.properties:17 (with `+<MC>` suffix), Ad Astra fabric/gradle.properties:2 (no suffix) |
| `21.11.38-beta` | `<NEOFORGE>` | CSL gradle.properties:19 |
| `[21.11,)` | `<NEOFORGE_RANGE>` | All CSL neoforge.mods.toml (×5) |
