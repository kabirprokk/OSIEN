#!/usr/bin/env node
'use strict';
// deepcheck-disable-file js-console-log js-process-exit

const fs = require('node:fs/promises');
const path = require('node:path');
const pkg = require('../package.json');
const {
  scanProjectRoot,
  rulesForKind,
  toSarifReport,
  toMarkdownReport,
  kinds,
  SEVERITIES = ['critical', 'high', 'medium', 'low', 'info'],
} = require('../electron/scanner.cjs');
const { checkAiHealth } = require('../electron/ai-review.cjs');

function printHelp() {
  console.log(`OSIEN Probe v${pkg.version} — Offline static analysis, AST bug finder, and AI verifier

Usage:
  osien-probe scan [path] [options]   Scan a project directory (default command)
  osien-probe rules [options]         List available rules and CWE mappings
  osien-probe ai-status               Check local Ollama AI review availability

Options:
  --kind, -k <category>   Project category (${kinds.join(' | ')}) [default: Software]
  --json                  Output raw JSON scan report
  --sarif                 Output SARIF 2.1.0 report
  --markdown, --md        Output Markdown report
  --out, -o <file>        Write output report to a file
  --fail-on <severity>    Exit 1 if any finding is at or above (critical|high|medium|low|info)
  --quiet, -q             Only print summary counts (no per-finding lines)
  --help, -h              Show this help
  --version, -v           Print version`);
}

function parseArgs(argv) {
  const opts = {
    command: 'scan',
    target: '.',
    kind: 'Software',
    format: 'text',
    out: null,
    failOn: null,
    quiet: false,
    help: false,
    version: false,
  };
  const positional = [];
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--help' || arg === '-h') opts.help = true;
    else if (arg === '--version' || arg === '-v') opts.version = true;
    else if (arg === '--json') opts.format = 'json';
    else if (arg === '--sarif') opts.format = 'sarif';
    else if (arg === '--markdown' || arg === '--md') opts.format = 'markdown';
    else if (arg === '--quiet' || arg === '-q') opts.quiet = true;
    else if ((arg === '--kind' || arg === '-k') && argv[i + 1]) opts.kind = argv[++i];
    else if ((arg === '--out' || arg === '-o') && argv[i + 1]) opts.out = argv[++i];
    else if (arg === '--fail-on' && argv[i + 1]) opts.failOn = argv[++i];
    else if (!arg.startsWith('-')) positional.push(arg);
  }
  if (['scan', 'rules', 'ai-status'].includes(positional[0])) {
    opts.command = positional.shift();
  }
  if (positional[0]) opts.target = positional[0];
  if (positional[1] && kinds.includes(positional[1])) opts.kind = positional[1];
  return opts;
}

async function run() {
  const opts = parseArgs(process.argv.slice(2));
  if (opts.version) {
    console.log(pkg.version);
    return;
  }
  if (opts.help) {
    printHelp();
    return;
  }

  if (opts.command === 'ai-status') {
    const status = await checkAiHealth();
    console.log(JSON.stringify(status, null, 2));
    process.exit(status.available && status.modelInstalled ? 0 : 1);
  }

  if (!kinds.includes(opts.kind)) {
    console.error(`Unknown category "${opts.kind}". Use one of: ${kinds.join(', ')}`);
    process.exit(2);
  }

  if (opts.command === 'rules') {
    const rules = rulesForKind(opts.kind);
    if (opts.format === 'json') {
      console.log(JSON.stringify(rules.map(({ id, title, severity, cwe, lang, hint }) => ({ id, title, severity, cwe, lang, hint })), null, 2));
    } else {
      console.log(`OSIEN Probe rules for "${opts.kind}" (${rules.length} rules):`);
      for (const r of rules) console.log(`- [${r.severity.toUpperCase()}] ${r.id} (${r.cwe}, ${r.lang}): ${r.title}`);
    }
    return;
  }

  const root = path.resolve(opts.target);
  const result = await scanProjectRoot(root, opts.kind);
  const projectMeta = { name: path.basename(root), root, kind: opts.kind };

  let output = '';
  if (opts.format === 'sarif') {
    output = JSON.stringify(toSarifReport(result, projectMeta), null, 2);
  } else if (opts.format === 'json') {
    output = JSON.stringify(result, null, 2);
  } else if (opts.format === 'markdown') {
    output = toMarkdownReport(result, projectMeta);
  } else {
    const lines = [
      `OSIEN Probe v${pkg.version}`,
      `Path:        ${root}`,
      `Category:    ${opts.kind}`,
      `Files:       ${result.filesScanned}`,
      `Rule checks: ${result.rulesEvaluated.toLocaleString()}`,
      `Findings:    ${result.findings.length}${result.truncated ? ' (truncated at limit)' : ''}`,
      `Score:       ${result.score}/100`,
      `Duration:    ${result.durationMs}ms`,
      `Severity:    ${Object.entries(result.severityCounts).map(([k, v]) => `${k}=${v}`).join(' ')}`,
    ];
    if (result.testCommand) lines.push(`Test cmd:    ${result.testCommand}`);
    if (!opts.quiet && result.findings.length) {
      lines.push('');
      for (const f of result.findings) {
        lines.push(`[${f.severity.toUpperCase()} · ${f.confidence || 'medium'}] ${f.title} (${f.ruleId}) — ${f.file}:${f.line}`);
        if (f.cwe || f.hint) lines.push(`    ${f.cwe ? `${f.cwe}: ` : ''}${f.hint || ''}`);
        if (f.reason) lines.push(`    Why: ${f.reason}`);
        if (f.excerpt) lines.push(`    ${f.excerpt}`);
      }
    }
    output = lines.join('\n');
  }

  if (opts.out) {
    await fs.mkdir(path.dirname(path.resolve(opts.out)), { recursive: true });
    await fs.writeFile(path.resolve(opts.out), output, 'utf8');
    console.error(`Report saved to ${path.resolve(opts.out)}`);
  } else {
    console.log(output);
  }

  if (opts.failOn) {
    const threshold = SEVERITIES.indexOf(opts.failOn.toLowerCase());
    if (threshold >= 0 && result.findings.some((f) => SEVERITIES.indexOf(f.severity) <= threshold)) {
      process.exit(1);
    }
  }
}

run().catch((error) => {
  console.error(error?.message || String(error));
  process.exit(2);
});
