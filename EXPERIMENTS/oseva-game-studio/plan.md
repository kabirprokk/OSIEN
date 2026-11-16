# OSEVA Game Engine — Build Plan

## Purpose

Build a reliable, cross-platform 2D engine for OSEVA Game Studio and use it to ship:

1. Neon Pulse
2. Orbit Sling
3. The OGS visual level designer

The engine must run on Windows and Linux, use the OGS visual identity, and support both standalone builds and portal builds.

## Non-negotiable rules

- Build one phase at a time.
- Never continue while the current phase has failing acceptance tests.
- Every bug fix must receive a regression test.
- No absolute `F:\` paths in runtime code.
- Use relative paths and platform-safe path joining.
- Logical game resolution is 960×540.
- Physics uses a fixed 120Hz timestep.
- Input uses pointer-safe behavior with no double-firing.
- Transparent PNGs must load correctly.
- Saves must survive reload and tolerate corruption.
- No console errors or unhandled crashes.
- OGS palette and signature style are locked by `OGS-SIGNATURE-STYLE.md`.
- Gold is reserved for rewards and red is reserved for hazards.

## OGS Studio/editor visual direction

The Studio and no-code editor UI must be based on the visual language of the official intro video:

- Charcoal-to-black cinematic background
- Soft overhead spotlight with a controlled falloff
- Subtle monochrome film grain/noise
- White and cool-gray geometric controls
- OGS octagon and geometric game-piece language
- Spacious layouts with strong alignment
- Thin hairline borders and restrained shadows
- Smooth fades, quiet scale transitions, and no noisy motion
- Clear hierarchy so a new user can build without coding

Studio/editor base tokens:

```text
studio-bg-deep: #080808
studio-bg:      #141414
studio-panel:   rgba(28,28,28,.88)
studio-panel-2: rgba(44,44,44,.92)
studio-ink:     #F4F4F2
studio-dim:     #B7B7B2
studio-faint:   #777772
studio-line:    rgba(255,255,255,.12)
studio-focus:   #F4F4F2
studio-danger:  #D95757
```

The editor should feel like a professional creative tool, not like a game HUD:

- Left side: searchable object and asset library
- Center: uncluttered canvas with clear selection handles
- Right side: contextual properties inspector
- Top: project, scene, play, undo, redo, save, and build controls
- Bottom: status, snapping, coordinates, warnings, and timeline state
- Every complex action includes a visible preview and undo path
- Every empty state explains the next action in plain language
- Tooltips explain controls without requiring documentation

The Studio/editor theme and the game theme are deliberately separated:

- Studio/editor: intro-video monochrome cinematic theme
- Games: locked OGS gameplay palette with cyan, violet, pink, gold, and hazard red

The official intro remains mandatory at the beginning of every game build. The Studio itself may use a shorter logo fade on editor startup, but the game intro must remain unchanged.

## Mandatory intro requirement

Every game build must show the official OGS intro whenever that game is launched.

Source asset:

```text
OGS Logos/oseva game studio in-game intro .mp4
```

Properties:

- 1920×1080
- 5 seconds
- H.264
- 30 FPS
- No audio
- Approximately 5 MB

Each game package should expose it through a normalized relative path:

```text
assets/branding/ogs_intro.mp4
```

Startup flow:

```text
launch game
→ initialize window and renderer
→ load intro
→ play OGS intro
→ optional skip input
→ fade to that game's main menu
```

Intro rules:

- Plays on every fresh game launch.
- Does not replay on level restart, death, pause, or menu navigation.
- Keyboard, mouse, and pointer input may skip it.
- Intro input must not leak into the main menu.
- Missing or unsupported video must never prevent the game from starting.
- Fallback is a native rendered OGS logo animation.
- Startup must work offline.
- Standalone builds and portal builds must both include the intro.

## Recommended technology

- C++20
- SDL3 for windowing, input, audio, and platform support
- OpenGL first through a renderer abstraction
- Dear ImGui for the editor
- CMake for builds
- JSON for projects, scenes, levels, and saves
- Box2D only if the custom physics layer becomes insufficient

## Target directory structure

```text
F:/OSEVA Game Engine/
  plan.md
  CONTEXT_SUMMARY.md
  OGS-MASTER-PROMPT.md
  OGS-SIGNATURE-STYLE.md
  OGS-ASSET-SIZES.md
  OGS Logos/
  Assets/
  engine/
    core/
    platform/
    renderer/
    input/
    audio/
    assets/
    scene/
    physics/
    animation/
    particles/
    ui/
    editor/
    tools/
    tests/
  games/
    neon-pulse/
    orbit-sling/
  projects/
  docs/
  packs/
  build/
