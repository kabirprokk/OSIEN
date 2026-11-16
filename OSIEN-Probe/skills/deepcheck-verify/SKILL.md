---
name: deepcheck-verify
description: Use OSIEN Probe (CLI, MCP server, and offline static analysis / multi-language AST scanner) to verify code changes for security vulnerabilities, logic bugs, secrets, and regressions before finishing a task.
---

# OSIEN Probe Verification Skill

Run **OSIEN Probe** as a fast, offline static-analysis and bug-detection gate after writing or modifying code. It executes **no project code** during scans and supports JavaScript/TypeScript (Babel AST + taint tracking), Python, Go, Rust, Java, Kotlin, C/C++, C#, Ruby, PHP, Swift, Bash, HTML, CSS, JSON, YAML, and SQL.

## 1. CLI Usage (Global `osien-probe` or `node bin/osien-probe.cjs`)

```powershell
osien-probe scan "<project-path>" --kind "<category>"
```

**Supported `<category>` values:**
- `"Website"` — Web UI, HTML/CSS accessibility, XSS, CSRF, CSP, JS/TS
- `"Software"` — Desktop/CLI apps, Electron, general multi-language source
- `"API / backend"` — HTTP services, auth, SQL/NoSQL, SSRF, rate limiting
- `"Mobile app"` — Android/iOS/WebView security & mobile config
- `"Library"` — npm/package metadata, exports, dependency hygiene
- `"Data / ML"` — Python ML/data pipelines (`torch.load`, `trust_remote_code`, `joblib`, seeding)

**Useful CLI flags & subcommands:**
- `--fail-on <severity>` — Exit with code `1` if any finding is at or above `critical | high | medium | low | info`
- `--json` / `--sarif` / `--markdown` — Structured output formats
- `--out <file>` — Write report directly to a file
- `--quiet` — Print summary counts only
- `osien-probe rules --kind Software` — List all rules and CWE mappings
- `osien-probe ai-status` — Check local Ollama AI review availability

Example verification gate:

```powershell
osien-probe scan "C:\path\to\project" --kind "API / backend" --json --fail-on high
```

## 2. MCP Server Tools (`osien-probe-mcp`)

AI clients supporting MCP can register `osien-probe-mcp` (stdio JSON-RPC) to call:
- `scan_project` (`path`, `category`, `format`, `maxFindings`)
- `inspect_finding_context` (`root`, `file`, `line`, `radius`)
- `verify_finding_with_ai` (`root`, `file`, `line`, `title`, ...)
- `list_rules` (`category`)

## 3. Interpreting Results & Handling Findings

1. **Fix real `critical`, `high`, and `medium` findings first** (command/SQL injection, prototype pollution, hardcoded secrets, off-by-one loops, async race conditions, unawaited promises, `NaN`/`typeof` bugs, ignored `.trim()`/`.filter()` returns, missing `return` in `.map()`, `sprintf`, `pickle.loads`).
2. **Validate context:** Static analysis is a high-signal heuristic, not formal proof. Read the reported `file:line` and `reason`/`hint` before editing.
3. **Suppress only verified false positives:** Use inline directives rather than disabling rules globally:
   - `// deepcheck-disable-next-line <ruleId>` (or `# deepcheck-disable-next-line <ruleId>`)
   - `// deepcheck-disable-line <ruleId>`
4. **Re-run OSIEN Probe** after your final edit to confirm the fix and verify the updated score.
