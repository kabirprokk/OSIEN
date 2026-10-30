# Linux install — any distro (no coding, ~5 minutes)

**Easiest:** download the `OsienForge-<edition>-linux.zip` from Releases,
unzip it, then:

```bash
cd OsienForge
bash scripts/setup.sh
```

Close the terminal, open a new one, restart opencode. Then double-click
`studio.html`, copy `/start`, paste it in chat. Done — no terminal after this.

Notes:
- Works on Ubuntu, Debian, Fedora, Arch, Mint, Zorin… any distro with `curl`, `python3`, `git`.
- No sudo needed. Everything installs into your home folder.
- Pick your size: `bash scripts/setup.sh --edition everyday` (small),
  `creator`, `agency`, or default `full` (everything).
- Stuck? `bash scripts/check.sh` and share the output.