```

## Phase 0 — Lock the contract

Define and document:

- Supported platforms
- Compiler versions
- Coordinate system
- Logical resolution
- Fixed timestep behavior
- Input actions
- Asset formats
- Scene format
- Save format
- Error handling
- Intro behavior
- Build and release rules

Acceptance:

- Architecture decisions are written down.
- No core technology decision is left ambiguous.
- The intro is defined as mandatory for every game launch.

## Phase 1 — Repository and build foundation

Create the directory structure, CMake project, debug/release configurations, test runner, formatting rules, static analysis, logging, and crash reporting.

Acceptance:

- Clean Windows build succeeds.
- Clean Linux build succeeds.
- Empty executable opens and closes safely.
- Tests run from a clean checkout.

## Phase 2 — Application and platform layer

### OGS gameplay differentiators locked into the engine

The engine is not a generic sprite-and-collision toolkit. Its core advantage is the combination of:

- Runtime OGS sprites: pattern textures wrapped in a dark core, hot center, colored aura, ink ring, and controlled breathing.
- Deterministic 120Hz physics with gravity wells, inverted gravity, orbital forces, and replay-safe stepping.
- A future mechanic layer for time distortion, gravity reversal, portals, orbit changes, slings, and spatial puzzles.
- Bounded simulation steps so fullscreen, low FPS, and high refresh-rate displays never change gameplay outcomes.

These systems are engine primitives and must be reusable by every future OGS game.

Implement:

- Window creation
- Window resizing
- DPI awareness
- Fullscreen/windowed mode
- High-resolution timing
- Application lifecycle
- Fixed-timestep game loop
- Frame statistics
- Intro playback state
- Intro skip state
- Intro-to-menu transition

Acceptance:

- Window uses 960×540 logical coordinates.
- Rendering remains proportional during resize.
- Fixed updates remain stable at different frame rates.
- Every test game can launch into its intro and then its own menu.
- Skip input works exactly once.

## Phase 3 — Core utilities

Implement vectors, rectangles, circles, transforms, colors, matrices, handles, events, logging, assertions, random seeds, time services, and safe memory helpers.

Acceptance:

- Math unit tests pass.
- Invalid handles fail safely.
- Deterministic random seeds reproduce identical results.
- Core code does not depend on the renderer.

## Phase 4 — Rendering foundation

Implement cameras, textures, sprites, primitives, text, layers, z-order, blending, render targets, scaling, and OGS glow effects.

Acceptance:

- Test scene renders at least 1000 sprites smoothly.
- Transparent PNGs work.
- High-DPI rendering works.
- Repeated texture loading does not leak memory.
- OGS gradient, grid, vignette, aura, ring, and breathing effects work.

## Phase 5 — Asset pipeline

Implement asset registry, logical asset IDs, texture/font/audio/video caches, validation, missing-asset warnings, duplicate detection, and the normalized intro asset path.

Register:

- 17 Grid Soul player patterns
- 6 planets/gravity wells
- 5 obstacles
- OGS logos
- OGS intro video

Acceptance:

- Assets load by logical name.
- Invalid assets produce useful warnings.
- Missing intro uses the native logo fallback.
- Intro video is included in every game build.

## Phase 6 — Entity and scene system

Implement entities, components, transforms, sprites, colliders, rigid bodies, cameras, animators, particle emitters, audio sources, scripts, tags, layers, parent-child relationships, scene creation, and scene switching.

Acceptance:

- Entities can be created, moved, hidden, and destroyed.
- Scenes can be saved and reopened.
- Scene reload is deterministic.
- Destroyed entities cannot be accessed.

## Phase 7 — Serialization

Implement versioned JSON formats for scenes, levels, settings, skins, and saves.

Acceptance:

- Corrupt files do not crash the engine.
- Unsupported versions produce clear errors.
- Exported levels load without editor code.
- Serialization tests pass.

## Phase 8 — Collision and physics

Implement AABB, circle, circle-rectangle, triggers, static bodies, dynamic bodies, restitution, gravity wells, collision layers, and collision masks.

Acceptance:

- Collision edge-case tests pass.
- Thin obstacles do not tunnel under normal conditions.
- Physics is deterministic at 120Hz.
- Fixed-seed replay produces the same result.

## Phase 9 — Input system

Implement action-based input for keyboard, mouse, pointer, touch-ready input, rebinding, input buffering, focus changes, and one-action-per-pointer-press protection.

Acceptance:

- One tap produces one action.
- Mouse and pointer behavior match.
- Focus loss does not leave stuck input.
- Input tests pass on both games.

## Phase 10 — Animation, particles, and game feel

Implement keyframes, easing, breathing, rotation, sprite animation, trails, particles, screen shake, slow motion, hit flashes, death bursts, reward bursts, and camera follow.

Acceptance:

- Effects are frame-rate independent.
- Pause freezes gameplay correctly.
- Slow motion does not break physics.
- Particle memory remains bounded.

## Phase 11 — Audio

Implement sound effects, music channels, volume groups, mute state, procedural beeps, pause behavior, and ad-mute integration.

Rules:

- First sound occurs only after user interaction.
- Audio mutes during ads.
- Missing audio never crashes the game.

Acceptance:

- Mute persists after reload.
- Audio resumes correctly after pause and ads.

## Phase 12 — UI framework

Implement panels, buttons, tabs, labels, counters, toasts, progress bars, sliders, modals, tooltips, and confirmations.

Acceptance:

- UI scales correctly.
- Pointer and keyboard navigation work.
- Disabled controls remain visible.
- No layout jumping occurs.

## Phase 13 — Editor foundation

Implement project browser, scene browser, hierarchy, inspector, asset browser, viewport, gizmos, snapping, undo/redo, copy/paste, multi-select, save/load, and play/stop mode.

Acceptance:

- A scene can be created without editing code.
- Undo/redo is reliable.
- Play mode cannot corrupt the edited scene.

## Phase 14 — Advanced level designer

Implement the OGS designer with:

- Neon Pulse profile
- Orbit Sling profile
- Automatic game-type configuration
- Object palettes
- Property editing
- Background controls
- Sprite stamps
- Parametric obstacle builders
- Animation timeline
- Fairness validation
- Play preview
- JSON export
- Send-to-game workflow
- Local autosave
- Import/export

Acceptance flow:

```text
open designer
→ choose game
→ place five objects
→ animate one object
→ play preview
→ export level
→ load level in game
```

## Phase 15 — Save data and progression

Implement versioned persistence for coins, stars, scores, skins, missions, progress, settings, and unlocks.

Acceptance:

- Valid progress survives reload.
- Corrupt saves recover safely.
- Rewards cannot be paid twice.
- Coin ledger is delta-based.

## Phase 16 — OGS platform SDK

Implement initialization, loading state, gameplay state, happytime, rewarded ads, midgame ads, retries, failures, simulated ads, offline mode, and audio muting.

Acceptance:

- Missing SDK never crashes the game.
- Ad failure keeps gameplay available.
- Simulated ads are clearly labeled.
- Game state restores correctly after ads.

## Phase 17 — First complete vertical slice

Build one tiny playable slice:

```text
launch
→ OGS intro
→ main menu
→ start level
→ move player
→ collide
→ die
→ restart
→ collect reward
→ finish level
→ pause
→ save
→ quit
→ reopen
```

Acceptance:

- Works on Windows and Linux.
- Intro works on every launch.
- Missing intro does not block startup.
- No developer tools are required.

## Phase 18 — Neon Pulse

Implement one-touch gravity flipping, endless mode, level mode, zen mode, 30 levels, finish gates, Grid Soul abilities, missions, combos, revive, rewind, shop, pause, and fullscreen.

Acceptance:

- Menu → play → die → recover → win → shop → zen → pause passes.
- Every level is finishable.
- Touch never double-fires.
- The OGS intro appears whenever Neon Pulse launches.

## Phase 19 — Orbit Sling

Implement drag-back sling, release, gravity wells, bouncy planets, spike hazards, goal rings, aim preview, par scores, stars, skin abilities, and voluntary reward doors.

Acceptance:

- Tutorial levels teach one mechanic at a time.
- Aim preview matches gameplay physics.
- Mobile input passes.
- The OGS intro appears whenever Orbit Sling launches.

## Phase 20 — Automated quality system

Add permanent tests for:

- Core math
- Physics
- Serialization
- Input
- Saves
- Assets
- Intro startup
- Video fallback
- Memory usage
- Window resize
- High-DPI rendering
- Low-FPS behavior
- Offline mode
- Replay determinism

Every bug must include a reproduction test before it is closed.

## Phase 21 — Packaging and release

Create Windows and Linux packages, portal ZIPs, build verification, asset manifests, version metadata, crash logs, and clean-machine launch tests.

Acceptance:

- Clean-machine installation works.
- No runtime absolute paths exist.
- Both games include and play the intro.
- Builds are reproducible.
- Final ZIPs contain only required portal files.

## Tomorrow — exact starting checklist

1. Read `CONTEXT_SUMMARY.md`.
2. Read `OGS-MASTER-PROMPT.md`.
3. Read `OGS-SIGNATURE-STYLE.md`.
4. Confirm the intro video and official logo files.
5. Choose and document the C++20/SDL3/CMake architecture.
6. Create the Phase 1 directory structure.
7. Create the initial CMake project.
8. Build a blank Windows executable.
9. Add the first automated test.
10. Update `CONTEXT_SUMMARY.md`.
11. Stop and record acceptance evidence before continuing.

## Definition of done

The project is complete only when:

- Both games build for Windows and Linux.
- Every game launch shows the OGS intro.
- The editor can create and export levels.
- Every ZIP can be rebuilt by script.
- Saves, ads, input, physics, and rendering pass their tests.
- No known release-blocking bugs remain.
- `CONTEXT_SUMMARY.md` accurately records the current state.
