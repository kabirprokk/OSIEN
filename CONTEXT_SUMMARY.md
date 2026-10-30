# Context Summary

## Objective
Apply selected visual elements from https://osien.is-a.dev/ to the OSIN landing page while preserving OSIN's distinct identity.

## Decisions
- Keep OSIN's violet/cyan accents and real terminal capture.
- Use Space Grotesk display headings, Inter body text, JetBrains Mono labels, warm near-black surfaces, lighter headings, and compact editorial labels.
- Continue using https://osin.pages.dev/; custom domain setup was abandoned.
- Work independently; user explicitly requested no delegation.

## Current State
- Branch: `fix/web-authentic-terminal-capture`, based at `441bd52` before pending changes.
- Pending edits: `WEB/index.html`, `WEB/styles.css`.
- Changes add Google font loading, adjust dark palette/theme color, and style headings/cards/labels with mobile overrides.
- `OSIN/assets/osin-terminal.png` remains the authentic screenshot.

## Verification
- `npm test` in `OSIN/`: 18 passed, 0 failed.
- `node --check WEB/script.js`: passed.
- `git diff --check`: passed.
- Local page and stylesheet returned HTTP 200; visual checks were made at desktop and 390px mobile widths.

## Next
- Commit and push the visual changes, then verify the deployed `https://osin.pages.dev/` page and its font/theme assets.
