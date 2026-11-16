# OSEVA GAME STUDIO (OGS) — MASTER BUILD PROMPT v1
### The single bible any AI follows to build the entire studio project step by step, perfectly.
### Workspace root: `F:\OSEVA Game Engine\` — EVERY project file lives here. Nothing elsewhere.

> HOW TO USE: paste this whole file to your AI, then say "execute PHASE 0".
> The AI executes ONE phase at a time, proves each acceptance test, then stops
> and waits for "next phase". Never skip phases. Never invent new folders.

---

## 0. STUDIO IDENTITY (locked, applies to everything)

- **Name:** OSEVA Game Studio. **Short:** OGS. **Voice:** glowline arcade.
- **Rule:** when someone opens any OGS game and sees the style, they must know it is OGS.
- **Palette (never add colors without approval):**
  ink `#EEF2FF`, dim `#9AA3BB`, cyan `#00F0FF`, violet `#7B2FFF`,
  pink `#FF2BD6`, gold `#FFD964` (reward ONLY), red `#FF2B2B` (hazards ONLY).
- **Signature motifs:** deep-space gradient canvas + faint grid + vignette;
  glowline bodies (dark core, white-hot center, aura, hairline ring);
  gold = reward, checkers = finish (used once per level, never decoration),
  dotted lines = future/preview; Orbitron for numbers/titles, Rajdhani for body.
- **Motion:** everything breathes (`sin(t*3.2)*0.04`); death = slow-mo + burst,
  never a hard cut. **Audio:** procedural beeps only, first sound only after a
  user gesture, always muted during ads.

## 1. DIRECTORY MAP (create exactly this, no extras)

```
F:\OSEVA Game Engine\
  OGS-MASTER-PROMPT.md        (this file)
  CONTEXT_SUMMARY.md          (living state: what is done, what is next)
  studio\
    STUDIO.md                 (identity guide)
    sprites.js                (shared signature bodies: orb x8, planet, spike,
                               coin, mote, goalRing, gate, logo — zero deps)
    ogs-sdk.js                (CrazyGames SDK v3 bridge, shared by ALL games)
    level-designer.html       (THE ADVANCED DESIGNER — spec in section 7)
    OGS Logos\                (OFFICIAL OGS MARKS — use as-is, never redraw/restyle)
      2.png                   PRIMARY mark: octagon + game-piece cutouts, 512x512
      3.png                   horizontal lockup (OGS + mark), 512x512
      1.png                   stacked lockup (large sizes only, tagline unreadable small), 512x512
  games\
    neon-pulse\               (Game 1: one-touch gravity flipper)
      index.html, styles.css, sprites.js(copy), game.js, ogs-sdk.js(copy),
      boot.js, levels.json, README.md, SUBMIT.md, build.ps1, covers\
    orbit-sling\              (Game 2: drag-release slingshot puzzles)
      (same file set as neon-pulse)
  packs\
    <game>-crazygames.zip     (portal uploads are built here by build.ps1)
```

## 2. TECH RULES (non-negotiable)

1. Vanilla HTML5 + CSS + JS only. Zero npm dependencies, zero frameworks, zero build step.
2. Canvas logical size 960x540 everywhere; DPR-aware (`min(devicePixelRatio,2)`); CSS scales it.
3. Fixed timestep 120Hz accumulator, max 8 steps, dt clamp. Pooled particles (no per-frame allocation).
4. All game-internal links are relative (`./game.js`). External URLs allowed: Google Fonts (with system fallback) + `https://sdk.crazygames.com/crazygames-sdk-v3.js` (never self-host the SDK).
5. Total game < 50MB initial (target < 200KB). Files per game <= 1500 (target < 10).
6. Touch = pointer events ONLY (`pointerdown/move/up`); touchstart fallback only `if (!window.PointerEvent)` (double listeners cause double-actions on mobile).
7. `touch-action:none` on stage/canvas; preventDefault only for gameplay touches, never for buttons.
8. localStorage everywhere wrapped in try/catch (private mode safe). Best/coins/skins/unlocks must survive reload.
9. No external ads, no external logins, no paywalls. PEGI-12 safe. Must run with AdBlock on.
10. Every file starts with a header comment (name, version, purpose). No dead code, no console errors.

## 3. MONEY RULES (how OGS earns)

