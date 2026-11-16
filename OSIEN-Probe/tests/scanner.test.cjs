'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');

const { scanProjectRoot, rulesForKind, toSarifReport, toMarkdownReport, kinds } = require('../electron/scanner.cjs');

const SEVERITIES = new Set(['critical', 'high', 'medium', 'low', 'info']);

async function fixture(files) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'deepcheck-test-'));
  for (const [rel, content] of Object.entries(files)) {
    const full = path.join(root, rel);
    await fs.mkdir(path.dirname(full), { recursive: true });
    await fs.writeFile(full, content, 'utf8');
  }
  return root;
}

const titles = (result) => result.findings.map((f) => f.title);

const VULNERABLE_SITE = `<!doctype html>
<html>
<head>
  <title>Broken</title>
</head>
<body>
  <img src="/hero.png">
  <a href="javascript:alert(1)">bad</a>
  <a href="https://example.test" target="_blank">external</a>
  <table><tr><td>no caption</td></tr></table>
  <button onclick="go()">go</button>
  <script src="https://cdn.example.test/lib.js"></script>
  <script>var answer = 42</script>
</body>
</html>
`;

const CLEAN_SITE = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Clean</title>
  <link rel="stylesheet" href="/app.css">
</head>
<body>
  <main>
    <img src="/a.png" alt="A diagram" width="640" height="480">
    <table><caption>Prices</caption><tr><td>1</td></tr></table>
  </main>
  <script src="/app.js" defer></script>
