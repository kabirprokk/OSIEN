# OpenCode Marketplace Registry

A community registry of skills, agents, and commands for [OpenCode](https://github.com/anomalyco/opencode).

## Browse & install

**Website:** https://ocmskills.vercel.app

Install the CLI (requires Node.js 20+, npm, and Git):
```bash
npm install --global github:kabirprokk/opencode-marketplace
```

The unscoped npm name `opencode-marketplace` belongs to a different project.
Use the GitHub install command above for this CLI.

Commands:
```bash
ocm list              # list all packages
ocm search <query>    # search
ocm info <name>       # details
ocm install <name>    # install to ~/.config/opencode/
ocm remove <name>     # remove only CLI-tracked installs
```

## Publish your own

1. Create a GitHub repo with your skill/agent/command (following OpenCode's format).
2. Fork this registry.
3. Add an entry to `registry.json`.
4. Open a PR.

### Entry format

```json
{
  "name": "my-skill",
  "type": "skill",
  "version": "1.0.0",
  "description": "What it does",
  "author": "your-github-username",
  "repo": "https://github.com/you/my-skill",
  "path": "skills/my-skill",
  "install": "git",
  "tags": ["tag1", "tag2"],
  "license": "MIT"
}
```

Types: `skill`, `agent`, `command`, `skill-pack`.

Package entries are community submissions and are not security-reviewed.
Review the source repository before installing. For individual skills, agents,
and commands, `path` must point to the specific source folder or Markdown file.
Skill packs are expected to provide `skills/`, `agents/`, and/or `commands/`
directories. The CLI skips existing package paths instead of overwriting them.

## License
MIT
