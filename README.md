# VGTU — Vulkan Graphics Temporal Upscaler

**Minecraft Java 26.3 · Fabric · Sodium · Vitrail · Vulkan**

VGTU is an independent performance-control and temporal-rendering extension for Vitrail. It does not contain GGTU code, assets, or decompiled material. GGTU is treated only as a public feature reference.

## Current build status

**0.1.0-alpha.1 — source milestone.** This is the first source implementation, not a tested public release. The GitHub Actions workflow builds the JAR on GitHub so a local Gradle installation is not needed.

### Implemented in this milestone

- **Adaptive render scale:** samples the time spent inside `GameRenderer.render()` and adjusts Vitrail's render scale toward a configurable FPS target.
- **Manual render scale:** set a render scale directly from 25–100%.
- **Optional temporal integration probe:** attempts to enable Vitrail's temporal-accumulation method by reflection only if the installed Vitrail JAR exposes it. This is not treated as a guaranteed 0.13.0-beta API.
- **Stability controls:** hysteresis, cooldowns, a rolling 120-frame sample window, and a guard that pauses downscaling after repeated changes fail to improve frame time.
- **Commands:** live control and status readout.

This milestone intentionally delegates render-target ownership, spatial reconstruction, camera motion-vector generation, and temporal history to Vitrail. VGTU must not create a second render-scale system beside Vitrail.

## Commands

Open chat and run:

| Command | Action |
|---|---|
| `/vgtu` | Show current settings and frame-time p90 |
| `/vgtu auto on` | Enable adaptive render scale |
| `/vgtu auto off` | Disable adaptive render scale |
| `/vgtu target 60` | Set target FPS (30–240) |
| `/vgtu scale 67` | Manually set render scale (25–100%) and disable auto mode |
| `/vgtu temporal on` | Enable Vitrail's temporal accumulation |
| `/vgtu temporal off` | Disable temporal accumulation |
| `/vgtu reset` | Reset the controller's CPU/VSync-bound guard |

The settings are stored in `config/vgtu.properties`.

## How adaptive scaling works

A render scale of 67% means the world target is approximately 67% of the window width and height, so it contains about 45% as many pixels before reconstruction. That can reduce shader GPU work substantially in GPU-bound scenes.

VGTU measures the wall-clock duration of `GameRenderer.render()`. This is a **frame-time proxy**, not a GPU timestamp query. It can include CPU work, driver waits, and VSync. To avoid needlessly degrading image quality, VGTU:
- uses the 90th percentile of a rolling 120-frame window;
- only adjusts after several consecutive windows outside the target budget;
- waits between changes;
- checks whether a downscale actually improved frame time;
- pauses further downscaling after two unsuccessful probes.

If lowering the render scale does not improve frame time, the session may be CPU-bound or limited by VSync/presentation. VGTU will stop reducing image quality and report that suspicion. This is an estimate, not a definitive hardware bottleneck detector.

## Known limitations

- The adaptive scale only affects scenes where Vitrail's shader-pack render-scale path is active. It does not currently reduce vanilla rendering resolution without an active Vitrail pack.
- The temporal command is an optional reflection probe, not a guaranteed API. If the installed Vitrail JAR does not contain the method, VGTU reports that it is unavailable and keeps it off. If enabled, Vitrail's current source describes camera-derived motion vectors; moving entities may not have complete object-motion vectors and can show ghosting. Test it carefully.
- **Frame generation is not implemented in this first milestone.** x2–x6 generation requires a separate motion/optical-flow and presentation pipeline; it cannot be honestly delivered by merely duplicating frames.
- The controller does not yet read a GPU hardware timestamp. It must not be described as measuring pure GPU time.
- No performance numbers are claimed. Results depend on the GPU, shader pack, scene, render scale, VSync, and CPU bottlenecks.
- VGTU is pinned to Vitrail 0.13.0-beta for its first integration. Vitrail internals are not a stable external API, so later Vitrail versions may require an update.
- SIRIS may override Vitrail's render scale while DLSS is active. Disable DLSS when testing VGTU's adaptive scale.

