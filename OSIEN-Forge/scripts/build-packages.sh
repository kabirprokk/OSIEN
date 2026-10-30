#!/usr/bin/env bash
# Builds release zips: dist/OsienForge-<edition>-<os>.zip
# Editions: everyday, creator, full (agency installs from full via --edition agency).
# OS: linux, macos, windows. Total: 3 x 3 = 9 zips.
# Usage: bash scripts/build-packages.sh   (output in dist/ + MANIFEST.txt)
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$ROOT/dist"
command -v zip >/dev/null 2>&1 || { echo "need 'zip' installed"; exit 1; }
rm -rf "$DIST"; mkdir -p "$DIST"

WEB_EVERYDAY="landing blog resume menu invite links invoice"
WEB_CREATOR="landing blog resume menu invite links invoice portfolio"
WEB_FULL="landing blog dashboard portfolio api resume menu invite links invoice proposal"

for ED in everyday creator full; do
  case "$ED" in
    everyday) WEB="$WEB_EVERYDAY"; GAME=""; SKILLS="ai-manager" ;;
    creator) WEB="$WEB_CREATOR"; GAME="vanilla phaser three versus"; SKILLS="ai-manager osien-3d-forge" ;;
    full) WEB="$WEB_FULL"; GAME="vanilla phaser three versus"; SKILLS="ai-manager osien-3d-forge" ;;
  esac
  for OS in linux macos windows; do
    STAGE="$(mktemp -d)"
    mkdir -p "$STAGE/plugin" "$STAGE/agents" "$STAGE/web-templates" "$STAGE/game-templates" "$STAGE/skills" "$STAGE/scripts" "$STAGE/os" "$STAGE/bundles"
    cp "$ROOT"/plugin/*.ts "$STAGE/plugin/"
    cp "$ROOT"/agents/*.md "$STAGE/agents/"
    for t in $WEB; do cp -r "$ROOT/web-templates/$t" "$STAGE/web-templates/"; done
    for t in $GAME; do cp -r "$ROOT/game-templates/$t" "$STAGE/game-templates/"; done
    for s in $SKILLS; do cp -r "$ROOT/skills/$s" "$STAGE/skills/"; done
    cp "$ROOT"/studio.html "$ROOT"/README.md "$ROOT"/LICENSE "$ROOT"/VERSION "$ROOT"/CHANGELOG.md "$ROOT"/config.patch.json "$STAGE/"
    cp "$ROOT/bundles/$ED.json" "$STAGE/bundle.json"
    cp "$ROOT/scripts/setup.sh" "$ROOT/scripts/merge-config.py" "$ROOT/scripts/check.sh" "$ROOT/scripts/audit-all.py" "$STAGE/scripts/"
    cp "$ROOT/os/$OS/README.md" "$STAGE/OS-README.md"
    echo "$ED" > "$STAGE/.edition"
    case "$OS" in
      windows) cp "$ROOT/scripts/setup.ps1" "$STAGE/scripts/"; cp "$ROOT/Install-Windows.bat" "$STAGE/" ;;
      macos) cp "$ROOT/Install-Mac.command" "$STAGE/"; chmod +x "$STAGE/Install-Mac.command" ;;
    esac
    (cd "$STAGE" && zip -qr "$DIST/OsienForge-$ED-$OS.zip" .)
    rm -rf "$STAGE"
    echo "built dist/OsienForge-$ED-$OS.zip"
  done
done
(cd "$DIST" && sha256sum ./*.zip > MANIFEST.txt 2>/dev/null || shasum -a 256 ./*.zip > MANIFEST.txt)
ls -la "$DIST"
