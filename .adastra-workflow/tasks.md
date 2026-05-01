# Ad Astra — Task Records

Canonical task records for the Fabric **1.21.11 → 1.26.1** port. Each task is keyed by `#NN`. Numbers don't change.

**Status legend:** `[todo]` not started · `[in-progress]` actively being worked on · `[blocked]` waiting on something · `[done]` complete.

**Priority legend:** `P0` shipping blocker · `P1` core feature broken · `P2` non-blocking but visible · `P3` polish / cosmetic.

---

## Open

### #01 — Bump Common Storage Lib to 1.26.1 [todo] [P0] [port]

Sibling repo at `/Users/alextodd/temp/Github-NOTSYNCED/Common-Storage-Lib`. Already on a `1.26.1` branch but `gradle.properties` still says `1.21.11`.

**Steps:**
- Update `gradle.properties`: `minecraftVersion`, `parchmentVersion`, fabric loader/API versions, bump `version` (e.g. `0.0.7` → `0.0.8`).
- Resolve any 1.26.1 mapping/API changes in the fluid/energy/storage/resources/context packages.
- `./gradlew publishToMavenLocal`.
- Verify `~/.m2/repository/earth/terrarium/common_storage_lib/common-storage-lib-fabric-1.26.1/<version>/` exists.

Blocks: #02, #03.

---

### #02 — Wire Ad Astra build files to 1.26.1 [todo] [P0] [port]

Files: `gradle.properties`, root `build.gradle.kts`, `common/build.gradle.kts`, `fabric/build.gradle.kts`, `fabric/gradle.properties`, `fabric/src/main/resources/fabric.mod.json`.

**Steps:**
- Apply all version bumps once user supplies them (see Phase 0b in plan file).
- Change CSL artifact name suffix `common-storage-lib-$modLoader-1.21.11` → `-1.26.1`.
- Bump `fabric.mod.json` `depends.minecraft` range and `depends.common_storage_lib`.
- `./gradlew --refresh-dependencies fabric:genSources` to confirm Loom resolves.
- Expect `./gradlew fabric:compileJava` to fail at this point — capture the error wall for #03.

Blocks: #03. Blocked by: #01.

---

### #03 — Vanilla API breakage sweep — `common/` module [todo] [P0] [port]

446 Java files, ~34.5K LOC. The bulk of the port. Compile-fix-repeat against the error wall from #02.

