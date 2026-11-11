# Public GGTU feature research and VGTU implementation map

This document uses only publicly available project descriptions and changelogs. GGTU's project page declares the project ARR, so VGTU does not decompile, copy, or redistribute GGTU implementation code or assets.

## What GGTU publicly advertises

The current public project description identifies three central functions:

1. **Temporal upscaling** — the image is rendered at a lower internal resolution and reconstructed using MEASU plus temporal accumulation.
2. **Smart reconstruction** — reconstructs a full-resolution image from a smaller render target to reduce shader GPU work.
3. **Frame generation** — advertises x2, x3, x4, x5, and x6 multipliers.

Public version changelogs for the 5.x line additionally describe:

- a more flexible render-scale slider;
- warm and night-vision colour-correction modes;
- improved generated-frame quality;
- disabling supported shader packs' own jitter, upscalers, and anti-aliasing when the mod is active, to avoid stacking temporal reconstruction;
- improved cloud/fog grain and Distant Horizons LOD anti-aliasing;
- an automatic frame-generation multiplier chosen from scene load;
- high-quality and simplified frame-generation modes;
- improved first-person hand smoothness and entity handling;
- monitor-refresh-limited and unlimited presentation modes;
- Shift+U to toggle upscaling and Shift+G to toggle frame generation.

**Version caveat:** the above list is assembled from the current GGTU project description and public 5.x changelogs. The public 26.3 NeoGGTU 1.1.3 release page does not provide a changelog, so those older 5.x notes should not be treated as proof that every detail is present in the supplied NeoGGTU 1.2.2 JAR.

## VGTU mapping

| Public feature | VGTU plan |
|---|---|
| Low-resolution rendering | Use Vitrail's existing Vulkan render-scale target. Do not create a second target-swap pipeline. |
| Spatial image reconstruction | Initially rely on Vitrail's own EASU/RCAS path. Benchmark a separate open implementation before replacing it. |
| Temporal reconstruction | Validate Vitrail's optional history pass; later evaluate an independently implemented FSR 2-style path with depth, motion, jitter, disocclusion rejection, and reactive masks. |
| Frame generation x2–x6 | Not in the first build. A real implementation needs motion estimation, hand/entity/UI handling, a Vulkan presentation design, and latency testing. |
| Automatic quality / scene response | First build has adaptive render-scale control using observed frame time. A later version can add GPU timing and scene-load-aware FG selection. |
| High-quality / simplified FG | Future, after x2 interpolation is implemented and evaluated. |
| Warm / night-vision colour modes | Future optional post-processing settings; no visual effects are changed by default. |
| Shader-pack AA/jitter coordination | Future per-pack compatibility layer. Must not blindly rewrite arbitrary shader packs. |
| Entity and hand improvements | Future motion-field coverage. Vitrail's camera-only vectors are insufficient for correct object interpolation on their own. |
| Distant Horizons / Voxy | Compatibility testing after the core image pipeline is stable. |
| Monitor-limited / unlimited presentation | Future, only after a safe presenter exists. |
| Hotkeys | Commands exist in the first milestone; keybinds and settings screen are later UI work. |

## Why frame generation is postponed

Repeating frames can make an FPS counter look larger but is not interpolation. A real generated frame needs to account for the current and previous images, motion, depth/occlusion changes, animated geometry, transparency, first-person hand, particles, and UI. It also needs a present/pacing method that doesn't advance game simulation or create excessive input latency. VGTU will not claim frame generation until that whole path works.

## Vitrail's existing responsibilities

Vitrail is the Vulkan shader engine and already owns the scaled world target, the render-scale setting, spatial upscaling, depth, and camera motion information. It also has temporal-accumulation code in its current public source tree. VGTU's initial job is to manage Vitrail's scale more intelligently and probe optional hooks safely, not compete with its render-target lifecycle.

## Sources

- [GGTU public project page](https://modrinth.com/project/ggtu)
- [GGTU 5.0.0 changelog](https://modrinth.com/mod/ggtu/version/o9nsWZ2O)
- [GGTU 5.0.1 changelog](https://modrinth.com/mod/ggtu/version/jNl3Bn28)
- [GGTU 5.0.2 changelog](https://modrinth.com/mod/ggtu/version/5.0.2%2Bmc1.21.11)
- [GGTU 5.1.0 changelog](https://modrinth.com/mod/ggtu/version/5.1.0%2Bmc1.21.11)
- [GGTU NeoGGTU 1.1.3 for Minecraft 26.3](https://modrinth.com/mod/ggtu/version/Neo-1.1.3%2Bmc26.3)
- [Vitrail 0.13.0-beta for Minecraft 26.3](https://modrinth.com/mod/vitrail-shaders/version/Q3nKuPKJ)
- [Vitrail source repository](https://github.com/avpbynf/Vitrail-Shaders)
- [Fabric Minecraft 26.3 toolchain guidance](https://fabricmc.net/develop/)
- [AMD FidelityFX SDK techniques](https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK/tree/main/Kits/FidelityFX/docs/techniques)
