# OSEVA GAME STUDIO — ASSET SIZE CHEAT SHEET v1
### Every dimension for sprites, logos, thumbnails, covers, videos. Sources: official CrazyGames docs (game-covers page) + OGS canvas spec. Locked 2026-10-08.

## 1. PORTAL COVERS (mandatory, exact pixels, PNG/JPG)
| Slot | Ratio | Pixels | Our file |
|------|-------|--------|----------|
| Landscape | 16:9 | **1920x1080** | `*-cover-1920x1080.png` |
| Portrait | 2:3 | **800x1200** | `*-cover-800x1200.png` |
| Square | 1:1 | **800x800** | `*-cover-800x800.png` |
Rules (official, instant reject if broken): NO borders · ONLY the game title as text (no "New/Play/Updated") · NO store/app icons · NO copyrighted or blurry art · all 3 same visual family · title ON the cover in a stylized font (Orbitron-class, never Times) · recognizable at 100px thumbnail · NOT just a raw screenshot.

## 2. PREVIEW VIDEOS (mandatory: 1 landscape + 1 portrait)
- Length **15–20s max** (longer gets cut; 10s accepted — ours are 10s, fine)
- Size **≤50MB** · MP4/MOV · H.264 + AAC, no sound track needed (platform mutes)
- Landscape **1920x1080 (16:9)** · Portrait **1080x1620 (2:3)** — keep exact ratio; our 800x1200 ports passed upload, upscale to 1080x1620 when re-exporting
- FORBIDDEN: black bars, black-screen/logo transitions, mouse cursor, "Play Now"/promo text, app/social icons, fast-forwarded footage (they auto-speed it slightly themselves)
- MUST: open on your static cover frame (seamless hover transition), show best 3 seconds FIRST (goal moment, win burst, rare skin)

## 3. IN-GAME CANVAS (OGS engine spec)
- Logical **960x540** (16:9). Backing store ×DPR capped 2 → **1920x1080 max**. Author raster art at 1920x1080, engine downscales.
- Safe zone: keep HUD-critical art inside 880x460 center (notches, portal chrome).

## 4. SPRITE AUTHORING (transparent PNG, draw at 4x logical, engine scales down)
| Sprite | Logical size | Draw at (4x) | Notes |
|--------|--------------|--------------|-------|
| Hero orb body | r 13–14 | **112x112** | dark core + white center + aura + 1.6px ring baked soft |
| Planet / well | r 20–36 | **288x288 max** | violet depth + orbit tick |
| Spike hazard | r 11–20 | **160x160 max** | red star, 8 points |
| Gold coin | r 8–12 | **96x96** | gold, draw 3 spin frames (wide/mid/edge) |
| Light mote | r 9 | **72x72** | cyan + white sparkle |
| Slab (neon) | 46 wide | **184px wide**, full-height strips | cyan top edge / magenta bottom edge variants |
| Finish gate | 64 wide | **256px wide** | gold pillars + 6-row checker strip + FINISH label |
| Goal ring | r 26–36 | **288x288** | gold ring + GOAL label |
Keep 8px transparent padding on all sides (glow room). 72dpi, sRGB.

## 5. LOGO (Oseva Game Studio — OGS, official set in `OGS Logos\`, all 512x512)
- **`2.png` = PRIMARY mark.** Black octagon + game-piece cutouts (circle, square, triangle, plus). Use everywhere by default.
- `3.png` = horizontal lockup (OGS + mark). Headers, footers, wide slots.
- `1.png` = stacked lockup. Large sizes ONLY — tagline dies below ~128px.
- Per use: 512 master files as-is; 192 (portal profile), 64 (favicon/tab), 32 (loading corner) — downscale, never stretch, clearspace = octagon edge x 1/4.
- Still missing (make before merch/launch): dark-bg + glow variant of 2.png; simplified small-size cut (solid octagon + plus) for ≤64px.
- Wordmark rule: ONE smooth geometric style to match the mark's cutouts. Tagline dropped below 128px.

## 6. THUMBNAIL WINS (why ours click)
One hero body + title + one gold element. Same family across all 3 ratios. Test: shrink to 100px — game still recognizable by color/shape.