**Watch for:**
- Registry / codec / packet API churn (1.21 → 1.26 is a major jump).
- 43 files import CSL — most should "just work" after #01, but `FluidUtils.java` has a workaround for a CSL `FluidResource` reference-equality bug that may now be fixed upstream (re-test, can simplify).
- The 5 known-stubbed renderers stay stubbed (see #07–#11). Just confirm they still compile.

**Verification per loop:** `./gradlew fabric:compileJava` until exit 0, then `./gradlew fabric:build`.

Blocks: #04, #05. Blocked by: #02.

---

### #04 — Mixin signature audit [todo] [P0] [port]

Mixin targets shift between MC versions; every `@Inject` / `@Redirect` needs verification.

**Scope:** 37 mixins in `common/src/main/resources/adastra-common.mixins.json` + 4 in `fabric/src/main/resources/adastra.mixins.json`. Mixin classes under `common/src/main/java/earth/terrarium/adastra/mixins/`.

**Already known to be stubbed (do not touch):** `ItemInHandRendererMixin` (#09), `PlayerRendererMixin` (#10).

Blocks: #05. Blocked by: #03.

---

### #05 — Compile-clean smoke test [todo] [P0] [port]

`./gradlew fabric:build` with exit 0 and no mixin verification errors in the build log.

Blocks: #06. Blocked by: #03, #04.

---

### #06 — Runtime boot + world-load test [todo] [P0] [port]

`./gradlew fabric:runClient`, then:
- Boot to main menu — log any mixin failures.
- New creative world loads, no chunk-gen crash.
- Creative inventory → Ad Astra tab — items render.
- Place rocket / rover / globe — 3D model renders (NoDataSpecialModelRenderer path).
- Open a machine GUI — energy bar visible, fluid bar visible (CSL).
- Travel to Moon dimension — sky will be black (stubbed renderer, expected).
- Check `logs/latest.log` for CSL warnings.

**Acceptance:** boots, loads worlds, no crashes, parity with 1.21.11 minus the documented stubs.

Blocked by: #05.

---

### #07 — ModSkyRenderer rewrite for 1.26.1 RenderPipeline [todo] [P1] [rendering] [carry-over]

File: `common/src/main/java/earth/terrarium/adastra/client/dimension/ModSkyRenderer.java`. Currently empty (TODO comment line 30-38). Custom dimension sky needs to be rewritten against the new RenderPipeline / RenderPass / GpuBuffer frame-graph API.

**Status:** stubbed during 1.21.11 port; carrying over. Black sky on Moon/Mars/etc. is expected until this is fixed.

Blocked by: #06 (port must be bootable first).

---

### #08 — ModDimensionSpecialEffects registration [todo] [P1] [rendering] [carry-over]

`DimensionRenderingRegistry` was removed from Fabric API in 1.21.11; needs a mixin approach. File: `common/src/main/java/earth/terrarium/adastra/client/dimension/ModDimensionSpecialEffects.java`.

**Status:** carry-over from 1.21.11.

Blocked by: #06.

---

### #09 — Ti69 handheld device renderer [todo] [P2] [rendering] [carry-over]

File: `common/src/main/java/earth/terrarium/adastra/mixins/client/ItemInHandRendererMixin.java`. Needs migration from `MultiBufferSource` to `SubmitNodeCollector`. Currently disabled (mixin lines 29-30).

**Status:** carry-over.

Blocked by: #06.

---

### #10 — Space suit hand rendering [todo] [P2] [rendering] [carry-over]

File: `common/src/main/java/earth/terrarium/adastra/mixins/client/PlayerRendererMixin.java`. `PlayerRenderer` → `AvatarRenderer` rename + `LivingEntityRenderer` type-param changes + `renderHand` signature mismatch.

**Status:** carry-over.

Blocked by: #06.

---

### #11 — BatterySlot custom texture [todo] [P3] [rendering] [carry-over]

File: `common/src/main/java/earth/terrarium/adastra/common/menus/slots/BatterySlot.java`. `getSlotTexture()` was removed in 1.21.11; needs alternative.

**Status:** carry-over.

Blocked by: #06.

---

### #12 — NeoForge 1.26.1 port [todo] [P3] [port] [deferred]

Deferred to its own branch. Mirrors what was deferred during 1.21.11. Datagen providers also still disabled.

Blocked by: #06.

---

### #13 — Delete orphaned CSL-migration stubs [todo] [P3] [cleanup]

Discovered during the autonomous sweep on 2026-04-30. Both files are **completely empty class bodies** with only TODO comments — implementation was wiped during the original CSL migration, and the surrounding codebase has moved on without ever needing them again.

- `common/src/main/java/earth/terrarium/adastra/common/container/BiFluidContainer.java` — zero external references.
- `common/src/main/java/earth/terrarium/adastra/common/recipes/base/BotariumByteCodecs.java` — zero external references.

`grep -rn "BiFluidContainer\|BotariumByteCodecs" common/ fabric/ neoforge/` returns only the files themselves. Build passes without changes (verified 2026-04-30 with `fabric:build` → BUILD SUCCESSFUL).

**Action:** delete both files. Git history preserves the breadcrumb if anyone needs to know what they used to do. **Not done autonomously — file deletion is destructive, awaiting user sign-off.**

---

### #14 — Create painting-variant data pack JSONs [done] [P3] [cosmetic]

Carry-over from 1.21.11 port (was tracked as P2 cosmetic). `common/src/main/java/earth/terrarium/adastra/common/registry/ModPaintingVariants.java` is now a no-op (paintings are data-driven in 1.21+).

**All 14 textures verified present** at `common/src/main/resources/assets/ad_astra/textures/painting/` (mercury/moon/pluto 1×1, earth/glacio/mars/venus 2×2, jupiter/neptune/uranus 3×3, saturn/the_milky_way 4×3, alpha_centauri 4×4, sun 5×5).

**Created** 14 JSONs at `common/src/main/resources/data/ad_astra/painting_variant/*.json`. Schema follows 1.21+ data-driven painting variant format: `asset_id`, `width`, `height` in blocks, `title`/`author` as translation components.

**Translation keys used:** `painting.ad_astra.<name>.title` and `painting.ad_astra.<name>.author` for each. **No lang entries created** — `en_us.json` doesn't exist in this repo (other lang files exist for ko/fr/de/ja/ru/uk/zh). Adding English titles/authors is a separate decision (translator/lore call).

**Verified:** `./gradlew --no-daemon fabric:build` → BUILD SUCCESSFUL in 12s.

---

## Needs From User

These items can't be progressed by the AI alone — they need a version string, a decision, or external info from the user. Each one blocks downstream work.

### #U01 — Provide MC 1.26.1 version pins for **core toolchain** [needs-user] [P0]

Required before #01/#02 can start. Currently unknown:

- `parchmentVersion` (currently `2025.07.18`, MC 1.21.8 data) — pick the latest 1.26.1-compatible mappings from `maven.parchmentmc.org`.
- `mixinExtrasVersion` (currently `0.4.1`) — confirm latest.
- Loom plugin version (root `build.gradle.kts`, currently `1.13-SNAPSHOT`) — needs a 1.26.1-compatible Loom.
- Architectury plugin version (currently `3.4-SNAPSHOT`).
- sponge-mixin override (currently pinned `0.17.2` to work around a 0.17.0 classloader bug) — confirm if still needed.

### #U02 — Provide **Fabric platform** versions [needs-user] [P0]

`fabric/gradle.properties` currently pins:
- `fabricLoaderVersion=0.18.4` — needs 1.26.1-compatible loader version.
- `fabricApiVersion=0.141.3` — must end in `+1.26.1` suffix; check fabricmc.net or modrinth.

### #U03 — Provide **required mod dep** versions [needs-user] [P0]

These are hard `modApi` dependencies — Ad Astra won't build without them.

| Dep | Current | Source |
|-----|---------|--------|
| `commonStorageLibVersion` | `0.0.7` | sibling repo, **we own this** — published locally after #01 |
| `resourcefulLibVersion` | `3.11.0` | maven.teamresourceful.com |
| `resourcefulConfigVersion` | `3.11.2` | maven.teamresourceful.com |

### #U04 — Decide on **optional mod deps** that may not have 1.26.1 builds [needs-user] [P2]

These were deferred during the 1.21.11 cycle (commented out in build files). For each, we need: (a) a 1.26.1 version string if it exists, or (b) confirmation to keep deferred.

| Dep | Current pin | 1.21.11 status | Notes |
|-----|-------------|----------------|-------|
| Patchouli | `81` | commented out | in-game guide book; large surface, can stay deferred |
| Shimmer | `0.2.3` | commented out | bloom rendering for fluids; cosmetic |
| Athena | `4.4.0` | commented out | connected-textures dep used by some blocks |
| Cadmus | `2.0.0-alpha.5` | commented out, alpha | claims integration |
| Argonauts | `2.0.0-alpha.5` | commented out, alpha | guilds/parties integration |
| jLAYER | `1.0.1` | active | audio lib in `build.gradle.kts`; check 1.26.1 compat |
| CommonATS | `2.0` | modCompileOnly | access transformers; confirm still needed |

**REI / JEI / ModMenu** are also pinned (`21.11.814`, `27.4.0.15`, `17.0.0-beta.1`) — versions for 1.26.1 needed for runtime testing in #06.

### #U05 — Decide on **NeoForge-only deps** if NeoForge port is reactivated [needs-user] [P3]

Out of scope for this branch but listed for completeness:
- `neoForgeVersion=21.11.38-beta`
- YABN 1.0.3 (NBT lib, runtime)
- ByteCodecs 1.0.2 (codec lib, runtime)
- Botarium 3.2.2+ (declared in `neoforge.mods.toml`)

Blocks: #12.

### #U06 — Confirm **carry-over render stubs** still acceptable [needs-user] [P2]

The 1.21.11 port left 5 features stubbed (#07–#11). Ad Astra ships with: black sky on space dimensions, no Ti69 handheld render, hands invisible in space suit first-person, BatterySlot using default slot art. Confirm these are still acceptable for the 1.26.1 ship, or flag any to fix as part of this port.

---

## Done

### #PRE01 — CSL `gradle.properties` JDK home + FluidResource/EntityResource equality fix [done] [P0] [autonomous]

Discovered while reconning CSL ahead of #01. **Two issues found and fixed in `Common-Storage-Lib`:**

1. **CSL build broke at baseline** because Loom 1.13-SNAPSHOT requires Java 21 but CSL's `gradle.properties` had no `org.gradle.java.home` — system default Java 17 was being used. Added `org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` (matches Ad Astra's setup).

2. **`FluidResource.equals()` was reference-equality** because `ResourceComponent` (parent class) doesn't override `equals/hashCode`, and `FluidResource` didn't override them either. **`ItemResource` does override them.** This was clearly an oversight, not intentional, and is the bug Ad Astra works around in `FluidUtils.insertFluid()` / `insertFluidStorage()`. Mirrored ItemResource's pattern in `FluidResource.java`. Found the same gap in `EntityResource.java` and fixed it for consistency.

**Verification:** `./gradlew --no-daemon build` → BUILD SUCCESSFUL in 11s after fixes (was failing with "JVM runtime version 21 required" before the JDK home fix).

**Not yet published to mavenLocal** — would overwrite existing `0.0.7-1.21.11` artifact, and the user should review the diff first. After review/publish, Ad Astra's `FluidUtils.insertFluid()` / `insertFluidStorage()` workarounds (`common/.../utils/FluidUtils.java` lines ~166-200) become removable. Slot's normal `insert(resource, ...)` path will work directly.

**Files changed in CSL repo (working tree dirty, not committed):**
- `gradle.properties` (added java.home)
- `resources/common/src/main/java/earth/terrarium/common_storage_lib/resources/fluid/FluidResource.java` (added equals/hashCode + Objects import)
- `resources/common/src/main/java/earth/terrarium/common_storage_lib/resources/entity/EntityResource.java` (added equals/hashCode + Objects import)