1. **Voluntary ads only.** Player taps a labeled button fully knowing an ad plays. NEVER auto-play, NEVER midgame spam, NEVER punish ad failure (instant retry + coin fallback).
2. Standard doors: revive/continue, 2x reward, unlock/skip, +100 coins, shielded start. Coins-first is FORBIDDEN — ad first, coins on failure.
3. Demo/offline/AdBlock = clearly-labeled simulated ad (dashed gold card, "SIMULATED AD"), game stays 100% playable.
4. Real video ads serve ONLY on CrazyGames at Full Launch. In preview/QA the SDK returns `adsDisabledBasicLaunch` — that is EXPECTED, not a bug. Ship anyway.
5. Coin ledger must be delta-based (never pay twice for the same score). Persist wallet + owned skins + selected skin + unlocks + stars.

## 4. CRAZYGAMES CONTRACT (memorize)

- Script: `<script src="https://sdk.crazygames.com/crazygames-sdk-v3.js"></script>` in head.
- `ogs-sdk.js` bridge MUST: `init()` with 30s polling watcher (portal injects SDK async) + CDN self-inject fallback + replay of missed handshake; `loadingStart/Stop`, `gameplayStart/Stop` (gameplayStart re-fires loadingStop if missed); `happytime()` on records; `requestAd('rewarded'|'midgame', {adStarted, adFinished, adError})` with 1 retry + error-code surfacing; mute game audio during ads; never throw.
- QA panel must show: init, Loading Start/Stop, Gameplay Start/Stop, Happytime green. Ad dots stay dark until a real ad completes — fine for Basic Launch.
- Submission answers: engine HTML5, mobile ON, LANDSCAPE, multiplayer OFF, progress save = "Yes, LocalStorage", SDK-muting checkbox OFF, category + 5 tags + plain-text description/controls, covers 1920x1080 + 800x1200 + 800x800, videos MP4 <= 20s.
- Each game's `build.ps1` zips EXACTLY the portal files flat into `F:\OSEVA Game Engine\packs\`.

## 5. PHASES (execute in order, prove each, then stop)

### PHASE 0 — Foundation
Create directory map + CONTEXT_SUMMARY.md. Acceptance: tree matches section 1 exactly.