</body>
</html>
`;

test('rule catalog is well formed and covers every category', () => {
  for (const kind of kinds) {
    const catalog = rulesForKind(kind);
    assert.ok(catalog.length >= 120, `${kind} should have a broad catalog`);
    for (const rule of catalog) {
      assert.equal(typeof rule.id, 'string');
      assert.ok(SEVERITIES.has(rule.severity));
      assert.ok(rule.pattern instanceof RegExp);
    }
  }
});

test('detects the deliberate bugs in the broken website', async () => {
  const root = await fixture({ 'index.html': VULNERABLE_SITE, 'app.js': "'use strict';\n" });
  const result = await scanProjectRoot(root, 'Website');
  const found = titles(result);

  for (const expected of [
    'Missing alt attribute',
    'JavaScript URL',
    'Target blank without rel',
    'Table without a caption',
    'Inline event handler',
    'External script without integrity',
    'Inline script',
    'Inline script',
  ]) {
    assert.ok(found.includes(expected), `expected to detect "${expected}"`);
  }
});

test('does not invent findings on clean source', async () => {
  const root = await fixture({
    'index.html': CLEAN_SITE,
    'app.js': "'use strict';\nconst total = (a, b) => a + b;\nmodule.exports = { total };\n",
    'component.tsx': 'export const Sum = ({ a, b }: { a: number; b: number }) => <span>{a + b}</span>;\n',
  });
  const result = await scanProjectRoot(root, 'Website');
  assert.ok(!result.findings.some((f) => f.severity === 'critical' || f.severity === 'high'));
  assert.ok(!titles(result).includes('Missing alt attribute'));
  assert.ok(!titles(result).includes('JavaScript URL'));
  assert.ok(!titles(result).includes('Unsafe HTML injection'));
});

test('reports the exact line a finding sits on', async () => {
  const root = await fixture({
    'app.cjs': ["'use strict';", '', '// setup', 'eval(userInput);', ''].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const finding = result.findings.find((f) => f.title === 'Dynamic code evaluation');
  assert.ok(finding, 'eval is detected');
  assert.equal(finding.line, 4);
  assert.equal(finding.excerpt, 'eval(userInput);');
});

test('skips dependencies, build output and lockfiles', async () => {
  const root = await fixture({
    'node_modules/left-pad/index.js': 'eval(hacked);\n',
    'dist/bundle.js': 'eval(hacked);\n',
    'package-lock.json': '{"scripts":{"test":"eval(hacked)"}}',
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.filesScanned, 0);
  assert.deepEqual(result.findings, []);
});

test('honors .deepcheckignore glob patterns', async () => {
  const root = await fixture({
    '.deepcheckignore': 'samples/**\n*.generated.js\n',
    'samples/broken.js': 'eval(input);\n',
    'src/broken.generated.js': 'eval(input);\n',
    'src/real.js': "'use strict';\nconst safe = true;\n",
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.filesScanned, 1); // only src/real.js remains after excludes
  assert.equal(result.findings.some((f) => f.file.includes('samples/')), false);
  assert.equal(result.findings.some((f) => f.file.includes('generated')), false);
});

test('supports file, line, and next-line rule suppressions', async () => {
  const root = await fixture({
    'file.cjs': [
      '// deepcheck-disable-file js-eval',
      'eval(fileSuppressed);',
      '// deepcheck-disable-next-line js-eval',
      'eval(nextLineSuppressed);',
      'eval(visible); // deepcheck-disable-line js-eval',
      'eval(stillVisible);',
    ].join('\n'),
    'line.cjs': [
      "'use strict';",
      '// deepcheck-disable-next-line js-eval',
      'eval(hidden);',
      'eval(visible); // deepcheck-disable-line js-eval',
      'eval(shown);',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const evalFindings = result.findings.filter((finding) => finding.ruleId === 'js-eval');
  assert.deepEqual(evalFindings.map((finding) => finding.excerpt), ['eval(shown);']);
});

test('reports provider secrets once and ignores obvious placeholders', async () => {
  const root = await fixture({
    'secrets.js': [
      "const apiKey = 'example';",
      "const apiKey = 'sk_live_9f2c8b71d4';",
      "const stripeKey = 'sk_live_9f2c8b71d4';",
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.findings.filter((f) => f.ruleId === 'secret-generic-credential').length, 1);
  assert.equal(result.findings.filter((f) => f.ruleId === 'secret-stripe-key').length, 0);
});

test('scanner is deterministic apart from duration and timestamp fields', async () => {
  const root = await fixture({ 'app.js': "'use strict';\neval(input);\n" });
  const first = await scanProjectRoot(root, 'Software');
  const second = await scanProjectRoot(root, 'Software');
  assert.deepEqual(first.findings, second.findings);
  assert.equal(first.filesScanned, second.filesScanned);
  assert.equal(first.rulesEvaluated, second.rulesEvaluated);
  assert.equal(first.score, second.score);
});

test('uses source-to-sink context for HTML injection confidence', async () => {
  const root = await fixture({
    'unsafe.js': [
      "'use strict';",
      'const query = new URLSearchParams(location.search).get("q");',
      'target.innerHTML = query;',
      'safe.innerHTML = "<b>Known static markup</b>";',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Website');
  const htmlFindings = result.findings.filter((finding) => finding.ruleId === 'js-innerhtml');
  assert.equal(htmlFindings.length, 1);
  assert.equal(htmlFindings[0].line, 3);
  assert.equal(htmlFindings[0].confidence, 'high');
  assert.match(htmlFindings[0].reason, /untrusted input/i);
});

test('does not treat security-looking words in comments as executable code', async () => {
  const root = await fixture({
    'app.js': [
      "'use strict';",
      '// eval(userInput); this is documentation, not code',
      'const message = "eval(userInput)";',
      'console.log(message);',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.findings.some((finding) => finding.ruleId === 'js-eval'), false);
});

test('does not scan JavaScript string literals as executable security issues', async () => {
  const root = await fixture({
    'samples.js': [
      "const sample = 'eval(userInput);';",
      'const sampleSecret = "const apiKey = \'sk_live_9f2c8b71d4\';";',
      "const sampleQuery = 'db.query(`SELECT * FROM users WHERE id=${userId}`);';",
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.deepEqual(result.findings.filter((finding) =>
    ['js-eval', 'secret-generic-credential', 'js-sql-concat'].includes(finding.ruleId),
  ), []);
});

test('flags missing strict mode once per CommonJS file only', async () => {
  const root = await fixture({
    'loose.cjs': 'const a = 1;\nconst b = 2;\nconst c = 3;\n',
    'strict.cjs': "'use strict';\nconst a = 1;\n",
    'typed.ts': 'const a = 1;\n',
    'modern.mjs': 'export const a = 1;\n',
  });
  const result = await scanProjectRoot(root, 'Software');
  const strictFindings = result.findings.filter((f) => f.title === 'Missing strict mode');
  assert.equal(strictFindings.length, 1);
  assert.equal(strictFindings[0].file, 'loose.cjs');
});

test('applies category rules only to the selected category', async () => {
  const files = { 'index.html': '<button onclick="go()">go</button>\n' };
  const website = await scanProjectRoot(await fixture(files), 'Website');
  const library = await scanProjectRoot(await fixture(files), 'Library');
  assert.ok(titles(website).includes('Inline event handler'));
  assert.ok(!titles(library).includes('Inline event handler'));
});

test('caps output at 1000 findings and reports truncation', async () => {
  const root = await fixture({ 'wall.js': "eval(x);\n".repeat(1200) });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.findings.length, 1000);
  assert.equal(result.truncated, true);
  assert.ok(result.score >= 1, 'score never collapses to zero');
});

test('reports each rule once per line even when the catalog repeats it', async () => {
  const root = await fixture({ 'log.js': "'use strict';\nconsole.log('one value');\n" });
  const result = await scanProjectRoot(root, 'Software');
  const consoleFindings = result.findings.filter((f) => f.title === 'Console logging');
  assert.equal(consoleFindings.length, 1);
});

test('scores critical findings heavier than informational ones', async () => {
  const clean = await scanProjectRoot(await fixture({ 'a.js': "'use strict';\n" }), 'Software');
  const info = await scanProjectRoot(await fixture({ 'a.js': "'use strict';\nconsole.log(1);\n" }), 'Software');
  const critical = await scanProjectRoot(
    await fixture({ 'a.js': "'use strict';\nconst apiKey = \"super-secret-value\";\n" }),
    'Software',
  );
  assert.equal(clean.score, 100);
  assert.ok(info.score <= clean.score);
  assert.ok(critical.score < info.score);
});

test('counts every rule against every scanned file', async () => {
  const root = await fixture({ 'a.ts': 'export const a = 1;\n', 'b.ts': 'export const b = 2;\n' });
  const result = await scanProjectRoot(root, 'Library');
  assert.equal(result.filesScanned, 2);
  const distinctPerFile = rulesForKind('Library').filter((rule) => rule.lang === 'any' || rule.lang === 'js').length;
  assert.equal(result.rulesEvaluated, distinctPerFile * 2);
});

test('skips files larger than the size guard', async () => {
  const big = 'const pad = 1;\n'.repeat(80000) + 'eval(x);\n';
  const root = await fixture({ 'huge.js': "'use strict';\n" + big });
  const result = await scanProjectRoot(root, 'Software');
  assert.equal(result.filesScanned, 0);
});

// These assertions pin the scanner's real limits. If a future rule catches these,
// the test fails on purpose so the limitation list in README.md gets updated.
test('known gaps stay documented', async () => {
  const root = await fixture({
    'obfuscated.js': "'use strict';\nglobalThis['ev' + 'al'](payload);\n",
    'split-sql.py': 'query = "SELECT * FROM users WHERE id=%s" % user_id\n',
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.ok(!titles(result).includes('Dynamic code evaluation'), 'obfuscated eval is not detected yet');
  assert.ok(!titles(result).includes('SQL string concatenation'), 'generic variable-built SQL without an execute call is not detected yet');
});

test('detects SQL built with template interpolation', async () => {
  const root = await fixture({
    'db.ts': 'const rows = db.query(`SELECT * FROM users WHERE id=${userId}`);\n',
  });
  const result = await scanProjectRoot(root, 'API / backend');
  assert.ok(titles(result).includes('SQL string concatenation'));
});

test('lists supported Node test files for individual runs without executing them', async () => {
  const root = await fixture({
    'package.json': JSON.stringify({ scripts: { test: 'node --test tests/*.test.cjs' } }),
    'tests/one.test.cjs': 'test("one", () => {});\n',
    'tests/two.spec.js': 'test("two", () => {});\n',
    'src/not-a-test.js': 'export const value = 1;\n',
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.deepEqual(result.testFiles.map(({ file, runner }) => [file, runner]), [
    ['tests/one.test.cjs', 'node'],
    ['tests/two.spec.js', 'node'],
  ]);
  assert.equal(result.testCommand, 'npm test');
});

test('lists pytest files individually when the project declares pytest configuration', async () => {
  const root = await fixture({
    'pyproject.toml': '[tool.pytest.ini_options]\n',
    'tests/test_api.py': 'def test_api(): pass\n',
    'tests/api_test.py': 'def test_other(): pass\n',
    'src/api.py': 'def api(): pass\n',
  });
  const result = await scanProjectRoot(root, 'Software');
  assert.deepEqual(result.testFiles.map(({ file, runner }) => [file, runner]), [
    ['tests/api_test.py', 'pytest'],
    ['tests/test_api.py', 'pytest'],
  ]);
  assert.equal(result.testCommand, 'python -m pytest');
});

test('lists Go test functions and Cargo integration files as individual targets', async () => {
  const root = await fixture({
    'go.mod': 'module example.test/project\n',
    'pkg/api_test.go': 'package pkg\nfunc TestOne(t *testing.T) {}\nfunc TestTwo(t *testing.T) {}\n',
    'Cargo.toml': '[package]\nname = "example"\nversion = "0.1.0"\n',
    'tests/http_api.rs': '#[test]\nfn rejects_bad_input() {}\n',
  });
  const result = await scanProjectRoot(root, 'Software');
  const goTarget = result.testFiles.find((target) => target.runner === 'go');
  const cargoTarget = result.testFiles.find((target) => target.runner === 'cargo');

  assert.deepEqual(goTarget?.names, ['TestOne', 'TestTwo']);
  assert.equal(goTarget?.file, 'pkg/api_test.go');
  assert.equal(cargoTarget?.file, 'tests/http_api.rs');
  assert.equal(cargoTarget?.command, 'cargo test --test "http_api"');
});

test('tracks destructured request inputs and clears taint after sanitization', async () => {
  const root = await fixture({
    'taint.js': [
      "'use strict';",
      'const { rawHtml } = req.query;',
      'unsafeNode.innerHTML = rawHtml;',
      'const cleanHtml = DOMPurify.sanitize(rawHtml);',
      'sanitizedNode.innerHTML = cleanHtml;',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Website');
  const htmlFindings = result.findings.filter((finding) => finding.ruleId === 'js-innerhtml');
  assert.equal(htmlFindings.length, 2);
  assert.equal(htmlFindings[0].line, 3);
  assert.equal(htmlFindings[0].confidence, 'high');
  assert.equal(htmlFindings[1].line, 5);
  assert.equal(htmlFindings[1].confidence, 'medium');
});

test('exports valid SARIF 2.1.0 and Markdown reports', async () => {
  const root = await fixture({ 'app.js': "'use strict';\neval(userInput);\n" });
  const scan = await scanProjectRoot(root, 'Software');
  const sarif = toSarifReport(scan, { name: 'demo', root, kind: 'Software' });
  assert.equal(sarif.version, '2.1.0');
  assert.equal(sarif.runs[0].tool.driver.name, 'DeepCheck');
  assert.ok(sarif.runs[0].results.some((r) => r.ruleId === 'js-eval' && r.level === 'error'));
  const markdown = toMarkdownReport(scan, { name: 'demo', root, kind: 'Software' });
  assert.match(markdown, /# DeepCheck Audit Report — demo/);
  assert.match(markdown, /Dynamic code evaluation/);
});

test('detects deep JS logic, concurrency, prototype pollution, and web structural bugs', async () => {
  const root = await fixture({
    'logic.js': [
      "'use strict';",
      'const DB_PASSWORD = "SuperSecret123!";',
      'function average(nums) { let s = 0; for (let i = 0; i <= nums.length; i++) s += nums[i]; return s; }',
      'function isEven(n) { return n % 2 == 1; }',
      'function removeEvens(arr) { for (let i = 0; i < arr.length; i++) { if (arr[i] % 2 === 0) arr.splice(i, 1); } }',
      'function merge(a, b) { for (const k in b) a[k] = b[k]; }',
      'let counter = 0; async function inc() { const cur = counter; await Promise.resolve(); counter = cur + 1; }',
      'function token() { return Math.random().toString(36); }',
      'function okUrl(u) { return u.startsWith("https://example.com"); }',
      'if (user == "admin" && password == "admin") console.log(DB_PASSWORD);',
    ].join('\n'),
    'page.html': '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>T</title></head><body><button id="dup" type="button">A</button><button id="dup" type="button">B</button><script>for (let i = 0; i <= 5; i++) {}</script></body></html>',
    'bad.css': '.broken { content: "unterminated; }\n',
  });
  const result = await scanProjectRoot(root, 'Website');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  for (const expected of [
    'secret-generic-credential',
    'js-off-by-one-loop',
    'js-suspicious-parity',
    'js-array-splice-in-loop',
    'js-prototype-pollution',
    'js-async-race-condition',
    'js-math-random-secret',
    'js-bypassable-url-check',
    'js-hardcoded-auth',
    'js-log-sensitive',
    'html-duplicate-id',
    'css-unterminated-string',
  ]) {
    assert.ok(ruleIds.has(expected), `expected ${expected} to be detected`);
  }
});

test('skips safe numeric innerHTML accumulators and catches NaN/typeof/await/ReDoS bugs', async () => {
  const root = await fixture({
    'editor.js': [
      "'use strict';",
      'function renderLines(total, box) {',
      '  let html = "";',
      '  for (let i = 1; i <= total; i++) { html += `<span>${i}</span>`; }',
      '  box.innerHTML = html;',
      '  box.innerHTML = `<span>${total} line${total !== 1 ? "s" : ""}</span>`;',
      '}',
      'async function saveData(x) { return x; }',
      'async function run(x) { saveData(x); if (x === NaN || typeof x === "array") return /(a+)+/.test(x); }',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Website');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  assert.ok(!ruleIds.has('js-innerhtml'), 'safe numeric innerHTML should not be flagged as XSS');
  for (const expected of ['js-missing-await', 'js-nan-compare', 'js-invalid-typeof', 'js-redos-regex']) {
    assert.ok(ruleIds.has(expected), `expected ${expected} to be detected`);
  }
});

test('detects switch fallthrough, duplicate case, return in finally, and async Promise executor', async () => {
  const root = await fixture({
    'advanced.js': [
      "'use strict';",
      'function route(x) {',
      '  switch (x) {',
      '    case 1: doA();',
      '    case 1: doB(); break;',
      '    default: break;',
      '  }',
      '}',
      'function cleanup() { try { return 1; } finally { return 2; } }',
      'const p = new Promise(async (resolve) => { resolve(1); });',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  for (const expected of ['js-switch-fallthrough', 'js-duplicate-case', 'js-return-in-finally', 'js-async-promise-executor']) {
    assert.ok(ruleIds.has(expected), `expected ${expected} to be detected`);
  }
});

test('detects unbounded recursion, empty promise catch, and sparse array holes', async () => {
  const root = await fixture({
    'extra.js': [
      "'use strict';",
      'function spin(n) { return spin(n - 1); }',
      'const arr = [1, , 3];',
      'Promise.resolve(1).catch(() => {});',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  for (const expected of ['js-unbounded-recursion', 'js-sparse-array-hole', 'js-empty-promise-catch']) {
    assert.ok(ruleIds.has(expected), `expected ${expected} to be detected`);
  }
});

test('detects negated in/instanceof precedence, unawaited condition, throw literal, and /g regex in loop', async () => {
  const root = await fixture({
    'precedence.js': [
      "'use strict';",
      'async function checkAuth() { return true; }',
      'function inspect(k, obj, s) {',
      '  if (!k in obj) return false;',
      '  if (checkAuth()) return true;',
      '  while (/a/g.test(s)) { break; }',
      '  throw "bad input";',
      '}',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  for (const expected of ['js-negated-in-instanceof', 'js-promise-in-condition', 'js-global-regex-in-loop', 'js-throw-literal']) {
    assert.ok(ruleIds.has(expected), `expected ${expected} to be detected`);
  }
});

test('detects ignored return values of immutable string and array methods', async () => {
  const root = await fixture({
    'immutable.js': [
      "'use strict';",
      'function clean(name, items) {',
      '  name.trim();',
      '  items.filter((x) => x > 0);',
      '  return items;',
      '}',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const ignored = result.findings.filter((f) => f.ruleId === 'js-ignored-immutable-return');
  assert.equal(ignored.length, 2);
});

test('detects missing return in array callbacks and duplicate else-if branches', async () => {
  const root = await fixture({
    'callbacks.js': [
      "'use strict';",
      'function transform(items, x) {',
      '  const mapped = items.map((n) => { n * 2; });',
      '  if (x === 1) return mapped;',
      '  else if (x === 1) return [];',
      '  return mapped;',
      '}',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Software');
  const ruleIds = new Set(result.findings.map((f) => f.ruleId));
  assert.ok(ruleIds.has('js-missing-array-return'));
  assert.ok(ruleIds.has('js-duplicate-else-if'));
});

test('allows Android MAIN/LAUNCHER exported activity while flagging non-launcher exported components', async () => {
  const root = await fixture({
    'AndroidManifest.xml': [
      '<manifest>',
      '  <application android:allowBackup="false">',
      '    <activity android:name=".MainActivity" android:exported="true">',
      '      <intent-filter>',
      '        <action android:name="android.intent.action.MAIN" />',
      '        <category android:name="android.intent.category.LAUNCHER" />',
      '      </intent-filter>',
      '    </activity>',
      '    <activity android:name=".DebugActivity" android:exported="true" />',
      '  </application>',
      '</manifest>',
    ].join('\n'),
  });
  const result = await scanProjectRoot(root, 'Mobile app');
  const exported = result.findings.filter((f) => f.ruleId === 'mob-android-exported');
  assert.equal(exported.length, 1);
  assert.equal(exported[0].line, 9);
});

test('scans the DeepCheck repo itself without crashing', async () => {
  const root = path.resolve(__dirname, '..');
  const result = await scanProjectRoot(root, 'Software');
  assert.ok(result.filesScanned > 10, `expected a real repo scan, got ${result.filesScanned} files`);
  assert.ok(result.durationMs < 60000, `scan took ${result.durationMs}ms`);
  assert.ok(result.findings.every((f) => f.file && typeof f.line === 'number'));
  assert.ok(Array.isArray(result.findings));
});
