# VGTU architecture notes

## Integration boundary

Vitrail owns the render target swap and shader-pack render chain. Its current source exposes:
- `dev.vitrail.render.RenderScale.wanted(int)` to choose the percentage used for the world target;
- an optional reflective probe for `dev.vitrail.render.TemporalAccumulation.setWanted(boolean)`. That class/method is not treated as a stable or guaranteed part of the pinned Vitrail release.

VGTU pins the first build to Vitrail 0.13.0-beta. Those methods are visible in the source but are not a declared stable third-party API. Update the integration adapter and re-test after a Vitrail update.

## Why not implement another low-resolution target yet?

Two mods swapping `GameRenderer`'s main target or independently changing its dimensions can corrupt the frame, depth, outline target, UI boundary, or resize recovery. Vitrail already has the difficult lifecycle code for its Vulkan-backed world target. VGTU therefore sends scale requests through Vitrail instead of maintaining a second target.

## Temporal input problem

A high-quality temporal upscaler requires more than a previous colour frame:
- motion vectors in the correct current-to-previous convention and pixel units;
- depth with the same projection/depth convention as the frame;
- camera jitter and correct previous/current transforms;
- disocclusion handling so newly revealed surfaces do not reuse stale history;
- reactive/transparency handling for particles, glass, water, foliage, and animated shader effects.

Vitrail's source includes camera-derived motion vectors and an optional temporal fold. Its motion-vector source explicitly distinguishes camera motion from per-object motion: moving mobs and other independently moving geometry need their own previous transforms for complete vectors. VGTU keeps that feature opt-in until it has a tested per-object solution.

## Adaptive controller

The controller observes `GameRenderer.render()` wall-clock duration and uses p90 over 120 frames. This is not a GPU timestamp. Scale changes occur slowly and only after repeated windows outside the target.

After a downscale, the controller waits 180 frames and compares p90 with the prior measurement. Two downscales with less than 4% improvement cause the controller to pause, restore its starting scale, and report a likely CPU/presentation limit. The heuristic is intentionally conservative.

## Frame generation

Frame generation needs a separate presentation design. The game must display generated frames without advancing game simulation, while keeping input latency, UI and hand layers, camera motion, entity motion, particles, and newly visible geometry coherent. Simply repeating an image increases the displayed frame count but is not real interpolation.

VGTU does not claim frame generation is implemented in this milestone.
