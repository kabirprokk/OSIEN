#!/usr/bin/env bash
# Osien Forge health check. Works on Linux/macOS/git-bash. No changes made.
# Usage: bash scripts/check.sh   (paste the output when asking for help)
set -u
pass=0; fail=0
ok() { echo "  PASS $1"; pass=$((pass+1)); }
bad() { echo "  FAIL $1 -- $2"; fail=$((fail+1)); }

echo "== Osien Forge health check =="
[ -n "${BASH_VERSION:-}" ] && echo "shell: bash $BASH_VERSION" || echo "shell: $0"

if command -v node >/dev/null 2>&1; then
  major="$(node -p "process.versions.node.split('.')[0]" 2>/dev/null || echo 0)"
  case "$major" in ''|*[!0-9]*) major=0 ;; esac
  if [ "$major" -ge 20 ] 2>/dev/null; then ok "node $(node --version)"; else bad "node $(node --version)" "need 20+, re-run setup.sh"; fi
else bad "node" "not found, run setup.sh"; fi

if command -v uvx >/dev/null 2>&1 || [ -x "$HOME/.local/bin/uvx" ]; then ok "uvx present"; else bad "uvx" "run setup.sh"; fi
command -v python3 >/dev/null 2>&1 && ok "python3 $(python3 --version 2>&1)" || bad "python3" "install python3"
command -v curl >/dev/null 2>&1 && ok "curl present" || bad "curl" "install curl"
command -v git >/dev/null 2>&1 && ok "git present" || bad "git" "install git (only needed for updates)"

CFG="${OPENCODE_DIR:-$HOME/.config/opencode}"
[ -f "$CFG/opencode.json" ] && ok "config exists" || bad "config" "run setup.sh merge step"
for d in plugin agents game-templates web-templates; do
  [ -d "$CFG/$d" ] && ok "$d installed" || bad "$d" "run setup.sh"
done
[ -f "$CFG/plugin/meta-tools.ts" ] && ok "meta-tools (doctor/brain)" || bad "meta-tools" "re-run setup.sh file copy"
[ -f "$CFG/plugin/webgame-tools.ts" ] && ok "webgame tools (bot/music)" || bad "webgame tools" "re-run setup.sh file copy"

BROWS="$(find "$HOME/.cache/ms-playwright" \( -name chrome-headless-shell -o -name headless_shell \) -type f 2>/dev/null | head -n 1)"
[ -n "$BROWS" ] && ok "headless browser cached" || bad "browser" "npx playwright install chromium --only-shell"

echo ""
echo "Result: $pass passed, $fail failed."
[ "$fail" = 0 ] && echo "Perfect everywhere. 🎉" || echo "Fix the FAIL lines (or paste this whole output when asking for help)."
