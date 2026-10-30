#!/usr/bin/env bash
# Osien Forge — double-click to install on Mac (opens Terminal automatically).
cd "$(dirname "$0")" || exit 1
bash scripts/setup.sh "$@"
echo ""
echo "Install finished! You can close this window."
echo "Next: double-click studio.html and copy a command like /start into your AI chat."
read -p "Press Enter to close..." dummy
