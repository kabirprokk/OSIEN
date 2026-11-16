'use strict';

// Scanning engine. Pure Node, no Electron import, so it can be tested directly.
//
// Pipeline per file: read -> pick language -> strip comments (code view) ->
// run line rules -> run whole-file rules -> apply suppressions.

const path = require('node:path');
const fs = require('node:fs/promises');
const { analyzeJavaScriptAst } = require('./js-analyzer.cjs');
const { analyzePolyglot, languageByExtension } = require('./polyglot.cjs');
const { shared, categoryRules, SEVERITY_WEIGHT } = require('./rules.cjs');

const kinds = ['Website', 'Software', 'API / backend', 'Mobile app', 'Library', 'Data / ML'];

const MAX_FINDINGS = 1000;
const MAX_FILES = 6000;
const MAX_FILE_BYTES = 1024 * 1024;
const MIN_SECRET_ENTROPY = 3.0;
// These rules need literal values to distinguish safe from unsafe code, but
// still must ignore lines that are only code samples inside source strings.
const rawCodeRuleIds = new Set([
  'js-timeout-string', 'js-dynamic-require', 'py-subprocess-string',
  'py-sql-fstring', 'py-sql-percent', 'sw-env-secret-fallback', 'api-no-rate-limit',
]);
const polyglotLegacyRuleIds = {
  'polyglot-python-eval': new Set(['py-eval']),
  'polyglot-python-exec': new Set(['py-exec']),
  'polyglot-python-pickle': new Set(['py-pickle']),
  'polyglot-python-marshal': new Set(['py-marshal-load']),
  'polyglot-python-yaml-load': new Set(['py-yaml-load']),
  'polyglot-python-shell': new Set(['py-os-system', 'py-subprocess-shell', 'py-subprocess-string']),
  'polyglot-bash-eval': new Set(['sh-eval']),
};

const ignoredDirs = new Set([
  '.git', 'node_modules', 'dist', 'build', 'coverage', '.next', '.turbo', '.cache',
  'vendor', 'target', '.venv', 'venv', 'out', 'bin', 'obj', '.idea', '.gradle',
]);
const ignoredFiles = new Set([
  'package-lock.json', 'yarn.lock', 'pnpm-lock.yaml', 'bun.lockb', 'composer.lock',
  'poetry.lock', 'Cargo.lock', 'Gemfile.lock', 'go.sum', '.ds_store',
]);

const langByExt = {
  '.js': 'js', '.jsx': 'js', '.cjs': 'js', '.mjs': 'js', '.ts': 'js', '.tsx': 'js',
  '.vue': 'js', '.svelte': 'js',
  '.py': 'py',
  '.html': 'html', '.htm': 'html',
  '.xml': 'html',
  '.css': 'css', '.scss': 'css', '.sass': 'css', '.less': 'css',
  '.json': 'json',
  '.yml': 'yaml', '.yaml': 'yaml',
  '.sh': 'sh', '.bash': 'sh', '.zsh': 'sh', '.ps1': 'sh',
  '.sql': 'sql',
  '.go': 'generic', '.rs': 'generic', '.java': 'generic', '.kt': 'generic', '.kts': 'generic',
  '.php': 'generic', '.rb': 'generic', '.cs': 'generic', '.c': 'generic', '.h': 'generic',
  '.cc': 'generic', '.cpp': 'generic', '.hpp': 'generic', '.swift': 'generic', '.dart': 'generic',
  '.scala': 'generic', '.toml': 'generic', '.ini': 'generic', '.cfg': 'generic', '.conf': 'generic',
  '.properties': 'generic', '.gradle': 'generic', '.tf': 'generic',
};

const sourceExt = new Set(Object.keys(langByExt));
const extensionlessSources = new Set(['dockerfile', 'makefile', 'jenkinsfile', '.env', '.npmrc', '.netrc', '.pypirc']);

function isExtensionlessSource(name) {
  return extensionlessSources.has(name.toLowerCase()) || /^\.env(?:\..+)?$/i.test(name);
}

const PLACEHOLDER = /^(?:example|sample|changeme|change-me|your[-_].*|placeholder|dummy|fake|xxxx+|<.*>|\$\{.*\}|process\.env.*|os\.environ.*|password|passwd|secret|token|abc123|foo|bar|none|null|(?:export|reset|test|mock|demo|ephemeral)[-_].*)$/i;
const TEST_FILE_PATH = /(?:^|[\\/])(?:tests?|__tests__|spec)[\\/]|(?:\.test|\.spec)\.[cm]?[jt]sx?$/i;

function looksLikePlaceholder(value) {
  return PLACEHOLDER.test(value) || /^\*+$/.test(value) || /(?:^|[-_])(?:test|mock|dummy|fake|example)(?:[-_]|$)/i.test(value);
}

function entropy(value) {
  if (!value) return 0;
  const counts = new Map();
  for (const char of value) counts.set(char, (counts.get(char) || 0) + 1);
  let result = 0;
  for (const count of counts.values()) {
    const p = count / value.length;
    result -= p * Math.log2(p);
  }
  return result;
}

