#!/usr/bin/env python3
"""Osien Forge config merger. Adds missing keys, NEVER overwrites yours.

Usage: merge-config.py --patch config.patch.json --config ~/.config/opencode/opencode.json [--dry-run] [--edition full|everyday|creator|agency]
- mcp/agent/command: adds keys you don't have, keeps yours untouched.
  --edition installs only that use-case's subset (full = everything).
- plugin/skills.paths: unions the lists.
- formatter/lsp/experimental/tool_output/compaction: sets only if absent.
- Always backs up your config with a timestamp first.
"""
import argparse
import copy
import json
import shutil
import time
from pathlib import Path


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--patch", required=True)
    ap.add_argument("--config", required=True)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--edition", default="full",
                    choices=("full", "everyday", "creator", "agency"),
                    help="use-case edition: install only what that user needs (full = everything)")
    args = ap.parse_args()

    # use-case editions: which mcp servers + commands each edition gets.
    # full = no filtering. Everything else is a strict subset.
    EDITION_MCP = {
        "everyday": {"everything", "memory", "sequential-thinking", "filesystem",
                     "time", "fetch", "context7-local", "context7-remote",
                     "exa-local", "exa-remote", "tavily", "deepwiki"},
        "creator": set(),   # filled below = everyday + game/creator set
        "agency": set(),    # filled below = everyday + agency set
    }
    CREATOR_MCP = {"git-mcp", "codebase-index", "repomix", "playwright",
                   "chrome-devtools", "puppeteer", "firecrawl", "sqlite",
                   "github", "arxiv", "fal-ai", "blender", "brave-search"}
    AGENCY_MCP = {"postgres", "supabase-local", "supabase-remote", "mongodb",
                  "gitlab", "railway", "docker", "kubernetes", "sentry",
                  "slack", "notion", "linear", "figma", "stripe", "posthog"}
    EDITION_MCP["creator"] = EDITION_MCP["everyday"] | CREATOR_MCP
    EDITION_MCP["agency"] = EDITION_MCP["everyday"] | AGENCY_MCP

    EDITION_CMD = {
        "everyday": {"start", "site", "brand", "brain", "doctor", "fast",
                     "context", "status", "handoff", "delegate"},
        "creator": set(),
        "agency": set(),
    }
    CREATOR_CMD = {"game", "play", "sprite", "level", "perfect", "publish",
                   "music", "deploy", "bot", "visual", "gen", "opt", "tex",
                   "sim", "collide", "tune"}
    AGENCY_CMD = {"web", "docs", "browser", "db", "deep", "get", "hire",
                  "ship", "fix", "plan", "standup", "commit", "sync",
                  "bakery", "restaurant", "salon", "shop", "freelancer"}
    EDITION_CMD["creator"] = EDITION_CMD["everyday"] | CREATOR_CMD
    EDITION_CMD["agency"] = EDITION_CMD["everyday"] | AGENCY_CMD

    edition = args.edition

    patch = json.loads(Path(args.patch).read_text())
    if args.config == "auto":
        cands = [
            Path.home() / ".config" / "opencode" / "opencode.json",
            Path(os.environ.get("APPDATA", "")) / "opencode" / "opencode.json",
        ]
        cfg_path = next((c for c in cands if c.exists()), cands[0])
        print(f"auto config: {cfg_path}")
    else:
        cfg_path = Path(args.config).expanduser()
    if cfg_path.exists():
        cfg = json.loads(cfg_path.read_text())
    else:
        cfg = {"$schema": "https://opencode.ai/config.json"}

    added: list[str] = []

    skipped = 0
    for section in ("mcp", "agent", "command"):
        cfg.setdefault(section, {})
        for key, val in patch.get(section, {}).items():
            if edition != "full":
                if section == "mcp" and key not in EDITION_MCP[edition]:
                    skipped += 1
                    continue
                if section == "command" and key not in EDITION_CMD[edition]:
                    skipped += 1
                    continue
            if key not in cfg[section]:
                cfg[section][key] = copy.deepcopy(val)
                added.append(f"{section}.{key}")

    # plugin list union (strings or [name, opts] tuples -> compare by name)
    def pname(p):
        return p[0] if isinstance(p, list) else p

    have = {pname(p) for p in cfg.get("plugin", [])}
    cfg.setdefault("plugin", [])
    for p in patch.get("plugin", []):
        if pname(p) not in have:
            cfg["plugin"].append(copy.deepcopy(p))
            added.append(f"plugin.{pname(p)}")
            have.add(pname(p))

    # skills.paths union
    cfg.setdefault("skills", {}).setdefault("paths", [])
    for p in patch.get("skills", {}).get("paths", []):
        if p not in cfg["skills"]["paths"]:
            cfg["skills"]["paths"].append(p)
            added.append(f"skills.paths+={p}")

    # safe scalar/object defaults (only if absent)
    for key in ("formatter", "lsp", "experimental", "tool_output", "compaction"):
        if key in patch and key not in cfg:
            cfg[key] = copy.deepcopy(patch[key])
            added.append(key)

    if args.dry_run:
        print(f"would add {len(added)} keys:")
        for a in added:
            print("  +", a)
        return

    if cfg_path.exists():
        bak = cfg_path.with_suffix(f".json.bak-forge-{int(time.time())}")
        shutil.copy2(cfg_path, bak)
        print(f"backup: {bak}")
    else:
        print("no existing config, writing fresh one")
    cfg_path.write_text(json.dumps(cfg, indent=2) + "\n")
    print(f"[edition: {edition}] merged {len(added)} new keys, skipped {skipped} (other editions), 0 overwritten:")
    for a in added:
        print("  +", a)


if __name__ == "__main__":
    main()
