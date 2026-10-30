# Osien Forge 🔥

**The toolkit no AI assistant has — for devs, agencies, and normal people.**

Games in seconds. Websites in minutes. Invoices, brand kits, resumes, music, sprites, research, self-healing tools — free, open-source (MIT), mostly offline.

## 30-second start

```bash
git clone https://github.com/<you>/Osien-Forge.git
cd Osien-Forge
bash scripts/setup.sh
# open a NEW terminal, restart opencode, then:
# /game snake  ·  /site bakery menu  ·  /brand my shop eco  ·  /doctor
```

**No terminal? No coding?** Download a ready-made package from
**[Releases](../../releases)** instead — 3 editions × 3 systems:

| Edition | Who | Download |
|---|---|---|
| 🏠 Everyday | Normal people, shops, students | `OsienForge-everyday-windows/macos/linux.zip` |
| 🎮 Creator | Gamers, YouTubers, kids | `OsienForge-creator-windows/macos/linux.zip` |
| 🔥 Full | Everyone — ALL use cases | `OsienForge-full-windows/macos/linux.zip` |

Windows: unzip → double-click `Install-Windows.bat`.
Mac: unzip → double-click `Install-Mac.command`.
Linux: unzip → `bash scripts/setup.sh`.
Then double-click `studio.html` inside — every feature is one copied command.
Agencies: install Full, then run setup with `--edition agency` for the client pack
(or pick per-client packs: `/bakery /restaurant /salon /shop /freelancer`).

New to all this? Read **[docs/START-HERE.md](docs/START-HERE.md)** — no jargon.

## What's inside

| Area | What you get |
|---|---|
| 🎮 Games | 4 perfect starters (arcade / platformer / 3D / 2-player versus), bot that **plays** your game and grades it, chiptune music + SFX generators, sprite maker, 100-point perfection audit |
| 🌐 Websites | 11 perfect starters: landing, blog, dashboard, portfolio, API, resume, menu, invite, links, invoice, proposal — all SEO + mobile perfect, most under 3KB |
| 🏢 Agency | 1-call brand kits (logo + palette + CSS), client proposals, editable invoices that print to PDF, SEO audits |
| 🧰 AI tools | 40 pre-wired MCP servers, `/doctor` self-healing, `/fanout` multi-model judge, `/brain` lessons that survive sessions |
| 🧠 Agents | 9 specialists: orchestrator, git, 3D, physics, monitor, researcher, devops, data, game-builder |

## Requirements

- Linux (or WSL2), `curl`, `python3`, `git`, `zip`
- That's it — `setup.sh` installs Node 22 + uv + browsers locally, no sudo.

## How it works

Every domain follows the same loop: **scaffold → audit to 100 → verify → deploy**.

- `/game flappy dragon` → `/perfect` → `/bot` → `/publish`
- `/site bakery menu` → fills content → free URL via `/deploy`
- `/brand Acme luxury` → `brand.css` + `logo.svg`
- `/doctor` → finds broken tools, fixes its own config

## Project layout

```
Osien-Forge/
  plugin/            instant offline tools (calc, music, sprites, doctor, brain, game bot…)
  game-templates/    vanilla, phaser, three, versus
  web-templates/     11 single-file starters
  agents/            specialist personalities
  skills/            auto-triggering skill(s)
  config.patch.json  merged SAFELY into your opencode.json (yours always wins)
  scripts/setup.sh   one-command installer
  scripts/merge-config.py
  docs/START-HERE.md beginner guide
```

Uninstall: delete the forge lines from `~/.config/opencode/opencode.json` (backups are auto-made, `*.bak-forge-*`).

## Contributing

PRs welcome! Add a template (`web-templates/<kind>/index.html`, run the audit, keep it <200KB), a tool (new file or function in `plugin/`), or a starter. Keep the pattern: **1 call → perfect → verified**.

## License

MIT — free for people and agencies, commercial use allowed. See [LICENSE](LICENSE).
