#!/usr/bin/env bash
# Osien Forge — one-command installer. Linux, macOS, Windows-git-bash/WSL.
# Usage:  bash scripts/setup.sh            (normal install)
#         bash scripts/setup.sh --full     (+ arxiv venv, all warm-ups)
# Safe: backs up opencode.json, merges (never overwrites your settings).
# No sudo. No terminal knowledge needed after this (see studio.html).
set -eu
# pipefail where supported (bash 3.2+); ignore failure on ancient shells
set -o pipefail 2>/dev/null || true

FORGE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OPENCODE_DIR="${OPENCODE_DIR:-$HOME/.config/opencode}"
say() { printf '[forge] %s\n' "$*"; }
warn() { printf '[forge] NOTE: %s\n' "$*"; }
EDITION="$(cat "$FORGE_DIR/.edition" 2>/dev/null || echo full)"
for a in "$@"; do
  case "$a" in
    --full) EDITION="full" ;;
    --edition=*) EDITION="${a#--edition=}" ;;
  esac
done
case "$EDITION" in full|everyday|creator|agency) ;; *) echo "[forge] ERROR: --edition must be full|everyday|creator|agency"; exit 1 ;; esac
FULL=0
[ "$EDITION" = "full" ] && FULL=1
say "Edition: $EDITION (full = all use cases; everyday/creator/agency = slim pick)"

# Portable "run with timeout" (macOS has no `timeout` command)
run_limited() {
  secs="$1"; shift
  "$@" & pid=$!
  i=0
  while kill -0 "$pid" 2>/dev/null; do
    i=$((i + 1))
    if [ "$i" -ge "$((secs * 2))" ]; then kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true; return 124; fi
    sleep 0.5
  done
  wait "$pid" 2>/dev/null
}

OS="$(uname -s)"; ARCH="$(uname -m)"
case "$OS" in
  Linux) OSN="linux" ;;
  Darwin) OSN="darwin" ;;
  MINGW*|MSYS*|CYGWIN*) OSN="windows-bash" ;;
  *) OSN="linux" ;;
esac
case "$ARCH" in
  x86_64|amd64) ARCHN="x64" ;;
  arm64|aarch64) ARCHN="arm64" ;;
  *) ARCHN="x64" ;;
esac
say "Detected: $OS ($ARCH). Windows native? Use Install-Windows.bat instead."

command -v curl >/dev/null 2>&1 || { echo "[forge] ERROR: curl is required. Install curl and re-run."; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "[forge] ERROR: python3 is required (preinstalled on Mac/Linux, Microsoft Store on Windows)."; exit 1; }

# ---- 1. Node 20+ ----
need_node=1
if command -v node >/dev/null 2>&1; then
  major="$(node -p "process.versions.node.split('.')[0]" 2>/dev/null || echo 0)"
  case "$major" in ''|*[!0-9]*) major=0 ;; esac
  if [ "$major" -ge 20 ] 2>/dev/null; then need_node=0; fi
fi
if [ "$need_node" = 1 ]; then
  say "Installing Node 22 for $OSN-$ARCHN (local, no sudo)..."
  LATEST="$(curl -s https://nodejs.org/dist/index.json | python3 -c "import json,sys; v=[x['version'] for x in json.load(sys.stdin) if x['version'].startswith('v22.')]; print(v[0])")"
  mkdir -p "$HOME/.local/nodejs"
  if [ "$OSN" = "darwin" ]; then EXT="tar.gz"; else EXT="tar.xz"; fi
  curl -sL "https://nodejs.org/dist/$LATEST/node-$LATEST-$OSN-$ARCHN.$EXT" -o /tmp/forge-node.$EXT
  if [ "$EXT" = "tar.gz" ]; then tar -xzf /tmp/forge-node.$EXT -C "$HOME/.local/nodejs" --strip-components=1
  else tar -xJf /tmp/forge-node.$EXT -C "$HOME/.local/nodejs" --strip-components=1; fi
  rm -f /tmp/forge-node.$EXT
else
  say "Node OK: $(node --version)"
fi

# ---- 2. uv ----
if ! command -v uvx >/dev/null 2>&1 && [ ! -x "$HOME/.local/bin/uvx" ]; then
  say "Installing uv..."
  curl -LsSf https://astral.sh/uv/install.sh | sh
else
  say "uv OK"
fi

