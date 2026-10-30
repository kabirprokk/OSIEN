#!/usr/bin/env bash
# Publishes OSIEN-Forge to GitHub: repo + push + 3 releases (everyday/creator/full x win/mac/linux).
# Usage: bash scripts/publish.sh [github-username]
# Needs: git. gh CLI is auto-installed if missing. Login via `gh auth login` (run once).
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
USER_HINT="${1:-}"

if ! command -v gh >/dev/null 2>&1; then
  echo "[publish] installing GitHub CLI..."
  OS="$(uname -s)"
  if [ "$OS" = "Darwin" ] && command -v brew >/dev/null 2>&1; then brew install gh
  elif [ "$OS" = "Linux" ]; then
    VER="$(curl -s https://api.github.com/repos/cli/cli/releases/latest | python3 -c "import json,sys; print(json.load(sys.stdin)['tag_name'])")"
    mkdir -p "$HOME/.local/bin"
    curl -sL "https://github.com/cli/cli/releases/download/$VER/gh_${VER#v}_linux_amd64.tar.gz" -o /tmp/gh.tgz
    tar -xzf /tmp/gh.tgz -C /tmp && cp /tmp/gh_${VER#v}_linux_amd64/bin/gh "$HOME/.local/bin/" && rm -rf /tmp/gh.tgz /tmp/gh_${VER#v}_linux_amd64
    export PATH="$HOME/.local/bin:$PATH"
  else
    echo "[publish] install gh manually: https://cli.github.com/ then re-run"; exit 1
  fi
fi

gh auth status >/dev/null 2>&1 || { echo "[publish] run first: gh auth login"; gh auth login; }
OWNER="$(gh api user -q .login)"
[ -n "$USER_HINT" ] && OWNER="$USER_HINT"
echo "[publish] owner: $OWNER"

git branch -M main 2>/dev/null || true
if ! gh repo view "$OWNER/OSIEN-Forge" >/dev/null 2>&1; then
  gh repo create OSIEN-Forge --public --source=. --push \
    --description="Osien Forge: free AI superpowers for everyone - games, websites, brand kits, invoices, self-healing tools. Works with free opencode models." \
    --homepage="https://github.com/$OWNER/OSIEN-Forge#readme"
else
  git remote get-url origin >/dev/null 2>&1 || git remote add origin "https://github.com/$OWNER/OSIEN-Forge.git"
  git push -u origin main
fi

for TAG in everyday-v1.0 creator-v1.0 full-v1.0; do
  git tag -f "$TAG"
  git push -f origin "$TAG"
done

[ -d dist ] || bash scripts/build-packages.sh
gh release create everyday-v1.0 dist/OsienForge-everyday-*.zip --title "Everyday Edition v1.0" --notes-file release-notes/everyday-v1.0.md 2>/dev/null || gh release upload everyday-v1.0 dist/OsienForge-everyday-*.zip --clobber
gh release create creator-v1.0 dist/OsienForge-creator-*.zip --title "Creator Edition v1.0" --notes-file release-notes/creator-v1.0.md 2>/dev/null || gh release upload creator-v1.0 dist/OsienForge-creator-*.zip --clobber
gh release create full-v1.0 dist/OsienForge-full-*.zip --title "Full Edition v1.0 (everything)" --notes-file release-notes/full-v1.0.md 2>/dev/null || gh release upload full-v1.0 dist/OsienForge-full-*.zip --clobber

echo "[publish] LIVE: https://github.com/$OWNER/OSIEN-Forge/releases"
