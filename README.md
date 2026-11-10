# VGTU — Vulkan Graphics Temporal Upscaler

VGTU stands for **Vulkan Graphics Temporal Upscaler**. It is an experimental project exploring a Vulkan-based rendering path for Minecraft, with Vitrail as the intended Vulkan integration point.

> **Status: Pre-alpha. No public, installable Minecraft build is available yet.** The project is under development. Do not treat this repository as a finished Minecraft mod or assume temporal upscaling is already working.

## What is VGTU?

VGTU is intended as a Vulkan-oriented fork/port of GGTU for Minecraft 26.3. The goal is to move rendering work that expects an OpenGL/Iris environment toward Minecraft's Vulkan rendering path through Vitrail.

The name describes the project's intended direction:

- **Vulkan Graphics** — use Vulkan's explicit graphics pipeline and resource-management model.
- **Temporal Upscaler** — investigate temporal image reconstruction: rendering at a lower internal resolution and using information from previous frames to reconstruct a higher-resolution output.

### Why Vulkan?

Vulkan provides explicit control over GPU resources, command submission, pipelines, descriptors, image layouts, and synchronization. This makes it a natural target for a renderer designed to operate on a native Vulkan path, rather than trying to attach an OpenGL-only pipeline to it.

Vulkan does **not** automatically make a game faster. Performance depends on the implementation, GPU, driver, and workload. VGTU needs real-world testing before any performance claims can be made.

## What is temporal upscaling?

A temporal upscaler reconstructs a higher-resolution image using the current frame together with information retained from previous frames. A robust implementation generally needs inputs such as motion information, depth, and a history buffer, plus techniques to reduce ghosting and other reconstruction artefacts.

**Important:** temporal upscaling is part of VGTU's intended direction and name. It is not confirmed as implemented in the current project. The name should not be read as a claim that the feature is already available.

## Intended project targets

The current project direction describes:

- Minecraft **26.3**
- Vulkan rendering through **Vitrail**
- A Vulkan-side port of rendering paths that currently depend on OpenGL
- Investigation of OptiFine-style shader-pack compatibility
- Future evaluation of temporal upscaling

Compatibility, stability, and performance remain experimental. Check the repository's Issues and Releases pages for updates.

## Development status

VGTU is pre-alpha. Work to validate and complete includes:

1. Confirm Vitrail integration points and supported Minecraft/mod-loader versions.
2. Port and validate rendering passes on Vulkan.
3. Test shader-pack loading and rendering behaviour.
4. Validate resizing, depth conventions, synchronization, and frame-history handling.
5. Implement and evaluate temporal reconstruction before describing it as a working feature.
6. Publish an installable build only after it can be built and tested.

Bug reports and suggestions are welcome through [GitHub Issues](https://github.com/sSunixX/VGTU/issues).

## Development tools and AI assistance

This project was made with assistance from **ChatGPT (OpenAI)** and **Base44**.

- **ChatGPT** was used as an AI assistant for planning, explanations, problem-solving, and code-related assistance.
- **Base44** was used to help create/scaffold the project's web interface and application structure.
- The project owner directed the work and is responsible for reviewing decisions, checking generated output, and testing the project.

This disclosure is included to be transparent about the tools used. AI-assisted output can be incorrect or incomplete; code and technical claims still need review and testing.

## Releases and downloads

There are currently **no published installable VGTU releases**. The repository contains development materials, not a confirmed Minecraft-ready download.

When a tested build is available, it will be published on the [GitHub Releases page](https://github.com/sSunixX/VGTU/releases), with its supported Minecraft version, mod loader, dependencies, installation steps, and known limitations.

Do not attach the source ZIP as though it were a compiled Minecraft mod. A game-ready artifact must be built and tested separately.

## Credits and third-party projects

VGTU's development direction references GGTU and the Vitrail renderer. Their names, code, and assets remain subject to their respective authors' licences and terms. Compatibility does not imply endorsement.

## License

No license has been declared for this repository. Until a license is added, do not assume that the code is licensed for reuse, redistribution, or modification.