// Replace comment bodies (and optionally string contents) with spaces so code
// rules never fire on prose, while keeping every offset and line number intact.
// Quote characters survive, so rules about string usage still see the quotes.
function mask(text, lang, blankStrings) {
  if (!['js', 'py', 'css', 'sh', 'sql', 'html', 'generic'].includes(lang)) return text;
  const lineMarker = lang === 'py' || lang === 'sh' ? '#' : lang === 'sql' ? '--' : lang === 'css' ? null : '//';
  const ranges = [];
  let state = 'code';
  let quote = '';
  let commentStart = -1;
  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    const next = text[i + 1];
    if (state === 'code') {
      if (lang !== 'html' && (char === '"' || char === "'" || char === '`')) { state = 'string'; quote = char; continue; }
      if (lineMarker && text.startsWith(lineMarker, i)) { state = 'line'; commentStart = i; continue; }
      if (lang === 'html' && text.startsWith('<!--', i)) { state = 'block'; commentStart = i; i += 3; continue; }
      if (['js', 'css', 'generic'].includes(lang) && char === '/' && next === '*') { state = 'block'; commentStart = i; i++; continue; }
      continue;
    }
    if (state === 'string') {
      if (char === '\\') { i++; continue; }
      if (char === quote) { state = 'code'; continue; }
      continue;
    }
    if (state === 'line') {
      if (char === '\n') { ranges.push([commentStart, i]); state = 'code'; }
      continue;
    }
    if (state === 'block') {
      if (lang === 'html' && text.startsWith('-->', i)) { ranges.push([commentStart, i + 3]); state = 'code'; i += 2; continue; }
      if (lang !== 'html' && char === '*' && next === '/') { ranges.push([commentStart, i + 2]); state = 'code'; i++; }
    }
  }
  if (state === 'line') ranges.push([commentStart, text.length]);
  if (state === 'block') ranges.push([commentStart, text.length]);
  if (blankStrings || ranges.length === 0) return blankStrings ? maskCommentsThenStrings(text, lang, ranges) : text;
  const chunks = [];
  let cursor = 0;
  for (const [start, end] of ranges) {
    chunks.push(text.slice(cursor, start), text.slice(start, end).replace(/[^\n]/g, ' '));
    cursor = end;
  }
  chunks.push(text.slice(cursor));
  return chunks.join('');
}

function maskCommentsThenStrings(text, lang, ranges) {
  const chunks = [];
  let cursor = 0;
  for (const [start, end] of ranges) {
    chunks.push(text.slice(cursor, start), text.slice(start, end).replace(/[^\n]/g, ' '));
    cursor = end;
  }
  chunks.push(text.slice(cursor));
  const uncommented = chunks.join('');
  const output = [];
  let state = 'code';
  let quote = '';
  for (let i = 0; i < uncommented.length; i++) {
    const char = uncommented[i];
    if (state === 'code' && (char === '"' || char === "'" || char === '`')) { output.push(' '); state = 'string'; quote = char; continue; }
    if (state === 'string' && char === '\\') {
      output.push(' ');
      if (i + 1 < uncommented.length) output.push(uncommented[++i] === '\n' ? '\n' : ' ');
      continue;
    }
    if (state === 'string' && char === quote) { output.push(' '); state = 'code'; continue; }
    output.push(state === 'string' && char !== '\n' ? ' ' : char);
  }
  return output.join('');
}

function stripComments(text, lang, blankStrings = false) {
  return mask(text, lang, blankStrings);
}

function suppressionMap(lines) {
  const byLine = new Map();
  const fileWide = new Set();
  const allLines = { value: false };

  const add = (lineNumber, ids) => {
    const set = byLine.get(lineNumber) || new Set();
    if (!ids.length) set.add('*');
    for (const id of ids) set.add(id);
    byLine.set(lineNumber, set);
  };

  for (let i = 0; i < Math.min(lines.length, 20); i++) {
    const match = lines[i].match(/deepcheck-disable-file\b(.*)$/);
    if (!match) continue;
    const ids = match[1].replace(/[/*-]+$/, '').split(/[\s,]+/).filter(Boolean);
    if (!ids.length) allLines.value = true;
    for (const id of ids) fileWide.add(id);
  }
  lines.forEach((line, index) => {
    const next = line.match(/deepcheck-disable-next-line\b(.*)$/);
    if (next) add(index + 2, next[1].replace(/[/*-]+$/, '').split(/[\s,]+/).filter(Boolean));
    const current = line.match(/deepcheck-disable-line\b(.*)$/);
    if (current) {
      const ids = current[1].replace(/[/*-]+$/, '').trim().split(/[\s,]+/).filter(Boolean);
      add(index + 1, ids);
    }
  });
  return { byLine, fileWide, allLines };
}

function suppressed(suppressions, lineNumber, ruleId) {
  if (suppressions.allLines.value) return true;
  if (suppressions.fileWide.has(ruleId)) return true;
  const set = suppressions.byLine.get(lineNumber);
  return Boolean(set && (set.has('*') || set.has(ruleId)));
}

function globToRegExp(glob) {
  let pattern = glob.trim().replace(/\\/g, '/');
  const directoryOnly = pattern.endsWith('/');
  pattern = pattern.replace(/\/+$/, '');
  if (!pattern) return null;
  let source = '';
  for (let i = 0; i < pattern.length; i++) {
    const char = pattern[i];
    if (char === '*') {
      if (pattern[i + 1] === '*') {
        source += '.*';
        i++;
        if (pattern[i + 1] === '/') { source += '?'; i++; }
      } else source += '[^/]*';
    } else if (char === '?') {
      source += '.';
    } else if ('\\^$.|+()[]{}'.includes(char)) {
      source += `\\${char}`;
    } else {
      source += char;
    }
  }
  const prefix = pattern.includes('/') ? '^' : '(?:^|.*/)';
  const suffix = directoryOnly ? '(?:/.*)?$' : '$';
  return new RegExp(`${prefix}${source}${suffix}`);
}

async function readIgnorePatterns(root) {
  const raw = await fs.readFile(path.join(root, '.deepcheckignore'), 'utf8').catch(() => '');
  return raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))
    .map(globToRegExp)
    .filter(Boolean);
}

