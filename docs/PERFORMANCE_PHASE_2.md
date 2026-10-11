# Winlator Enhanced — Performance Phase 2 (Winlator 11.2)

This is a **stacked branch** based on `feature/winlator-enhanced-profiles-11` (PR #2). The Winlator upstream `app` submodule remains pinned, with a fail-closed overlay applied by `scripts/apply_app_overrides.py`.

## Implemented changes

### 1. Sample content-update jitter in the Full HUD

`FrameTimeTracker` stores the latest 240 frame-content update intervals in a bounded ring, with O(1) writes and no allocations in the hot path. It calculates mean, p95 and p99 **only when requested**, not on each update. A pause exceeding two seconds resets the history. The Full HUD displays `update p95 XXX.Xms` beside the existing FPS estimate.

**Scientific limitation:** `FrameRating.update()` is called for window content updates, **not necessarily for GPU presents/vsync frames**. This is a proxy for content-update irregularity, NOT a GPU frametime monitor. Compare it only alongside observed stutter and DXVK's native HUD. It cannot measure missed frames that do not produce content updates or distinguish CPU and GPU bottlenecks.

### 2. Optional Box64 DynaCache profiles

The quick-profile picker now includes:
- **Box64 – enable 512 MiB JIT cache (experimental):** `BOX64_DYNACACHE=1`, `BOX64_DYNACACHE_LIMIT=512`.
- **Box64 – disable JIT cache:** `BOX64_DYNACACHE=0`, remove the limit override.

It does not change the Box64 CPU preset, and existing environment variables are preserved. Caching compiled code may reduce repeated-run translation overhead, but **first-run performance can be worse**, cache entries may be invalidated, cache space is consumed, and specific Box64 binary builds may behave differently. No cache is forced by default. Disabled does not delete previously stored cache files.

See the current [Box64 DynaCache documentation](https://github.com/ptitSeb/box64/blob/main/docs/USAGE.md), including size limits and changing defaults across versions. Run on a device and check actual Box64 build behavior.

### 3. Prevent unnecessary DXVK config writes

`DXVKConfigDialog` previously called `FileUtils.delete(dxvkConfigFile)` before writing the regenerated text on each setup. We now compare the desired string to the current file and only rewrite on differences or when missing. The file remains usable after identical launches; output is equivalent when valid.

This reduces disk I/O during *setup* and avoids a transient missing-config window. **It does not promise additional runtime FPS.** The wrapper still generates the same DXVK settings.

### 4. Thermal state and power-saver warning

When the user explicitly selects **Performance** or **Box64 JIT cache**, the picker queries Android's read-only `PowerManager` battery-saver and (API 29+) thermal-state APIs. It warns about moderate-or-higher thermal status, where a resolution decrease or frame cap may help. It **never disables thermal protection**, changes frequencies, grants elevated privilege, or touches governor controls.

## Tests and validation

- Java unit tests: known content-update intervals, p95/p99 tails, history eviction, 2-second pause reset, input changes requiring DXVK writes, identical config skip.
- Java unit tests: Box64 cache-on/cache-off, 512 MiB limit, persistence of other variables and no Box64 preset switch.
- GitHub Actions: patches the pinned upstream, checks idempotence, runs both tests, compiles the Android debug APK.
- Device tests **still needed** for actual Mali/Adreno frame pacing, cache hit ratio, thermals, startup speed, disk space and regressions.

## A/B benchmark protocol

1. Keep the **same game version, APK, Android settings, drivers, container resolution and Box64 preset**.
2. Restart the same game and play the same scene for at least 3 runs per variant. First-run runs may include cold cache costs.
3. Test cache **off → on → off**, separately recording cold launch duration and repeated launch duration; don't compare an empty cache to a warmed cache.
4. For each run, collect at least: FPS (native DXVK HUD if DXVK game), observed hitch count, **content-update p95** from Full HUD, Android device temperature / thermal-state if available, and duration.
5. Avoid confounding variables: screen recording, charging status, battery saver, shader warmup, room temperature, render resolution, vsync and background downloads.
6. Compare distributions and report scene, duration, means/medians, not a single arbitrary FPS score.

### What is not implemented

FEXCore swap, full Valve Proton runtime, driver recompile, root-only CPU governor tuning, GameHub code reuse and proprietary shader libraries are **not** included. These require their own integration, licensing and system-level compatibility work.

## Upstream sources

- [Box64 – configuration / DynaCache](https://github.com/ptitSeb/box64/blob/main/docs/USAGE.md)
- [DXVK – configuration](https://github.com/doitsujin/dxvk/blob/master/dxvk.conf)
- [DXVK – per-app config](https://github.com/doitsujin/dxvk/wiki/Configuration)
- [FEX emulator architecture](https://github.com/FEX-Emu/FEX)
- [Android PowerManager](https://developer.android.com/reference/android/os/PowerManager)

**Reminder:** debug APK package ID may conflict with the original Winlator; back up your container data before installing.
