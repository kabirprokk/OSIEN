# OSEVA GAME STUDIO (OGS) — SIGNATURE STYLE BIBLE v1
### Read this before drawing a single pixel. The goal: anyone blind-told "this is OGS" must be right.
### If in doubt, choose LESS. OGS taste = restraint + glow.

---

## 1. THE ONE-SENTENCE TASTE
Dark silence, one glowing thing that matters, gold only when the player earned it.

## 2. COLOR — EXACT TOKENS (no substitutions, no gradients outside these)
```
--bg:      #05060F   page + canvas base
--bg-deep: #02030A   gradient end / fullscreen letterbox
--bg-top:  #0B1030   gradient start
--panel:   rgba(10,13,24,.82)   sheets, pills, toasts
--hair:    rgba(255,255,255,.09) every border. 1px only. Never 2px boxes.
--ink:     #EEF2FF   text, rings, primary buttons
--dim:     #9AA3BB   labels, hints, secondary text
--faint:   #5C657F   microcopy, footers
--cyan:    #00F0FF   THE player color. Aim dots, trails, primary glow.
--violet:  #7B2FFF   gravity, depth, planets, secondary surfaces
--pink:    #FF2BD6   combos, near-miss, thrill accents
--gold:    #FFD964   REWARD ONLY: coins, stars, finish, wins
--red:     #FF2B2B   HAZARDS ONLY: spikes, death flash
```
RULES: gold and red never share a screen element. Max TWO accent colors per screen (cyan + one). Backgrounds never pure black (`#000` forbidden except letterbox bars).

## 3. CANVAS LOOK (every game frame)
1. Vertical gradient `#0B1030 → #05060F → #02030a`.
2. Faint grid, cyan at 6–8% alpha, 80px cells. Always static, never scrolling with gameplay.
3. 60–90 tiny stars, alpha twinkle 0.2–0.5. Slow or static.
4. Vignette: radial transparent→black 0.42 at corners. Mandatory.
5. Optional: ONE drifting nebula blob per zone color at ≤14% alpha. Never two bright blobs.
6. Letterbox 6% top/bottom bars on menus and win cinematics only.

## 4. BODIES & SPRITES (the OGS fingerprint)
- Every living thing = **dark core `#05060F` + white-hot center + colored aura + 1.6px ink hairline ring**. No exceptions. This ring is the signature — a player must see it and think OGS.
- Glow via layered radial gradients + `shadowBlur` 12–28. Never flat fills for heroes.
- Glyphs (bolt, flame, ring, rays, pixels) drawn INSIDE the body, never outside.
- Everything breathes: `scale = 1 + sin(t*3.2)*0.04`. Wobble on action `+wob*0.12`.
- Death = 0.25x slow-mo + magenta flash + radial burst. Win = gold burst + rising chime.
- Pickups: gold coins spin (ellipse squash), blue motes bob. Counts float up (`+10`, `NEAR +5`) in Orbitron and fade in 0.9s.

## 5. UI TASTE (clean, less, perfect)
- Layout: whisper topbar (12px tracked brand left, coin + status right), stage fills the biggest 16:9 the viewport allows, one-line footer.
- HUD = floating TEXT, not boxes: big thin score, micro tracked labels (`SCORE · ENDLESS`), combo pill appears only when ≥x2.
- Sheets: one quiet card per screen, `rgba(10,13,24,.82)` + blur(10px) + 20px radius + hairline. Entrance: 14px rise, 0.4s, soft spring.
- Buttons: ONE primary per screen (ink fill, dark text). Secondaries = hairline ghosts in rows of two. Danger/voluntary-ad actions may use pink gradient. Disabled = 55% opacity, never hidden (layout must not jump).
- Tabs = iOS segmented control (pill container, active = ink fill). Counts as `1/50` micro badges.
- Toasts: bottom-center dark pill, 12px, auto-dismiss 1.8–3s. Only ONE message at a time.
- Countdowns: 96px Orbitron center, pop animation, one soft beep per beat.
- Empty states always explain the next action ("Clear a level to unlock the next").
- No ALL-CAPS paragraphs. No more than 2 exclamation marks per game. No emojis in UI (icons are inline SVG strokes).

