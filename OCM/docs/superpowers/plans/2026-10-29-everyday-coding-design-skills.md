# Everyday Coding and Design Skills Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add curated, directly installable everyday coding and design skills from verified public repositories to the OpenCode registry.

**Architecture:** Add package metadata entries only; each entry points to an existing `SKILL.md` directory in its source repository. Keep the registry's existing schema and validator as the contract; do not add dependencies or install executable integrations.

**Tech Stack:** JSON registry, JSON Schema, Node.js validator, GitHub repositories.

**Spec:** Approved conversation design: curated individual coding/design skills, avoid duplicates, verify source and license, keep registry schema-valid.

## Global Constraints

- Only add skills whose source path and license are verifiable.
- Do not list names already in `registry.json`.
- Keep each package entry compatible with `schema.json` and `scripts/validate-registry.js`.
- Do not change the CLI, website, or MCP configuration in this pass.

## Review Focus

- Existing package name collision: assert every added name is unique.
- Missing upstream skill folder: verify all `repo` + `path` targets expose `SKILL.md`.
- License mismatch: preserve per-skill Apache-2.0 for Anthropic skills and MIT for Addy Osmani skills.
- Invalid metadata or description length: run the existing registry test.
- Drift in aggregate counts: do not hardcode a separate package/skill total; let consumers derive counts from registry entries and pack metadata.

---

### Task 1: Add curated everyday coding and design entries

**Files:**
- Modify: `registry.json`
- Modify: `schema.json` (allow existing nonnegative `skill_count` pack metadata)
- Test: `npm test`

**Interfaces:**
- Consumes: existing registry package schema and source conventions.
- Produces: eleven individually installable `skill` entries.

- [ ] **Step 1: Confirm additions are absent**

Check that the following names do not already occur in `registry.json`: `frontend-ui-engineering`, `code-simplification`, `api-and-interface-design`, `browser-testing-with-devtools`, `security-and-hardening`, `incremental-implementation`, `documentation-and-adrs`, `performance-optimization`, `webapp-testing`, `web-artifacts-builder`, `theme-factory`.

- [ ] **Step 2: Add the exact sources**

Add the first eight entries from `https://github.com/addyosmani/agent-skills`, using `skills/<name>` paths and `MIT` license. Add the final three from `https://github.com/anthropics/skills`, using `skills/<name>` paths and `Apache-2.0` license. Use version `main`, install `git`, authors `addyosmani` and `anthropics`, type `skill`, and concise descriptions/tags matching each skill's actual purpose.

- [ ] **Step 3: Validate registry and sources**

Run `npm test`; verify all eleven entries are unique, schema-valid, and their GitHub source directories contain `SKILL.md`.

- [ ] **Step 4: Commit the registry update**

Commit only `registry.json` and this plan after checks pass.
