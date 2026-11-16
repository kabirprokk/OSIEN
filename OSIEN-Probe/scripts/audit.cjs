'use strict';
// deepcheck-disable-file js-console-log js-process-exit

// Command-line audit:
//   node scripts/audit.cjs <path> [category] [--json | --sarif | --markdown] [--fail-on <severity>]
// Runs the same scanner the desktop app uses and prints a summary or structured report.

const path = require('node:path');
const { scanProjectRoot, toSarifReport, toMarkdownReport, kinds } = require('../electron/scanner.cjs');

(async () => {
  const args = process.argv.slice(2);
  const flags = new Set(args.filter((arg) => arg.startsWith('--')));
  const failOnIndex = args.indexOf('--fail-on');
  const failOn = failOnIndex >= 0 ? args[failOnIndex + 1] : null;
  const positional = args.filter((arg, index) => !arg.startsWith('--') && (failOnIndex < 0 || index !== failOnIndex + 1));

  const target = positional[0];
  const kind = positional[1] || 'Software';
  if (!target) {
    console.error(`Usage: node scripts/audit.cjs <path> [${kinds.join(' | ')}] [--json | --sarif | --markdown] [--fail-on <severity>]`);
    process.exit(2);
  }
  if (!kinds.includes(kind)) {
    console.error(`Unknown category "${kind}". Use one of: ${kinds.join(', ')}`);
    process.exit(2);
  }

  const root = path.resolve(target);
  const started = Date.now();
  const result = await scanProjectRoot(root, kind);
  const projectMeta = { name: path.basename(root), root, kind };

  if (flags.has('--sarif')) {
    console.log(JSON.stringify(toSarifReport(result, projectMeta), null, 2));
  } else if (flags.has('--json')) {
    console.log(JSON.stringify(result, null, 2));
  } else if (flags.has('--markdown') || flags.has('--md')) {
    console.log(toMarkdownReport(result, projectMeta));
  } else {
    const bySeverity = { critical: 0, high: 0, medium: 0, low: 0, info: 0 };
    for (const finding of result.findings) bySeverity[finding.severity]++;

    console.log(`Path:        ${root}`);
    console.log(`Category:    ${kind}`);
    console.log(`Files:       ${result.filesScanned}`);
    console.log(`Rule checks: ${result.rulesEvaluated.toLocaleString()}`);
    console.log(`Findings:    ${result.findings.length}${result.truncated ? ' (truncated at limit)' : ''}`);
    console.log(`Score:       ${result.score}/100`);
    console.log(`Duration:    ${Date.now() - started}ms`);
    console.log(
      `Severity:    ${Object.entries(bySeverity)
        .map(([name, count]) => `${name}=${count}`)
        .join(' ')}`,
    );
    if (result.testCommand) console.log(`Detected test command: ${result.testCommand}`);

    const order = ['critical', 'high', 'medium', 'low', 'info'];
    const sorted = [...result.findings].sort(
      (a, b) => order.indexOf(a.severity) - order.indexOf(b.severity),
    );
    console.log('');
    for (const finding of sorted) {
      console.log(`[${finding.severity.toUpperCase()} · ${finding.confidence || 'medium'} confidence] ${finding.title} — ${finding.file}:${finding.line}`);
      if (finding.cwe) console.log(`    ${finding.cwe}: ${finding.hint}`);
      if (finding.reason) console.log(`    Why: ${finding.reason}`);
      if (finding.excerpt) console.log(`    ${finding.excerpt}`);
    }
  }

  if (failOn) {
    const order = ['critical', 'high', 'medium', 'low', 'info'];
    const threshold = order.indexOf(failOn.toLowerCase());
    if (threshold >= 0 && result.findings.some((f) => order.indexOf(f.severity) <= threshold)) {
      process.exit(1);
    }
  }
})();
