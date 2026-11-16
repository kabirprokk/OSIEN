'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const execFileAsync = promisify(execFile);

const { TOOLS, handleToolCall } = require('../bin/osien-probe-mcp.cjs');
const cliPath = path.resolve(__dirname, '..', 'bin', 'osien-probe.cjs');

test('osien-probe CLI scans a project and honors --json and --fail-on', async (t) => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'osien-probe-cli-'));
  t.after(() => fs.rm(dir, { recursive: true, force: true }));
  await fs.writeFile(path.join(dir, 'bug.js'), "'use strict';\neval(userInput);\n", 'utf8');

  const { stdout } = await execFileAsync(process.execPath, [cliPath, 'scan', dir, '--kind', 'Software', '--json']);
  const parsed = JSON.parse(stdout);
  assert.equal(parsed.filesScanned, 1);
  assert.ok(parsed.findings.some((f) => f.ruleId === 'js-eval'));

  await assert.rejects(
    execFileAsync(process.execPath, [cliPath, 'scan', dir, '--fail-on', 'high']),
    (err) => err.code === 1,
  );
});

test('osien-probe MCP server exposes scan_project, inspect_finding_context, and list_rules', async (t) => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'osien-probe-mcp-'));
  t.after(() => fs.rm(dir, { recursive: true, force: true }));
  await fs.writeFile(path.join(dir, 'app.js'), "'use strict';\nconst x = 1;\neval(input);\n", 'utf8');

  assert.deepEqual(TOOLS.map((tool) => tool.name), [
    'scan_project',
    'inspect_finding_context',
    'verify_finding_with_ai',
    'list_rules',
  ]);

  const scanRes = await handleToolCall('scan_project', { path: dir, category: 'Software' });
  const scanData = JSON.parse(scanRes.content[0].text);
  assert.ok(scanData.findings.some((f) => f.ruleId === 'js-eval'));

  const ctxRes = await handleToolCall('inspect_finding_context', { root: dir, file: 'app.js', line: 3, radius: 2 });
  const ctxData = JSON.parse(ctxRes.content[0].text);
  assert.match(ctxData.context, /3: eval\(input\);/);

  const rulesRes = await handleToolCall('list_rules', { category: 'Software' });
  const rulesData = JSON.parse(rulesRes.content[0].text);
  assert.ok(rulesData.count > 100);
});