function makeFinding(rule, file, line, excerpt, context = {}) {
  return {
    id: `${file}:${line}:${rule.id}`,
    ruleId: rule.id,
    title: rule.title,
    severity: rule.severity,
    cwe: rule.cwe,
    hint: rule.hint,
    confidence: context.confidence || 'medium',
    reason: context.reason || 'Pattern match; validate the code context.',
    file,
    line,
    excerpt: String(excerpt || '').trim().slice(0, 220),
  };
}

function dedupeFindings(findings) {
  const specificSecretLocations = new Set();
  const sensitiveLogLocations = new Set();
  for (const finding of findings) {
    const loc = `${finding.file}:${finding.line}`;
    if (finding.ruleId?.startsWith('secret-') && finding.ruleId !== 'secret-generic-credential') {
      specificSecretLocations.add(loc);
    }
    if (finding.ruleId === 'js-log-sensitive') {
      sensitiveLogLocations.add(loc);
    }
  }
  const seen = new Set();
  return findings.filter((finding) => {
    const loc = `${finding.file}:${finding.line}`;
    if (finding.ruleId === 'secret-generic-credential' && specificSecretLocations.has(loc)) return false;
    if (finding.ruleId === 'js-console-log' && sensitiveLogLocations.has(loc)) return false;
    const key = `${loc}:${finding.ruleId}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function rulesForKind(kind) {
  const all = [...shared, ...(categoryRules[kind] || [])];
  const seen = new Set();
  return all.filter((rule) => {
    if (seen.has(rule.id)) return false;
    if (rule.kinds && !rule.kinds.includes(kind)) return false;
    seen.add(rule.id);
    return true;
  });
}

function appliesTo(rule, lang) {
  if (rule.lang !== 'any' && rule.lang !== lang) return false;
  return true;
}

function secretAllowed(rule, line) {
  if (!rule.secretGroup) return true;
  const match = rule.pattern.exec(line);
  const value = match && match[rule.secretGroup];
  if (!value) return false;
  const normalized = value.replace(/^(?:sk|rk)_(?:live|test)_/, '');
  return !looksLikePlaceholder(value) && entropy(normalized) >= MIN_SECRET_ENTROPY;
}

function scoreFor(findings) {
  const penalty = findings.reduce((total, finding) => total + (SEVERITY_WEIGHT[finding.severity] || 0), 0);
  return Math.max(1, Math.round((100 * 100) / (100 + penalty)));
}

const USER_INPUT = /(?:req\.(?:body|query|params|headers|cookies)|request\.(?:body|query|params|headers|cookies)|searchParams\.|URLSearchParams|location\.(?:search|hash)|event\.target\.value|e\.target\.value|process\.argv|stdin|userInput|user_input|untrusted|payload|message)/i;
const INPUT_NAME = /(?:user|input|query|param|request|payload|message|comment|content|body|search|value)/i;
const SANITIZER = /\b(?:DOMPurify\.sanitize|sanitizeHtml|escapeHtml|escapeRegExp|encodeURIComponent|encodeURI|Number|parseInt|parseFloat|Boolean|validator\.escape|path\.basename|pick|omit|html\.escape|shlex\.quote|ast\.literal_eval)\s*\(/;

function evaluateTaint(lines, lang) {
  const tainted = new Set();
  const byLine = [];
  const containsIdentifier = (source, name) => {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return new RegExp(`(^|[^\\w$])${escaped}(?=$|[^\\w$])`).test(source);
  };
  const isTaintedExpr = (rhs) => {
    if (SANITIZER.test(rhs)) return false;
    return USER_INPUT.test(rhs) || [...tainted].some((source) => containsIdentifier(rhs, source));
  };
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (lang === 'js') {
      const destructured = line.match(/(?:^|[;,{]\s*)(?:const|let|var)\s*\{([^}]+)\}\s*=\s*([^;]+)/);
      if (destructured) {
        const [, bindings, rhs] = destructured;
        const rhsTainted = isTaintedExpr(rhs);
        for (const part of bindings.split(',')) {
          const cleaned = part.split('=')[0].trim();
          if (!cleaned || cleaned.startsWith('...')) continue;
          const segments = cleaned.split(':').map((s) => s.trim()).filter(Boolean);
          const localName = segments[segments.length - 1];
          if (/^[A-Za-z_$][\w$]*$/.test(localName)) {
            tainted.delete(localName);
            if (rhsTainted) tainted.add(localName);
          }
        }
      }
    }
    let assignment;
    if (lang === 'js') {
      assignment = line.match(/(?:^|[;,{]\s*)(?:const|let|var)?\s*([A-Za-z_$][\w$]*)\s*=\s*([^;]+)/);
    } else if (lang === 'py') {
      assignment = line.match(/^\s*([A-Za-z_]\w*)\s*=\s*(.+)$/);
    }
    if (assignment) {
      const [, name, rhs] = assignment;
      // Assignment replaces the previous value; don't let stale taint survive
      // a later safe reassignment or sanitizer call, but propagate it through aliases.
      tainted.delete(name);
      if (isTaintedExpr(rhs)) tainted.add(name);
    }
    byLine.push(new Set(tainted));
  }
  return { byLine };
}

function contextFor(rule, line, taintedNames, lines = [], lineIndex = -1) {
  const sourceMatch = USER_INPUT.test(line) || [...taintedNames].some((name) => {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return new RegExp(`(^|[^\\w$])${escaped}(?=$|[^\\w$])`).test(line);
  });

  if (rule.id === 'mob-android-exported' && lineIndex >= 0 && lines.length) {
    const windowText = lines.slice(lineIndex, Math.min(lines.length, lineIndex + 10)).join('\n');
    if (/android\.intent\.action\.MAIN/.test(windowText) && /android\.intent\.category\.LAUNCHER/.test(windowText)) {
      return { report: false };
    }
  }

  if (['js-innerhtml', 'js-outerhtml', 'js-document-write', 'js-insert-adjacent-html'].includes(rule.id)) {
    const rhs = line.split(/(?:innerHTML|outerHTML)\s*=|document\.write\s*\(|insertAdjacentHTML\s*\(/i).slice(1).join(' ');
    const staticMarkup = /^\s*["'][^"']*["']\s*\)?\s*;?\s*$/.test(rhs);
    if (staticMarkup && !/<script\b|\bon\w+\s*=/i.test(rhs)) return { report: false };
    return { report: true, confidence: sourceMatch ? 'high' : 'medium', reason: sourceMatch ? 'Untrusted input reaches an HTML sink.' : 'Dynamic content reaches an HTML sink; source is not proven.' };
  }

  if (rule.id === 'js-react-dangerous-html') {
    if (!sourceMatch && /__html\s*:\s*["'][^"']*["']/.test(line)) return { report: false };
    return { report: true, confidence: sourceMatch ? 'high' : 'medium', reason: sourceMatch ? 'Untrusted input is rendered as HTML.' : 'HTML rendering bypasses React escaping; inspect the value.' };
  }

  if (rule.id === 'js-sql-concat' || rule.id === 'py-sql-fstring' || rule.id === 'py-sql-percent') {
    return { report: true, confidence: sourceMatch || INPUT_NAME.test(line) ? 'high' : 'medium', reason: sourceMatch ? 'User-controlled value appears in SQL construction.' : 'Dynamic SQL construction detected; verify parameter binding.' };
  }

  return { report: true, confidence: rule.severity === 'critical' || rule.severity === 'high' ? 'medium' : 'low', reason: 'Pattern match; validate the code context.' };
}

function analyzeHtmlStructure(raw, file, lines, catalog, suppressions) {
  const findings = [];
  const seenIds = new Map();
  const idRule = {
    id: 'html-duplicate-id',
    title: 'Duplicate HTML id attribute',
    severity: 'medium',
    cwe: 'CWE-1084',
    hint: 'Use unique id attributes; duplicate ids break DOM lookups and form labels.',
  };

  for (let i = 0; i < lines.length; i++) {
    const lineNum = i + 1;
    const matches = lines[i].matchAll(/\bid\s*=\s*["']([^"'\s]+)["']/gi);
    for (const m of matches) {
      const idVal = m[1];
      if (seenIds.has(idVal)) {
        if (!suppressed(suppressions, lineNum, idRule.id)) {
          findings.push(makeFinding(idRule, file, lineNum, lines[i], {
            confidence: 'high',
            reason: `Duplicate id="${idVal}" (first defined on line ${seenIds.get(idVal)}).`,
          }));
        }
      } else {
        seenIds.set(idVal, lineNum);
      }
    }
  }

  const scriptRx = /<script(?![^>]*\bsrc\b)[^>]*>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = scriptRx.exec(raw)) !== null) {
    const body = match[1];
    if (!body || !body.trim()) continue;
    const tagEndOffset = match.index + match[0].indexOf('>') + 1;
    const lineOffset = raw.slice(0, tagEndOffset).split('\n').length - 1;
    const scriptCodeLines = stripComments(body, 'js', true).split(/\r?\n/);
    const taint = evaluateTaint(scriptCodeLines, 'js');
    findings.push(...analyzeJavaScriptAst(body, file, catalog, taint, suppressions, makeFinding, suppressed, { lineOffset }));
  }
  return findings;
}

function analyzeCssStructure(raw, file, lines, suppressions) {
  const findings = [];
  let braceDepth = 0;
  let inComment = false;
  let hadUnterminatedString = false;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const lineNum = i + 1;
    let quote = '';
    for (let j = 0; j < line.length; j++) {
      const ch = line[j];
      const next = line[j + 1];
      if (inComment) {
        if (ch === '*' && next === '/') { inComment = false; j++; }
        continue;
      }
      if (!quote && ch === '/' && next === '*') { inComment = true; j++; continue; }
      if (!quote && (ch === '"' || ch === "'")) { quote = ch; continue; }
      if (quote) {
        if (ch === '\\') { j++; continue; }
        if (ch === quote) quote = '';
        continue;
      }
      if (ch === '{') braceDepth++;
      else if (ch === '}') braceDepth--;
    }
    if (quote) {
      hadUnterminatedString = true;
      if (!suppressed(suppressions, lineNum, 'css-unterminated-string')) {
        findings.push(makeFinding({
          id: 'css-unterminated-string',
          title: 'Unterminated CSS string literal',
          severity: 'medium',
          cwe: 'CWE-1077',
          hint: 'Close the string quote so subsequent CSS rules are not swallowed.',
        }, file, lineNum, line, {
          confidence: 'high',
          reason: `Unclosed ${quote} string literal on this line.`,
        }));
      }
    }
  }
  if (!hadUnterminatedString && braceDepth !== 0) {
    let reportIdx = lines.length - 1;
    while (reportIdx > 0 && !lines[reportIdx].trim()) reportIdx--;
    const reportLine = reportIdx + 1;
    if (!suppressed(suppressions, reportLine, 'css-unbalanced-braces')) {
      findings.push(makeFinding({
        id: 'css-unbalanced-braces',
        title: 'Unbalanced CSS braces',
        severity: 'medium',
        cwe: 'CWE-1077',
        hint: 'Check for a missing or extra "{" or "}" brace.',
      }, file, reportLine, lines[reportIdx] || '', {
        confidence: 'high',
        reason: braceDepth > 0 ? `${braceDepth} unclosed "{" brace(s) in stylesheet.` : `${Math.abs(braceDepth)} extra "}" brace(s) in stylesheet.`,
      }));
    }
  }
  return findings;
}

function rawMatchHasCode(rule, rawText, codeText, matchIndex, matchLength) {
  const matchEnd = matchIndex + matchLength;
  for (let i = matchIndex; i < matchEnd; i++) {
    if (codeText[i] && !/\s/.test(codeText[i])) return true;
  }
  if (['js-todo', 'js-fixme', 'js-hack', 'js-commented-code', 'py-todo'].includes(rule.id)) return true;
  if (rule.id === 'js-http-url' || rule.id === 'py-http-url' || rule.id === 'js-ssrf-metadata') return /\b(?:fetch|requests?\.(?:get|post|put|delete)|axios|request|https?\.|url\s*\(|src\s*=|href\s*=)/i.test(codeText);
  if (rule.id === 'js-hardcoded-ip') return /\b(?:host|hostname|ip|address|endpoint)\w*\s*[:=]/i.test(codeText);
  if (rule.id === 'secret-basic-auth-url') return /\b(?:fetch|axios|request|https?\.)/i.test(codeText);
  if (rule.id.startsWith('secret-')) {
    if (/\b(?:sample|fixture|example|test)[\w$]*\s*[:=]/i.test(codeText)) return false;
    return /\b(?:[\w$]*(?:key|token|secret|password|passwd|auth|credential)|stripe|aws|github|slack|openai|anthropic|npm|hook|url|database|connection)\w*\s*[:=]/i.test(codeText);
  }
  if (rule.id === 'js-sql-concat') return /\b(?:query|execute|prepare)\s*\(/i.test(codeText);
  return false;
}

async function detectTestCommand(root) {
  try {
    const raw = await fs.readFile(path.join(root, 'package.json'), 'utf8');
    const pkg = JSON.parse(raw);
    return pkg.scripts && pkg.scripts.test ? 'npm test' : null;
  } catch {
    return null;
  }
}

async function discoverTestPlan(root) {
  const packageRaw = await fs.readFile(path.join(root, 'package.json'), 'utf8').catch(() => '');
  let packageData = null;
  if (packageRaw) {
    try { packageData = JSON.parse(packageRaw); } catch (error) { packageData = error ? null : null; }
  }
  const script = String(packageData?.scripts?.test || '');
  const nodeRunner = /\bnode(?:\.exe)?\s+.*--test\b/i.test(script) ? 'node' : null;
  const npmRunner = /\b(?:vitest|jest|mocha|tap|uvu)\b/i.test(script) ? 'npm' : null;
  const hasPytest = Boolean(await fs.stat(path.join(root, 'pyproject.toml')).catch(() => null));
  const hasGo = Boolean(await fs.stat(path.join(root, 'go.mod')).catch(() => null));
  const hasCargo = Boolean(await fs.stat(path.join(root, 'Cargo.toml')).catch(() => null));
  const ignorePatterns = await readIgnorePatterns(root);
  const queue = [root];
  const testFiles = [];
  let visitedDirectories = 0;

  while (queue.length && testFiles.length < 201 && visitedDirectories < MAX_FILES) {
    const directory = queue.pop();
    visitedDirectories++;
    let entries;
    try { entries = await fs.readdir(directory, { withFileTypes: true }); } catch { continue; }
    for (const entry of entries) {
      const full = path.join(directory, entry.name);
      const relative = path.relative(root, full).replace(/\\/g, '/');
      if (entry.isDirectory()) {
        if (!ignoredDirs.has(entry.name.toLowerCase()) && !ignorePatterns.some((rx) => rx.test(relative))) queue.push(full);
        continue;
      }
      if (!entry.isFile() || ignorePatterns.some((rx) => rx.test(relative))) continue;
      const nodeTest = /(?:^|\/)(?:__tests__\/.+|.+\.(?:test|spec)\.[cm]?[jt]sx?)$/i.test(relative);
      const pythonTest = /(?:^|\/)(?:test_[\w.-]+|[\w.-]+_test)\.py$/i.test(relative);
      const goTest = hasGo && /_test\.go$/i.test(relative);
      const cargoTest = hasCargo && /^tests\/[^/]+\.rs$/i.test(relative);
      if (nodeTest && nodeRunner) testFiles.push({ file: relative, runner: nodeRunner, command: `node --test "${relative}"` });
      else if (nodeTest && npmRunner) testFiles.push({ file: relative, runner: npmRunner, command: `npm test -- "${relative}"` });
      else if (pythonTest && hasPytest) testFiles.push({ file: relative, runner: 'pytest', command: `python -m pytest "${relative}"` });
      else if (goTest) {
        const source = await fs.readFile(full, 'utf8').catch(() => '');
        const code = stripComments(source, 'generic');
        const names = [...code.matchAll(/^\s*func\s+((?:Test|Example|Fuzz)[A-Za-z0-9_]*)\s*\(/gm)].map((match) => match[1]);
        if (names.length) testFiles.push({ file: relative, runner: 'go', names, command: `go test "${path.posix.dirname(relative)}" [${names.join(', ')}]` });
      } else if (cargoTest) {
        const target = path.posix.basename(relative, '.rs');
        testFiles.push({ file: relative, runner: 'cargo', command: `cargo test --test "${target}"` });
      }
      if (testFiles.length >= 201) break;
    }
  }
  testFiles.sort((a, b) => a.file.localeCompare(b.file));
  const testCommand = script ? 'npm test'
    : hasPytest ? 'python -m pytest'
      : hasCargo ? 'cargo test'
        : hasGo ? 'go test ./...'
          : null;
  return {
    testCommand,
    testFiles: testFiles.slice(0, 200),
    testFilesTruncated: testFiles.length > 200 || queue.length > 0,
  };
}

function scanFileContent(raw, rel, extName, lang, catalog, applicableByLang) {
  const findings = [];
  let applicable = applicableByLang?.get(lang);
  if (!applicable) {
    applicable = catalog.filter((rule) => appliesTo(rule, lang));
    applicableByLang?.set(lang, applicable);
  }
  const lines = raw.split(/\r?\n/);
  const needsMasking = ['js', 'py', 'css', 'sh', 'sql', 'generic'].includes(lang);
  const commentText = needsMasking ? stripComments(raw, lang) : raw;
  const codeText = needsMasking ? maskCommentsThenStrings(commentText, lang, []) : raw;
  const commentLines = commentText.split(/\r?\n/);
  const codeLines = codeText.split(/\r?\n/);
  const suppressions = suppressionMap(lines);
  const taint = (lang === 'js' || lang === 'py') ? evaluateTaint(codeLines, lang) : { byLine: [] };
  const safeHtmlLines = new Set();

  if (lang === 'js') {
    findings.push(...analyzeJavaScriptAst(raw, rel, applicable, taint, suppressions, makeFinding, suppressed, { safeHtmlLines }));
  } else if (lang === 'html' && (extName === '.html' || extName === '.htm')) {
    findings.push(...analyzeHtmlStructure(raw, rel, lines, catalog, suppressions));
  } else if (lang === 'css') {
    findings.push(...analyzeCssStructure(raw, rel, lines, suppressions));
  }

  // CommonJS only: ESM syntax and .mjs/.ts files are strict by definition.
  if (['.js', '.cjs'].includes(extName) && !/^\s*(?:import|export)\s/m.test(raw) && !/(['"])use strict\1/.test(raw)) {
    const rule = { id: 'js-missing-strict', title: 'Missing strict mode', severity: 'low', cwe: 'CWE-710', hint: 'Add "use strict" so accidental globals throw.' };
    if (!suppressed(suppressions, 1, rule.id)) findings.push(makeFinding(rule, rel, 1, lines[0]));
  }

  for (const rule of applicable) {
    if (rule.id === 'secret-generic-credential' && TEST_FILE_PATH.test(rel)) continue;
    if (rule.absent && rule.absent.test(raw)) continue;
    const isCodeView = rule.view === 'code';
    const isRawCode = rule.view === 'raw' || rawCodeRuleIds.has(rule.id);
    const searchLines = isCodeView
      ? (rawCodeRuleIds.has(rule.id) ? commentLines : codeLines)
      : rule.lang === 'html' ? lines : commentLines;
    const foundLines = new Set();

    if (rule.multi) {
      const rawTextLines = lines.map((line, index) =>
        isRawCode && !codeLines[index].trim() ? line.replace(/[^\n]/g, ' ') : line,
      );
      const text = (isRawCode ? rawTextLines : searchLines).join('\n');
      const match = rule.pattern.exec(text);
      if (!match) continue;
      if (isRawCode && !rawMatchHasCode(rule, text, codeText, match.index, match[0].length)) continue;
      const line = text.slice(0, match.index).split('\n').length;
      if (suppressed(suppressions, line, rule.id)) continue;
      if (rule.secretGroup) {
        const value = match[rule.secretGroup];
        const normalized = value && value.replace(/^(?:sk|rk)_(?:live|test)_/, '');
        if (!value || looksLikePlaceholder(value) || entropy(normalized) < MIN_SECRET_ENTROPY) continue;
      }
      const key = `${rel}:${line}`;
      if (foundLines.has(key)) continue;
      foundLines.add(key);
      findings.push(makeFinding(rule, rel, line, lines[line - 1]));
      continue;
    }

    const useRawLines = rule.view === 'raw' || ['js-todo', 'js-fixme', 'js-hack', 'js-commented-code', 'py-todo'].includes(rule.id);
    const rawSearchLines = useRawLines ? lines : searchLines;
    const wholeText = useRawLines ? raw : (isCodeView ? (rawCodeRuleIds.has(rule.id) ? commentText : codeText) : (rule.lang === 'html' ? raw : commentText));

    // Fast pre-filter: skip rules whose unanchored pattern does not appear anywhere in the file.
    if (!/[\^$]/.test(rule.pattern.source) && !rule.pattern.flags.includes('y')) {
      rule.pattern.lastIndex = 0;
      if (!rule.pattern.test(wholeText)) continue;
      rule.pattern.lastIndex = 0;
    }

    for (let i = 0; i < rawSearchLines.length; i++) {
      const line = rawSearchLines[i];
      if (rule.exclude && rule.exclude.test(line)) continue;
      rule.pattern.lastIndex = 0;
      if (!rule.pattern.test(line)) continue;
      if (isRawCode) {
        rule.pattern.lastIndex = 0;
        const match = rule.pattern.exec(line);
        rule.pattern.lastIndex = 0;
        if (!match || !rawMatchHasCode(rule, line, codeLines[i] || '', match.index, match[0].length)) continue;
      }
      if (!secretAllowed(rule, line)) continue;
      if (suppressed(suppressions, i + 1, rule.id)) continue;
      if (['js-innerhtml', 'js-outerhtml'].includes(rule.id) && safeHtmlLines.has(i + 1)) continue;
      if (rule.id === 'js-promise-no-catch' && new RegExp('\\.then\\s*\\(').test(line)) {
        if (/^\s*(?:const|let|var|return)\b/.test(line)) continue;
        const lookahead = rawSearchLines.slice(i + 1, i + 6).join('\n');
        if (new RegExp('\\.catch\\b').test(lookahead)) continue;
      }
      const context = contextFor(rule, lines[i], taint.byLine[i] || new Set(), lines, i);
      if (!context.report) continue;
      const key = `${rel}:${i + 1}`;
      if (foundLines.has(key)) continue;
      if (findings.some((existing) => existing.ruleId === rule.id && existing.file === rel && existing.line === i + 1)) continue;
      foundLines.add(key);
      findings.push(makeFinding(rule, rel, i + 1, lines[i], context));
    }
  }

  if (languageByExtension[extName]) {
    for (const finding of analyzePolyglot(raw, rel, extName, suppressions)) {
      const equivalents = polyglotLegacyRuleIds[finding.ruleId];
      if (equivalents && findings.some((existing) =>
        existing.file === rel && existing.line === finding.line && equivalents.has(existing.ruleId))) continue;
      findings.push(finding);
    }
  }
  return { findings, rulesEvaluated: applicable.length };
}

async function scanProjectRoot(root, kind, options = {}) {
  if (!kinds.includes(kind)) throw new Error(`Unknown category "${kind}"`);
  const scanRoot = path.resolve(root);
  const catalog = rulesForKind(kind);
  const ignorePatterns = await readIgnorePatterns(scanRoot);
  const candidates = [];
  const queue = [scanRoot];
  const startedAt = Date.now();

  while (queue.length && candidates.length < MAX_FILES) {
    const dir = queue.pop();
    let entries;
    try { entries = await fs.readdir(dir, { withFileTypes: true }); } catch { continue; }
    entries.sort((a, b) => a.name.localeCompare(b.name));
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      const rel = path.relative(scanRoot, full);
      const ignorePath = rel.replace(/\\/g, '/');
      if (entry.isDirectory()) {
        const name = entry.name.toLowerCase();
        if (ignoredDirs.has(name) || ignorePatterns.some((rx) => rx.test(ignorePath))) continue;
        queue.push(full);
        continue;
      }
      if (!entry.isFile() || ignoredFiles.has(entry.name.toLowerCase())) continue;
      if (ignorePatterns.some((rx) => rx.test(ignorePath))) continue;
      const extName = path.extname(entry.name).toLowerCase();
      if (!sourceExt.has(extName) && !isExtensionlessSource(entry.name)) continue;
      candidates.push({ full, rel, extName });
      if (candidates.length >= MAX_FILES) break;
    }
  }

  const findings = [];
  const languagesScanned = new Set();
  const applicableByLang = new Map();
  let filesScanned = 0;
  let rulesEvaluated = 0;
  let truncated = false;
  const CONCURRENCY = 16;

  for (let start = 0; start < candidates.length && !truncated; start += CONCURRENCY) {
    const batch = candidates.slice(start, start + CONCURRENCY);
    const batchResults = await Promise.all(batch.map(async ({ full, rel, extName }) => {
      let stat;
      try { stat = await fs.stat(full); } catch { return null; }
      if (stat.size > MAX_FILE_BYTES) return null;
      let raw;
      try { raw = await fs.readFile(full, 'utf8'); } catch { return null; }
      if (raw.includes('\u0000')) return null;
      const lang = langByExt[extName] || 'generic';
      return { lang, ...scanFileContent(raw, rel, extName, lang, catalog, applicableByLang) };
    }));

    for (const item of batchResults) {
      if (!item) continue;
      filesScanned++;
      languagesScanned.add(item.lang);
      rulesEvaluated += item.rulesEvaluated;
      for (const finding of item.findings) {
        findings.push(finding);
        if (findings.length >= MAX_FINDINGS) { truncated = true; break; }
      }
      if (truncated) break;
    }
  }

  findings.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line || String(a.ruleId).localeCompare(String(b.ruleId)));

  const testPlan = await discoverTestPlan(scanRoot);
  const testCommand = testPlan.testCommand || await detectTestCommand(scanRoot);

  const uniqueFindings = dedupeFindings(findings);
  const selected = options.maxFindings ? uniqueFindings.slice(0, options.maxFindings) : uniqueFindings;
  const capped = selected.slice(0, MAX_FINDINGS);
  const severityCounts = { critical: 0, high: 0, medium: 0, low: 0, info: 0 };
  for (const finding of capped) {
    if (severityCounts[finding.severity] !== undefined) severityCounts[finding.severity]++;
  }
  return {
    id: `scan-${Date.now()}`,
    projectId: null,
    completedAt: new Date().toISOString(),
    durationMs: Date.now() - startedAt,
    filesScanned,
    rulesEvaluated,
    rulesAvailable: catalog.length,
    languagesScanned: [...languagesScanned].sort(),
    severityCounts,
    findings: capped,
    truncated: truncated || selected.length > MAX_FINDINGS,
    testCommand,
    testFiles: testPlan.testFiles,
    testFilesTruncated: testPlan.testFilesTruncated,
    score: scoreFor(selected),
  };
}

function toSarifReport(scan, project = {}) {
  const levelMap = { critical: 'error', high: 'error', medium: 'warning', low: 'note', info: 'note' };
  const rulesMap = new Map();
  for (const finding of scan.findings || []) {
    const id = finding.ruleId || 'deepcheck-rule';
    if (!rulesMap.has(id)) {
      rulesMap.set(id, {
        id,
        name: finding.title,
        shortDescription: { text: finding.title },
        help: { text: finding.hint || finding.reason || finding.title },
        properties: {
          tags: ['security', ...(finding.cwe ? [finding.cwe] : [])],
          'security-severity': finding.severity === 'critical' ? '9.0' : finding.severity === 'high' ? '7.5' : finding.severity === 'medium' ? '5.0' : '2.0',
        },
      });
    }
  }
  return {
    $schema: 'https://json.schemastore.org/sarif-2.1.0.json',
    version: '2.1.0',
    runs: [
      {
        tool: {
          driver: {
            name: 'DeepCheck',
            informationUri: 'https://github.com/deepcheck/deepcheck',
            version: '1.0.0',
            rules: [...rulesMap.values()],
          },
        },
        invocations: [
          {
            executionSuccessful: true,
            endTimeUtc: scan.completedAt || new Date().toISOString(),
          },
        ],
        results: (scan.findings || []).map((finding) => ({
          ruleId: finding.ruleId || 'deepcheck-rule',
          level: levelMap[finding.severity] || 'warning',
          message: {
            text: `${finding.title}${finding.cwe ? ` (${finding.cwe})` : ''}: ${finding.hint || finding.reason || ''}`.trim(),
          },
          locations: [
            {
              physicalLocation: {
                artifactLocation: {
                  uri: String(finding.file || '').replace(/\\/g, '/'),
                  uriBaseId: '%SRCROOT%',
                },
                region: {
                  startLine: Math.max(1, Number(finding.line) || 1),
                  snippet: { text: finding.excerpt || '' },
                },
              },
            },
          ],
          properties: {
            severity: finding.severity,
            confidence: finding.confidence || 'medium',
            cwe: finding.cwe || null,
            projectName: project.name || null,
            aiAssessment: finding.aiAssessment || null,
          },
        })),
      },
    ],
  };
}

function toMarkdownReport(scan, project = {}) {
  const lines = [
    `# DeepCheck Audit Report${project.name ? ` — ${project.name}` : ''}`,
    '',
    `- **Completed:** ${scan.completedAt || new Date().toISOString()}`,
    ...(project.root ? [`- **Path:** \`${project.root}\``] : []),
    ...(project.kind ? [`- **Category:** ${project.kind}`] : []),
    `- **Score:** ${scan.score}/100`,
    `- **Files scanned:** ${scan.filesScanned}`,
    `- **Rule evaluations:** ${(scan.rulesEvaluated || 0).toLocaleString()}`,
    `- **Findings:** ${(scan.findings || []).length}${scan.truncated ? ' (truncated at limit)' : ''}`,
    '',
  ];
  if (!scan.findings?.length) {
    lines.push('No findings reported.');
    return lines.join('\n');
  }
  lines.push('## Findings', '');
  for (const finding of scan.findings) {
    lines.push(`### [${finding.severity.toUpperCase()}] ${finding.title}${finding.cwe ? ` (${finding.cwe})` : ''}`);
    lines.push(`- **Location:** \`${finding.file}:${finding.line}\``);
    lines.push(`- **Scanner confidence:** ${finding.confidence || 'medium'}`);
    if (finding.reason) lines.push(`- **Why:** ${finding.reason}`);
    if (finding.hint) lines.push(`- **Remediation:** ${finding.hint}`);
    if (finding.aiAssessment) {
      lines.push(`- **Local AI review:** \`${finding.aiAssessment.verdict}\` (${finding.aiAssessment.confidence}%) — ${finding.aiAssessment.rationale}`);
    }
    if (finding.excerpt) {
      lines.push('', '```', finding.excerpt, '```');
    }
    lines.push('');
  }
  return lines.join('\n');
}

module.exports = {
  scanProjectRoot,
  discoverTestPlan,
  scoreFor,
  dedupeFindings,
  suppressionMap,
  suppressed,
  rulesForKind,
  stripComments,
  entropy,
  globToRegExp,
  toSarifReport,
  toMarkdownReport,
  kinds,
  shared,
  categoryRules,
  MAX_FINDINGS,
  MAX_FILES,
  MAX_FILE_BYTES,
};
