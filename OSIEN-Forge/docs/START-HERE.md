# START HERE (no jargon) 👋

Osien Forge makes your AI assistant much stronger. It is **free** and mostly works **offline**.

## Install (one command, ~5 minutes)

```bash
git clone https://github.com/<you>/Osien-Forge.git
cd Osien-Forge
bash scripts/setup.sh
```

It installs everything itself (Node, tools, browsers). Your existing settings are **never overwritten** — a backup is made first.

Then: **close your terminal, open a new one, restart opencode.**

## Your first magic tricks

**A game in 60 seconds** — type in opencode:
```
/game snake
```
Open the file it makes. Arrow keys to play. Then `/bot` — a robot plays it and tells you the score.

**A website in 5 minutes:**
```
/site my bakery menu
```
Fill in your dishes, then put it online free (it will tell you how).

**An invoice that prints to PDF:**
```
/site my work invoice
```
Click the yellow cells, type, press the Print button → Save as PDF. Send to client. Get paid. 🎉

**A logo in 10 seconds:**
```
/brand Mama Njoroge eco
```

## Do I need…

- **Internet?** For install, yes. After that, games/sites/invoices/music work offline. Web search tools need internet.
- **API keys?** No — for everything above. Keys only unlock extras (Google-grade search, databases, Slack…). Run `/doctor` anytime to see what's working.
- **Money?** No. Everything here is free and open-source (MIT). Some *optional* extras (fancy AI image APIs) cost money — you never need them.

## If something breaks

Type `/doctor`. It checks its own tools and fixes itself. If still stuck, open an issue on GitHub with the `/doctor` output pasted in.

## FAQ

- *Will this delete my stuff?* No. The installer only **adds**; your config is backed up every time.
- *Windows/Mac?* Windows: use WSL2 + Ubuntu, then follow Linux steps. Mac: most works; browsers install via the same script.
- *I run an agency?* See: `/brand`, proposal + invoice starters, SEO audits, and the dashboard starter for client reports.
