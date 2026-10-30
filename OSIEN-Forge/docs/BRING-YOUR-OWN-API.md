# Bring your own API (optional — everything works without it)

Osien Forge is built for **free opencode models** (`opencode/...` — the free contributor tier).
Every tool, template, and command works with zero keys and mostly offline.

## If you HAVE your own key, the forge gets stronger

Any standard opencode provider works. Two ways:

**Option A — interactive login (easiest):**
```bash
opencode auth login
```
Pick your provider, paste the key. Done.

**Option B — config (good for teams):**
```json
{
  "provider": {
    "anthropic": { "options": { "apiKey": "sk-ant-..." } },
    "openai": { "options": { "apiKey": "sk-..." } }
  },
  "model": "anthropic/claude-sonnet-4-6"
}
```
(Also fine: `export ANTHROPIC_API_KEY=...` before starting opencode.)

## What changes with a paid key

| Without key (free) | With your key |
|---|---|
| All 50+ commands, all templates, music/SFX/sprites, bot, doctor, brain | Same, plus: |
| Web search via free tiers (rate-limited) | Higher search limits, faster big models |
| `fanout` uses 3 free models | `fanout` can judge with frontier models |

Nothing is ever locked behind a key. Keys only raise ceilings.
