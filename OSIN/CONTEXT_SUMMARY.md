# OSIN handoff

## Current state

The OSIN CLI changes are ready for review in the `OSIN/` package.

- Repository: `kabirprokk/OSIEN`, CLI package under `OSIN/`.
- The branch is current with `origin/main` before the pending OSIN feature commit.
- `/model` browses provider or Codex models; arrow keys and Enter select.
- `/signin` invokes Codex CLI's ChatGPT device sign-in; `/api` selects the
  configured API-key provider; `/test-model` tests the selected model.
- Explicit unsupported-model responses exclude that model from the current
  OSIN run. Transient failures do not.
- ChatGPT requests use the Codex CLI, ephemeral sessions, and read-only mode.

## Verification

The local checks cover the CLI package and authenticated Codex path.

- `npm test`: all 26 tests pass.
- `node --check` passes for changed JavaScript files.
- Codex login, visible-model listing, and a real `gpt-6-luna` request were
  verified locally.
- `npm pack --dry-run` includes `codex.js` in package contents.

## Release

The package version is prepared for a release after the published `1.0.2`.

- Package version is prepared as `1.0.3` after the published `1.0.2`.
- Publish from this directory with `npm publish --access public`, then update
  an existing global install with the following command:

  ```sh
  npm install --global @kabirprokk/osin-cli@latest
  ```