## 6. TYPE
- Orbitron 600–800: numbers, titles, buttons, badges. Tabular numerals for scores.
- Rajdhani 500–600: body, hints, microcopy. Line-height ≥1.6 for hints.
- Fallbacks mandatory: `Orbitron,Rajdhani,system-ui,sans-serif`. Game must look right with fonts blocked.

## 7. MOTION FEEL (game feel = OGS feel)
- Fixed 120Hz physics; render every rAF. No frame-dependent speeds anywhere.
- Forgiveness is a feature: hitboxes 65–78% of visuals, 4–7px edge forgiveness, always-passable generation (clamp deltas), 1–2s spawn grace with visible shield ring.
- Juice on everything the player earns: pop scale, burst, chime up; near-miss = 0.45x slow-mo 0.22s; record = happytime + gold toast.
- Never punish: ad failure = instant retry path; revive keeps progress and SHOWS any step-back honestly (`−3% REGROUP` + bar visibly moves).

## 8. LEVEL DESIGN TASTE
- Teach in 3: level 1 = one verb, level 2 = one twist, level 3 = combine. Hint line under 60 chars on early levels only.
- Progress = steady distance/travel %, never score chunks. Player must SEE the ending coming (finish gate sails in, "FINISH →" callout).
- Difficulty ramps ONE variable at a time (speed, then gap, then movers, then hazards). Movers only after level ~8. Nothing impossible: clamp every delta.
- Fairness checklist per level: spawn shield, killer removed on respawn, runway cleared before finish, hazards never guard pad/goal.

## 9. MONETIZATION TASTE (earn without shame)
- Ads are doors the player opens, never walls: labeled buttons, reward named, failure path stated. No auto-play, no mid-run breaks, no fake close buttons.
- Coins-first is forbidden: ad first, coins when ads fail, demo gets a labeled simulated ad.
- Shop = rows, not noisy cards: glow dot + name + one-line ability + price button. Abilities scale with price and are described in plain words.

## 10. FORBIDDEN (instant fail)
Pure black backgrounds, 2px+ box borders, more than 2 accents per screen, gold for non-rewards, red for non-hazards, rainbow gradients, Comic-style fonts, auto-playing anything, login walls, external hotlinked art, placeholder text shipped, console errors, double-firing touch handlers.

## 11. AI SELF-CHECK (run before calling anything done)
1. Screenshot-eyes: bg gradient + grid + vignette present? Max 2 accents? Hairline ring on hero?
2. `node --check` green, zero console errors, all DOM ids resolve.
3. Play blind: menu→core loop→fail→recover→win→shop→pause, keyboard AND touch.
4. Numbers honest: progress, coins, ledger — nothing double-paid, nothing silently reset.
5. Mobile portrait: letterboxed, tappable, single-fire per tap.

## 12. VERSION LINE
Style v1 locked 2026-10-08. Changes need a dated amendment entry here, never silent edits.

### Amendment 2026-10-09 — GRID SOULS players (owner-locked)
- Players = 17 abstract pattern skins in `Assets/Game Assets Player/` (crosses, dot-star, grids, folded, pixel-diamond, ornate tile, circle-links, dot-clusters, checker-warp, wavy net, rainbow grid, dotted wave, halftone sphere, perspective, blue burst). Kept per owner order.
- Exception to §4: pattern texture replaces hand-drawn glyph. Engine MUST add OGS signature at runtime: 1.6px ink ring + aura glow + breathe `sin(t*3.2)*0.04` so every GRID SOUL still reads as OGS.
- Planets = 6 in `Assets/Game Assets Planets/`, Obstacles = 5 in `Assets/Game Assets Obstacles/` — must export transparent (no baked bg), hazards red-only, gravity violet-only.