### PHASE 1 — Studio core
Write `studio/STUDIO.md`, `studio/sprites.js` (all 8 signature orb bodies + planet + spike + coin + mote + goalRing + gate + logo, zero deps, `window.OGS` API), `studio/ogs-sdk.js` (full section-4 bridge, `window.OGSSDK`, per-game UI hooks via `window.__ogsUI`), and verify the official `OGS Logos\` set by viewing it (2.png = primary mark).
Acceptance: `node --check` green on both JS files; logo viewed and on-brand.

### PHASE 2 — Game 1: NEON PULSE (one-touch gravity flipper)
Copy studio shell; write `game.js`: endless/level/zen modes, 30 levels with distance-based progress + golden finish gate at exactly 100%, 8 skins with cost-scaled abilities, coins/missions/combo, same-spot respawn with 3-2-1 countdown and HONEST rewind display, delta coin ledger, pause/fullscreen/zen-exit.
Acceptance: syntax green; manual playtest checklist (menu→play→die→revive→win→shop→zen→pause) all pass with zero console errors.

### PHASE 3 — Game 1 portal pack
Covers (3 exact sizes) + 2 preview MP4s (<=20s) + README + SUBMIT.md (copy-paste texts) + build.ps1 + zip in packs/. Acceptance: QA audit script passes (ids present, size << 50MB, no local paths).

### PHASE 4 — Game 2: ORBIT SLING (slingshot puzzles, reuses EVERYTHING)
New folder, same shell; write `game.js`: drag-back release, gravity wells (roaming variants later levels), bouncy planets/walls, spike hazards, gold GOAL ring, dotted aim preview with gravity simulation, par/stars persist, sling-tuned skin abilities, voluntary doors (2x, skip, +100).
Acceptance: same as Phase 2 + mobile single-tap verified (no double-fire).

### PHASE 5 — Game 2 portal pack (same as Phase 3).

### PHASE 6 — THE ADVANCED LEVEL DESIGNER (the whiteboard vision)
Build `studio/level-designer.html` EXACTLY per section 7. This is a flagship deliverable: no-code, multi-tab, auto-adaptive, play-preview. Acceptance: design→export→drop-in→playtest loop works end-to-end for BOTH games without touching code.

### PHASE 7 — No-code content pass
Using only the designer: 10 new levels per game, export, rebuild zips. Acceptance: new levels playable in-game, zero code edits.

### PHASE 8 — Submission & scale
Submit both games; log review feedback in CONTEXT_SUMMARY.md; fix within 24h; plan game 3 by cloning the shell.

## 6. PER-PHASE PROTOCOL (the AI must obey)

1. Read CONTEXT_SUMMARY.md first; update it last.
2. One phase per turn batch. After finishing: list files created, paste test evidence (command outputs), state the acceptance result, STOP.
3. If any acceptance fails: fix forward in the same phase, never borrow from the next phase.
4. Never edit a submitted/frozen build mid-review. Fixes go to a clearly-named next version.
5. Mobile single-tap correctness is a release blocker every phase (pointer-events-only rule).

## 7. ADVANCED LEVEL DESIGNER SPEC (from the OGS whiteboard — build exactly this)

### 7.1 Shell & tabs (top bar, three tabs — new browser tab per the vision)
- Tab 1 **PLAY PREVIEW**: runs the actual game build in an iframe/canvas with one click.
- Tab 2 **ENTER LEVEL DESIGNER**: the full editor below.
- Tab 3 (future): PUBLISH — reserved, disabled with "soon" tag. Do not build it in v1.

### 7.2 Auto game-type understanding (core requirement)
- On open, the designer reads the target game's manifest (`game.json`: `{game, genre, mechanics, units}`) and RECONFIGURES itself: object palette, property fields, validators, and preview physics all match that game automatically.
- Ship with two profiles: `neon-pulse` (slabs, gaps, orbs, movers, finish gate) and `orbit-sling` (pad, goal, planets, spikes, par). Switching profile swaps the whole UI with zero code changes.

### 7.3 Left panel — OBJECTS
- Shape gallery: per-profile placeable objects (slab, orb, planet, spike, pad, goal ring, finish gate, decoration).
- ADD BG: gradient presets + grid density + vignette slider (all studio-palette-locked).
- SPRITES: the 8 signature skins as draggable stamps; clicking one sets it as the level's preview body.
- CREATE OBSTACLES: parametric builders (slab row with gap, planet triangle, spike arc) with count/spacing sliders.

### 7.4 Center — CANVAS
- 960x540 design surface, zoom (50–200%), snap-to-grid toggle, drag-move everything, multi-select with shift, undo/redo (Ctrl+Z/Y), duplicate (Ctrl+D), delete key.
- Live rulers/guides: distance readouts, gap-width labels, par-time estimate per profile.
- PLAY PREVIEW button overlays a real playable test run of the current level instantly.

### 7.5 Right panel — PROPERTIES (selection-aware)
- SHAPE: type-specific numeric fields (x, y, w/h or r, gap, mass, par, orbit radius/speed).
- COLOR: studio palette swatches ONLY (custom hex locked behind "advanced" toggle).
- IMPORT IMAGE: bring PNGs as decorations; auto-downscaled, auto-credited in a manifest log.
- ANIMATIONS: mover paths (sine/h orbit, speed, amplitude), pulse, spin — with timeline scrubber.
- FUNCTION: what the object DOES (solid, bouncy + restitution, deadly, collectible + value, goal, checkpoint, decoration). Each function shows its gameplay consequence inline.

### 7.6 Validation & export
- FAIRNESS ENGINE: per-profile rules (e.g. gap always passable, goal reachable, no spawn-camping hazards) with inline red flags + one-click auto-fix suggestions.
- EXPORT: one `levels.js`/`levels.json` download + copy button + "Send to game" (writes straight into the game's folder and rebuilds its zip via its build.ps1).
- All designs autosave to localStorage; dirty-level counter; import/export JSON.

### 7.7 Acceptance for the designer
Fresh user flow with zero instructions: open → pick game profile → place 5 objects → set one animation → press PLAY PREVIEW and play it → export → game runs it. Any step needing docs = fail, simplify.

## 8. DONE DEFINITION (whole project)
Both games submitted, designer live per 7.7, every zip rebuildable by one script, CONTEXT_SUMMARY.md tells the full story, and a stranger can tell an OGS game blind. Then we plan game 3.
