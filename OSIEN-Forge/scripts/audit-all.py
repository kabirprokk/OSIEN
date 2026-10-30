#!/usr/bin/env python3
"""Osien Forge CI audit: every web template must score 100, game starters must
contain the perfection markers, config patch must be valid JSON. Exit 1 on fail.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
fails: list[str] = []


def check(cond: bool, msg: str) -> None:
    print(("PASS " if cond else "FAIL ") + msg)
    if not cond:
        fails.append(msg)


WEB_CHECKS = {
    "doctype": lambda h: h.lstrip().lower().startswith("<!doctype html>"),
    "viewport": lambda h: "viewport" in h,
    "title": lambda h: bool(re.search(r"<title>[^<]{5,}</title>", h)),
    "desc": lambda h: 'meta name="description"' in h,
    "h1": lambda h: "<h1" in h,
    "responsive": lambda h: bool(re.search(r"media ?\(|clamp\(|max-width", h)),
    "lang": lambda h: "<html lang=" in h,
    "small": lambda h: len(h.encode()) < 200 * 1024,
}

for d in sorted((ROOT / "web-templates").iterdir()):
    idx = d / "index.html"
    if not idx.exists():
        continue  # api/ is server.js, checked below
    h = idx.read_text()
    bad = [k for k, f in WEB_CHECKS.items() if not f(h)]
    check(not bad, f"web/{d.name}: {'100' if not bad else 'fix ' + ','.join(bad)}")

api = ROOT / "web-templates" / "api" / "server.js"
check(api.exists() and "http.createServer" in api.read_text(), "web/api server.js serves + routes")

GAME_MARKS = ["requestAnimationFrame", "visibilitychange", "AudioContext", "localStorage"]
for g in ("vanilla", "versus"):
    idx = ROOT / "game-templates" / g / "index.html"
    h = idx.read_text() if idx.exists() else ""
    missing = [m for m in GAME_MARKS if m not in h]
    check(idx.exists() and not missing, f"game/{g}: {'perfect markers ok' if not missing else 'missing ' + ','.join(missing)}")
for g in ("phaser", "three"):
    check((ROOT / "game-templates" / g / "index.html").exists(), f"game/{g} exists")

for f in ("config.patch.json",):
    try:
        p = json.loads((ROOT / f).read_text())
        check(bool(p.get("mcp")) and bool(p.get("command")), f"{f} valid ({len(p.get('mcp', {}))} mcp, {len(p.get('command', {}))} cmds)")
    except Exception as e:  # noqa: BLE001
        check(False, f"{f} invalid: {e}")

for f in ("plugin/meta-tools.ts", "plugin/webgame-tools.ts", "plugin/mega-tools.ts", "plugin/daily-tools.ts", "scripts/setup.sh", "scripts/setup.ps1", "scripts/check.sh", "scripts/merge-config.py", "studio.html"):
    check((ROOT / f).exists(), f"{f} packaged")

import subprocess
EXPECTED_EDITIONS = {"everyday": 41, "creator": 70, "agency": 74, "full": 109}  # incl. 9 agents
for ed, want in EXPECTED_EDITIONS.items():
    r = subprocess.run(
        [sys.executable, "scripts/merge-config.py", "--patch", "config.patch.json",
         "--config", "forge-ci-probe.json", "--dry-run", "--edition", ed],
        capture_output=True, text=True, cwd=ROOT)
    m = re.search(r"would add (\d+) keys", r.stdout)
    got = int(m.group(1)) if m else -1
    check(got == want, f"edition {ed}: {got} keys (want {want})")

print(f"\n{len(fails)} failures" if fails else "\nAll audits green.")
sys.exit(1 if fails else 0)