# ---- 3. PATH (every shell, idempotent) ----
PATH_LINE='export PATH="$HOME/.local/nodejs/bin:$HOME/.local/bin:$PATH"'
for rc in "$HOME/.bashrc" "$HOME/.zprofile" "$HOME/.bash_profile"; do
  case "$rc" in *bash_profile) [ "$OSN" = "darwin" ] || continue ;; esac
  touch "$rc" 2>/dev/null || continue
  grep -qF ".local/nodejs/bin" "$rc" 2>/dev/null || echo "$PATH_LINE" >> "$rc"
done
export PATH="$HOME/.local/nodejs/bin:$HOME/.local/bin:$PATH"
say "Tools ready: node $(node --version), uvx $(uvx --version 2>/dev/null || echo missing)"

# ---- 4. Browsers for playtesting ----
say "Installing headless Chromium (one-time download)..."
run_limited 600 npx -y playwright@latest install chromium --only-shell 2>&1 | tail -n 1 || warn "browser install had issues (playwright still tries system browsers)"

# ---- 5. Warm key packages so first connect is instant ----
say "Warming packages..."
run_limited 90 npx -y @upstash/context7-mcp --help >/dev/null 2>&1 || true
run_limited 60 npx -y repomix --version >/dev/null 2>&1 || true
ln -sf "$(command -v python3)" "$HOME/.local/bin/python3.12" 2>/dev/null || true

if [ "$FULL" = 1 ]; then
  say "Full warm-up (arxiv venv)..."
  if [ ! -x "$HOME/.local/share/arxiv-mcp/bin/python" ]; then
    SRC="$(find "$FORGE_DIR" -path "*arxiv_mcp_server/server.py" 2>/dev/null | head -n 1)"
    if [ -n "$SRC" ]; then
      mkdir -p "$HOME/.local/share"
      SRCROOT="$(cd "$(dirname "$SRC")/.." && pwd)"
      cp -r "$SRCROOT" "$HOME/.local/share/arxiv-mcp-src"
      "$HOME/.local/bin/uvx" --python 3.12 venv "$HOME/.local/share/arxiv-mcp" 2>/dev/null || python3 -m venv "$HOME/.local/share/arxiv-mcp" || warn "venv failed, arxiv will use fallback"
      "$HOME/.local/share/arxiv-mcp/bin/pip" -q install "mcp==0.9.1" aiohttp "pydantic>=2.0.3,<3.0.0" python-dateutil PyPDF2 pdfplumber requests beautifulsoup4 lxml 2>&1 | tail -n 1 || true
    else
      warn "arxiv src not in package, skipping"
    fi
  fi
fi

# ---- 6. Copy forge files ----
say "Installing forge files to $OPENCODE_DIR..."
mkdir -p "$OPENCODE_DIR/plugin" "$OPENCODE_DIR/agents" "$OPENCODE_DIR/commands" "$OPENCODE_DIR/game-templates" "$OPENCODE_DIR/web-templates" "$OPENCODE_DIR/skills"
cp -f "$FORGE_DIR/plugin/"*.ts "$OPENCODE_DIR/plugin/" 2>/dev/null || true
cp -f "$FORGE_DIR/agents/"*.md "$OPENCODE_DIR/agents/" 2>/dev/null || true
case "$EDITION" in
  everyday) WEB="landing blog resume menu invite links invoice" ;;
  creator) WEB="landing blog resume menu invite links invoice portfolio" ;;
  agency) WEB="landing blog dashboard portfolio api resume menu invite links invoice proposal" ;;
  *) WEB="landing blog dashboard portfolio api resume menu invite links invoice proposal" ;;
esac
for t in $WEB; do [ -d "$FORGE_DIR/web-templates/$t" ] && cp -rf "$FORGE_DIR/web-templates/$t" "$OPENCODE_DIR/web-templates/" 2>/dev/null || true; done
case "$EDITION" in creator|full) cp -rf "$FORGE_DIR/game-templates/"* "$OPENCODE_DIR/game-templates/" 2>/dev/null || true ;; esac
if [ -d "$FORGE_DIR/skills" ]; then cp -rf "$FORGE_DIR/skills/"* "$OPENCODE_DIR/skills/" 2>/dev/null || true; fi

# ---- 7. Merge config (safe) ----
say "Merging config (your settings are never overwritten)..."
python3 "$FORGE_DIR/scripts/merge-config.py" --patch "$FORGE_DIR/config.patch.json" --config "$OPENCODE_DIR/opencode.json" --edition "$EDITION"

say "Done! Next:"
echo "  1. CLOSE this terminal, open a NEW one (loads new tools)"
echo "  2. Restart opencode, run:  opencode mcp list"
echo "  3. Double-click studio.html, copy a command, paste it in chat. No terminal needed ever again."
echo "  4. Not working? run:  bash $FORGE_DIR/scripts/check.sh   and share the output."
