'use strict';

// Real-world clean-code regression suite.
// Ensures modern valid idioms across languages never trigger critical, high, or medium false positives.

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { scanProjectRoot } = require('../electron/scanner.cjs');

async function scanClean(files, kind = 'Software') {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'osien-probe-clean-'));
  try {
    for (const [rel, content] of Object.entries(files)) {
      const full = path.join(dir, rel);
      await fs.mkdir(path.dirname(full), { recursive: true });
      await fs.writeFile(full, content, 'utf8');
    }
    return await scanProjectRoot(dir, kind);
  } finally {
    await fs.rm(dir, { recursive: true, force: true });
  }
}

test('clean modern Kotlin, Jetpack Compose, and AndroidManifest produce zero critical/high/medium false positives', async () => {
  const result = await scanClean({
    'app/src/main/AndroidManifest.xml': [
      '<manifest>',
      '  <application android:allowBackup="false" android:usesCleartextTraffic="false">',
      '    <activity android:name=".MainActivity" android:exported="true">',
      '      <intent-filter>',
      '        <action android:name="android.intent.action.MAIN" />',
      '        <category android:name="android.intent.category.LAUNCHER" />',
      '      </intent-filter>',
      '    </activity>',
      '  </application>',
      '</manifest>',
    ].join('\n'),
    'app/src/main/java/QuizEngine.kt': [
      'package com.example.app',
      'val bands = listOf(0.2f, 0.6f, 0.9f)',
      'val mid = bands.filter { it in 0.5f..<0.8f }',
      'val label = when (mid.size) {',
      '  0 -> "None"',
      '  else -> if (mid.isNotEmpty()) "🏆 Good" else "Low"',
      '}',
      '@Composable fun CardRow(content: @Composable RowScope.() -> Unit) {}',
    ].join('\n'),
  }, 'Mobile app');

  const serious = result.findings.filter((f) => ['critical', 'high', 'medium'].includes(f.severity));
  assert.deepEqual(serious, [], `Unexpected serious findings: ${JSON.stringify(serious)}`);
});

test('clean modern Node/Express, Firestore, Promise chains, and tests produce zero critical/high/medium false positives', async () => {
  const result = await scanClean({
    'src/service.js': [
      "'use strict';",
      'const rateLimit = require("express-rate-limit");',
      'async function updateLead(tx, ref, data) {',
      '  tx.update(ref, { expiresAt: Date.now() + 1000, ...data });',
      '}',
      'function enqueue(previous, op) {',
      '  const task = previous.catch(() => {}).then(op);',
      '  return task;',
      '}',
      'const repo = {',
      '  collection(name) { return { doc: (id) => ({ collection: name, id }) }; },',
      '  set(ref, payload) { return { ref, payload }; },',
      '};',
      'module.exports = { updateLead, enqueue, repo, rateLimit };',
    ].join('\n'),
    'test/service.test.js': [
      "'use strict';",
      'const test = require("node:test");',
      'const assert = require("node:assert/strict");',
      'process.env.DASHBOARD_PASSWORD = "export-password";',
      'test("selection count", () => {',
      '  const beforeSocialSelection = 1;',
      '  assert.equal(2, beforeSocialSelection + 1, "a prompt with choices");',
      '});',
    ].join('\n'),
  }, 'API / backend');

  const serious = result.findings.filter((f) => ['critical', 'high', 'medium'].includes(f.severity));
  assert.deepEqual(serious, [], `Unexpected serious findings: ${JSON.stringify(serious)}`);
});

test('clean modern Python, Go, Rust, and Java produce zero critical/high/medium false positives', async () => {
  const result = await scanClean({
    'app.py': [
      'from dataclasses import dataclass',
      '@dataclass',
      'class User:',
      '    name: str',
      'def greet(user: User) -> str:',
      '    return f"Hello, {user.name}"',
    ].join('\n'),
    'main.go': [
      'package main',
      'import "fmt"',
      'func sum[T ~int | ~float64](items []T) T {',
      '    var total T',
      '    for _, v := range items { total += v }',
      '    return total',
      '}',
      'func main() { fmt.Println(sum([]int{1, 2, 3})) }',
    ].join('\n'),
    'lib.rs': [
      'pub async fn add(a: i32, b: i32) -> i32 { a + b }',
    ].join('\n'),
    'Main.java': [
      'public record Point(int x, int y) {}',
    ].join('\n'),
  }, 'Software');

  const serious = result.findings.filter((f) => ['critical', 'high', 'medium'].includes(f.severity));
  assert.deepEqual(serious, [], `Unexpected serious findings: ${JSON.stringify(serious)}`);
});
