# CONTEXT_SUMMARY.md — OSEVA Game Studio (OGS) living state
Last updated: 2026-10-09 — NEON PULSE + ORBIT SLING POWER: flip/sling runner, 9 objects, physics params, par/hint, templates. Windows rebuild green

## Identity (locked)
- Name: OSEVA Game Studio. Short: OGS. Voice: glowline arcade.
- Workspace root: `F:\OSEVA Game Engine\` — EVERY project file lives here.
- Palette: ink #EEF2FF, dim #9AA3BB, cyan #00F0FF, violet #7B2FFF, pink #FF2BD6, gold #FFD964 (reward ONLY), red #FF2B2B (hazards ONLY).
- Marks: `OGS Logos\` 2.png = PRIMARY, 3.png = horizontal lockup, 1.png = stacked (large only). 4.png = extra (unlisted, keep).
- APIs: `window.OGS` (sprites), `window.OGSSDK` (CrazyGames bridge), `window.__ogsUI` hooks, file `studio/ogs-sdk.js`.

## Assets — owner-locked 2026-10-09
- Players = GRID SOULS: 17 patterns in `Assets/Game Assets Player/` (1 crosses, 2 dot-star, 3 white grid, 4 orange grid, 5 folded, 6 faded, 7 pixel-diamond, 8 ornate, 9 circle-links, 10 dot-clusters, 11 checker-warp, 12 wavy net, 13 rainbow grid, 14 dotted wave, 15 halftone sphere, 16 green perspective, 17 blue burst). Each gets own ability/power. See STYLE amendment 2026-10-09 for runtime ring/aura rule.
- Planets/wells = 6 in `Assets/Game Assets Planets/` (blue grid, teal earth, dark moon, black swirl, red swirl, red eye). Need transparent export, 288x288.
- Obstacles/hazards = 5 in `Assets/Game Assets Obstacles/` (X-cross, starburst, crack-blob, triangle-trap, flash-star). Need transparent export, 160x160 max, red-only.
- Still to make (priority): slab cyan 184x1080, coin 96x96 x3, goal ring 288x288, finish gate 256x540, shield/checkpoint 144x144, FX 128x128, preview dot 32x32. Full 20-item list given 2026-10-09, not started.

## What is done
- 2026-10-08: Rebrand FER/FS/FGS → OGS complete (OGS-MASTER-PROMPT, OGS-ASSET-SIZES, OGS-SIGNATURE-STYLE). Grep 0 matches.
- 2026-10-09: GRID SOULS exception amended in STYLE §12. No code, no PHASE 0 build started.

## What is next — ON HOLD per owner
- Owner is starting REAL ENGINE APP (Linux + Windows). DO NOT start working until owner says go.
- Engine requirements noted for later: cross-platform paths (no `F:\` hardcode in engine code, use relative + OS join), Linux + Windows builds, transparent PNG loading, runtime OGS ring/aura/breathe for GRID SOULS, 960x540 logical, 120Hz fixed timestep, pointer + mouse.
- Web games (neon-pulse, orbit-sling, PHASE 0-8) paused. When resumed: PHASE 0 foundation per OGS-MASTER-PROMPT §1 still pending.
- Agents: git-pusher, model-creator, physics-engine, monitor on standby, no tasks delegated.

## Engine build status — 2026-10-09
- Windows-first Phase 1 scaffold is implemented and built.
- CMake 4.4.4 and MSYS2 GCC 16.1 are installed.
- `build/windows-msys/ogs_editor.exe` launches successfully.
- `build/windows-msys/ogs_core_tests.exe` passes: 1/1 test.
- Real `ogs_intro.mp4` playback is integrated into the Windows startup path with keyboard/mouse skip and native fallback.
- `OGS Logos/2.png` is loaded as the Windows application icon through the packaged `assets/branding/ogs_logo_primary.png` asset.
- Release package created at `dist/OSEVA Game Studio/` with bundled runtime DLLs, intro video, logo asset, and a desktop shortcut.
- Windows executable now embeds `OGS Logos/2.png` as a real application icon; desktop shortcut points to the packaged `.ico`.
- First real OGS Studio shell implemented: project browser, asset tree, canvas workspace, inspector, quick-start actions, toolbar, status bar, and clickable starter controls.
- Intro-to-Studio transition fixed by deferring Media Foundation teardown to the main event loop; the shell now redraws after video completion.
- Phase 2 started: fixed 120Hz clock, deterministic gravity-well physics, OGS sprite-style contract, and regression tests added. Windows phase-2 build passes 1/1 test.
- First live engine slice added: fixed-step application updates, clickable `NEW LEVEL` preview, packaged Grid Soul sprite loading, runtime aura/ring/breathe treatment, and gravity-well orbit simulation. Windows phase-2b build passes 1/1 test.
- 2026-10-09 fix: intro skip freeze — WndProc no longer tears down MFPlay inline (EVR deadlock left last frame stuck). Skip now: WndProc flags atomic pending + swallows ALL key/mouse/pointer/touch/wheel inputs, main-loop GetAsyncKeyState poll catches inputs MFPlay swallows, teardown runs only in pump_events with hide→stop→show + focus restore, 150ms input-leak guard, atomic_bool for MFP callback thread safety. Rebuilt ogs_editor.exe, tests 1/1 pass, dist synced.
- Build note: MSYS2 UCRT64 g++ requires C:\msys64\ucrt64\bin in PATH or cc1plus fails STATUS_DLL_NOT_FOUND (-1073741515) with silent Error 1. Windows CMake (C:\Program Files\CMake) + MSYS make is BROKEN (unix paths); use MinGW Makefiles + C:/msys64/ucrt64/bin/mingw32-make.exe. Fresh working dir is `build/windows-fix/` (MinGW); binaries synced back to `build/windows-msys/` + `dist/` for compatibility.
- 2026-10-09 PRODUCTION FIX (Codex handoff completed): placement mismatch ROOT CAUSE = browser runner camera followed player (`Math.max(0,player.x-180)`) while editor canvas is fixed 960x540 centered at 0,0. FIXED to `cameraX=0` + `translate(480,270)` so editor drag == RUN position 1:1. Added saved player-spawn (first ORB auto-spawn, SET SPAWN button, gold ring + SPAWN tag, spawnId in HTML, R restarts there). Added startup game-type chooser (Platformer / Top-Down / Orbit Puzzle / Side-Scroller, Esc dismiss, Ctrl+N reopen, Inspector rows switch anytime) with per-type controls + starter layouts + templates. Added scene background color (Inspector swatch + Windows ChooseColor picker, saved in .ogsproject v2, canvas fill + HTML preview share same hex). Canvas now shows world-origin cross + 1:1 label. Tests 1/1 pass in `build/windows-fix`, dist synced.
- 2026-10-09 NEON/SLING POWER (reference: `C:\Users\Sultan Sayed\crazy-neon-pulse\game.js` + `C:\Users\Sultan Sayed\orbit-sling\game.js`): Studio builds both game types. 9 objects, well/mover/coin params, ACTION cycler, par/hint via L, NEON flip + SLING drag-preview runner, 4 templates, save v3. Tests 1/1 pass.
- 2026-10-09 BUGFIX PASS: inspector overlaps, ghost chooser clicks, phantom NEW LEVEL, runner drift (see below).
- 2026-10-09 SLING SHOOT FIX + GAME SHELL (user: sling shooting bugged, wants GDevelop-like): ROOT CAUSE of broken shots = wall bounds written in screen pixels (`p.x<13 / p.x>947`) while the runner simulates in world coords (-480..480) — the whole left half behaved as a wall teleporting the orb to x=13. Fixed to world walls ±467/±257. Drag feel: multi-touch guard, grab-zone ring (110) + max-pull ring while dragging, power-colored bands (cyan→gold→red). Runner is now a real game: CLICK TO PLAY menu overlay, win overlay (stars/score/best), best persisted in localStorage. Studio closer to GDevelop flow: top bar shows real project filename, snap-8 toggle (G key + footer readout), Inspector ACTION title shows live values (WELL pull/roam, SLAB mover px, COIN value). Tests 1/1 pass (world-wall + shell asserts). NOTE: `dist` exe still locked (Studio open) — fresh binary in `build/windows-msys/ogs_editor.exe`. (1) Inspector BACKGROUND label/button + hint text overlapped (rows 548-584 shared pixels) → re-laid out BG to 570-614, CONTROLS shifted to 638-728; (2) GHOST CLICKS: invisible startup-chooser card areas kept stealing center-canvas clicks and randomly switching game type after close → split `game_type_choice_hit` (card only) from new `game_type_row_hit` (inspector only); (3) NEW LEVEL ghost: clicking empty canvas center with objects present secretly added starter objects → gated on empty scene; (4) RUNNER DRIFT: roamed wells + movers never restored on R/restart, wells drifted forever → reset() restores base positions. Tests 1/1 pass (added ghost-click guard assert). NOTE: `dist` exe could not be synced — user has Studio open (file lock); fresh binary in `build/windows-msys/ogs_editor.exe`, sync dist after close.
- Professional Studio UI, no-code editor, renderer, and game systems remain in their planned later phases.

## Issues / decisions
- GitHub: https://github.com/kabirprokk/oseva-game-studio (main @ 0d0a535, 84 files, pushed 2026-10-09). All OSEVA source + assets + docs + release exes live there. Regenerable CMake scratch + duplicate mp4 copies ignored via .gitignore.
- `OGS Logos\4.png` unclassified. `F:\fer-studio\` deprecated, root is `F:\OSEVA Game Engine\`.
- Player patterns have baked black bg — engine must handle as alpha/grid additive or require transparent re-export (to decide when engine starts).
- Cross-platform means future move off `F:\` absolute paths — spec update pending owner go.