## Requirements

- Minecraft Java **26.3**
- Fabric Loader **0.20.1 or newer**
- Fabric API **0.166.0+26.3**
- Sodium **0.9.x** for Minecraft 26.3
- Vitrail Shaders **0.13.0-beta+mc26.3**
- Java **25** for development/building

## Build without installing CMD tools locally

The repository includes `.github/workflows/build.yml`.

1. Upload this source tree to the `sSunixX/VGTU` GitHub repository.
2. Open the repository's **Actions** tab.
3. Select **Build VGTU**.
4. Press **Run workflow**.
5. Open the successful run and download the `vgtu-mc26.3` artifact.
6. Extract the artifact ZIP. The `.jar` inside is the mod.

A push to `main` also starts a build automatically.

For a feature-by-feature public GGTU research map and VGTU porting plan, see [docs/FEATURE_RESEARCH.md](docs/FEATURE_RESEARCH.md).

## Development roadmap

### Phase 1 — performance controller (this source milestone)
- [x] Vitrail render-scale integration
- [x] Automatic scale controller
- [x] Manual target and scale commands
- [x] Frame-time window and anti-oscillation
- [x] Conservative CPU/VSync-bound guard
- [x] Vitrail temporal-accumulation toggle

### Phase 2 — independent temporal reconstruction
- [ ] Validate the complete motion/depth/jitter input path with Vitrail and shader packs
- [ ] Add disocclusion rejection and history clipping
- [ ] Add reactive/transparency handling
- [ ] Evaluate an FSR 2/3-compatible Vulkan path with correct licence notices
- [ ] Benchmark against Vitrail's built-in EASU/RCAS and temporal fold

### Phase 3 — frame generation
- [ ] Investigate GPU optical flow or motion-field reconstruction
- [ ] Track first-person hand, entities, particles, and screen-space UI separately
- [ ] Integrate a Vulkan-compatible presentation/pacing path
- [ ] Add x2 first, then evaluate x3–x6 only if quality and latency are acceptable
- [ ] Auto multiplier and quality/simplified modes

### Phase 4 — optional quality features
- [ ] Per-pack temporal-AA coordination
- [ ] Optional colour modes
- [ ] Settings screen and performance overlay
- [ ] Keyboard shortcuts for scale and temporal options
- [ ] Distant Horizons/Voxy test matrix

## Public feature research reference

Public GGTU descriptions and changelogs list:
- temporal upscaling and MEASU-based reconstruction;
- lower internal render resolution and reconstruction;
- frame generation up to x2–x6 on the current project page;
- a render-scale slider;
- warm and night-vision colour modes;
- disabling supported shader-pack TAA/jitter/upscaling to avoid two temporal systems running at once;
- automatic frame-generation multiplier selection;
- high-quality and simplified generation modes;
- improved hand/entity handling and monitor-limited/unlimited presentation modes.

The GGTU project page states that its normal configuration requires Sodium and Iris. VGTU targets Vulkan through Vitrail instead.

## References

- [GGTU public project page](https://modrinth.com/project/ggtu)
- [GGTU 5.0.0 public changelog](https://modrinth.com/mod/ggtu/version/o9nsWZ2O)
- [GGTU 5.1.0 public changelog](https://modrinth.com/mod/ggtu/version/5.1.0%2Bmc1.21.11)
- [Vitrail Shaders](https://github.com/avpbynf/Vitrail-Shaders)
- [Vitrail 0.13.0-beta for Minecraft 26.3](https://modrinth.com/mod/vitrail-shaders/version/Q3nKuPKJ)
- [AMD FidelityFX Super Resolution 2 documentation](https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK/tree/main/Kits/FidelityFX/docs/techniques)
- [Fabric for Minecraft 26.3](https://fabricmc.net/2026/09/15/263.html)

## License

VGTU's original source in this repository is MIT-licensed. Vitrail remains a separate LGPL-3.0-only dependency and is not bundled into the VGTU JAR.
