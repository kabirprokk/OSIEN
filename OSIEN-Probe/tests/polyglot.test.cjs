'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { scanProjectRoot } = require('../electron/scanner.cjs');

async function scanFixtures(files) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'deepcheck-polyglot-'));
  for (const [name, source] of Object.entries(files)) {
    await fs.writeFile(path.join(root, name), source, 'utf8');
  }
  try {
    return await scanProjectRoot(root, 'Software');
  } finally {
    await fs.rm(root, { recursive: true, force: true });
  }
}

test('language AST rules catch dangerous APIs across supported languages', async () => {
  const result = await scanFixtures({
    'unsafe.py': 'import pickle, requests\nvalue = pickle.loads(data)\nrequests.get("https://example.com")\n',
    'unsafe.go': 'package main\nimport "os/exec"\nfunc f(cmd string) { exec.Command("sh", "-c", cmd); md5.New(); http.ListenAndServe(":8080", nil) }\n',
    'unsafe.rs': 'use std::process::Command;\nfn f(cmd: &str) { Command::new("sh").arg("-c").arg(cmd); }\n',
    'unsafe.java': 'class A { Object f(InputStream i) throws Exception { MessageDigest.getInstance("MD5"); return new ObjectInputStream(i).readObject(); } }\n',
    'unsafe.php': '<?php unserialize($data); system($cmd);\n',
    'unsafe.rb': 'Marshal.load(data)\nsystem(cmd)\n',
    'unsafe.c': 'void f(char *x) { gets(x); sprintf(x, "%s", x); system(x); }\n',
    'unsafe.cs': 'new BinaryFormatter();\nnew LosFormatter();\nMD5.Create();\n',
    'unsafe.swift': 'let obj = NSKeyedUnarchiver.unarchiveObject(with: data)\n',
    'unsafe.kt': 'fun f(s: InputStream) = ObjectInputStream(s).readObject()\n',
    'unsafe.sh': 'eval "$COMMAND"\n',
  });
  const detected = new Set(result.findings.map((finding) => finding.ruleId));

  for (const ruleId of [
    'py-pickle',
    'polyglot-python-requests-timeout',
    'polyglot-go-shell',
    'polyglot-go-weak-hash',
    'polyglot-go-cleartext-http',
    'polyglot-rust-shell',
    'polyglot-java-deserialize',
    'polyglot-java-weak-hash',
    'polyglot-php-deserialize',
    'polyglot-php-shell',
    'polyglot-ruby-deserialize',
    'polyglot-ruby-shell',
    'polyglot-cpp-gets',
    'polyglot-cpp-sprintf',
    'polyglot-cpp-system',
    'polyglot-csharp-binaryformatter',
    'polyglot-csharp-losformatter',
    'polyglot-csharp-weak-hash',
    'polyglot-swift-deserialize',
    'polyglot-kotlin-deserialize',
  ]) assert.ok(detected.has(ruleId), `expected ${ruleId}`);

  assert.ok(!detected.has('polyglot-python-pickle'), 'existing Python rule should not be duplicated');
  assert.ok(!detected.has('polyglot-bash-eval'), 'existing shell eval rule should not be duplicated');
  assert.equal(result.findings.find((finding) => finding.ruleId === 'polyglot-java-deserialize').line, 1);
});

test('language AST rules ignore comments and honor inline suppression', async () => {
  const result = await scanFixtures({
    'safe.py': '# eval(user_input)\nimport json\nvalue = json.loads(data)\n',
    'safe.go': 'package main\n// exec.Command("sh", "-c", userInput)\nfunc f() {}\n',
    'suppressed.php': '<?php unserialize($data); // deepcheck-disable-line polyglot-php-deserialize\n',
  });
  const detected = result.findings.map((finding) => finding.ruleId);

  assert.ok(!detected.some((ruleId) => ruleId.startsWith('polyglot-')));
});

test('reports parser-marked syntax errors without treating valid code as erroneous', async () => {
  const result = await scanFixtures({
    'broken.go': 'package main\nfunc broken( {\n',
    'broken.rs': 'fn broken( {\n',
    'valid.go': 'package main\nfunc okay() {}\n',
    'modern.kt': [
      'val r = radarData.filter { it.score in 0.5f..<0.8f }',
      '@Composable fun Box(content: @Composable RowScope.() -> Unit) {}',
      'val msg = when (res) { is Ok -> if (!host) "🏆 WIN" else "OK"; else -> "ERR" }',
    ].join('\n'),
  });
  const syntaxFindings = result.findings.filter((finding) => finding.ruleId === 'polyglot-syntax-error');

  assert.deepEqual(syntaxFindings.map((finding) => finding.file).sort(), ['broken.go', 'broken.rs']);
});
