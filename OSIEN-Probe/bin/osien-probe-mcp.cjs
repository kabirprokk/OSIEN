#!/usr/bin/env node
'use strict';
// deepcheck-disable-file js-console-log

// Zero-dependency MCP (Model Context Protocol) stdio server exposing OSIEN Probe
// static-analysis, source context inspection, and local AI verification tools.

const readline = require('node:readline');
const path = require('node:path');
const pkg = require('../package.json');
const { scanProjectRoot, rulesForKind, toSarifReport, kinds } = require('../electron/scanner.cjs');
const { getSourceContext, verifyFinding, checkAiHealth } = require('../electron/ai-review.cjs');

const TOOLS = [
  {
    name: 'scan_project',
    description: 'Run OSIEN Probe offline static analysis and multi-language AST bug checks on a local directory. Executes no project code.',
    inputSchema: {
      type: 'object',
      properties: {
        path: { type: 'string', description: 'Absolute or relative path to the project directory to scan.' },
        category: { type: 'string', enum: kinds, description: 'Project category (defaults to Software).' },
        format: { type: 'string', enum: ['json', 'sarif'], description: 'Output format (defaults to json).' },
        maxFindings: { type: 'integer', minimum: 1, maximum: 1000, description: 'Optional cap on returned findings.' },
      },
      required: ['path'],
    },
  },
  {
    name: 'inspect_finding_context',
    description: 'Read nearby source code lines around a reported finding inside a project root (path-traversal protected).',
    inputSchema: {
      type: 'object',
      properties: {
        root: { type: 'string', description: 'Project root directory.' },
        file: { type: 'string', description: 'Relative file path from the scan finding.' },
        line: { type: 'integer', minimum: 1, description: '1-based line number of the finding.' },
        radius: { type: 'integer', minimum: 1, maximum: 30, description: 'Lines before and after to include (default 6).' },
      },
      required: ['root', 'file', 'line'],
    },
  },
  {
    name: 'verify_finding_with_ai',
    description: 'Ask local Ollama (if running) to conservatively verify whether a single finding is likely_real, likely_false_positive, or uncertain.',
    inputSchema: {
      type: 'object',
      properties: {
        root: { type: 'string', description: 'Project root directory.' },
        file: { type: 'string', description: 'Relative file path.' },
        line: { type: 'integer', minimum: 1, description: 'Line number.' },
        title: { type: 'string', description: 'Finding title.' },
        severity: { type: 'string', description: 'Finding severity.' },
        ruleId: { type: 'string', description: 'Rule identifier.' },
        excerpt: { type: 'string', description: 'Code excerpt.' },
      },
      required: ['root', 'file', 'line', 'title'],
    },
  },
  {
    name: 'list_rules',
    description: 'List OSIEN Probe rule IDs, severities, CWEs, and hints for a project category, plus local AI health status.',
    inputSchema: {
      type: 'object',
      properties: {
        category: { type: 'string', enum: kinds, description: 'Project category (defaults to Software).' },
      },
    },
  },
];

async function handleToolCall(name, args = {}) {
  if (name === 'scan_project') {
    const root = path.resolve(String(args.path || '.'));
    const category = kinds.includes(args.category) ? args.category : 'Software';
    const scan = await scanProjectRoot(root, category, { maxFindings: args.maxFindings });
    const payload = args.format === 'sarif' ? toSarifReport(scan, { name: path.basename(root), root, kind: category }) : scan;
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
  if (name === 'inspect_finding_context') {
    const ctx = await getSourceContext(path.resolve(String(args.root)), String(args.file), Number(args.line), Number(args.radius) || 6);
    return { content: [{ type: 'text', text: JSON.stringify(ctx, null, 2) }] };
  }
  if (name === 'verify_finding_with_ai') {
    const assessment = await verifyFinding(path.resolve(String(args.root)), {
      file: String(args.file),
      line: Number(args.line),
      title: String(args.title),
      severity: args.severity || 'medium',
      ruleId: args.ruleId || '',
      excerpt: args.excerpt || '',
    });
    return { content: [{ type: 'text', text: JSON.stringify(assessment, null, 2) }] };
  }
  if (name === 'list_rules') {
    const category = kinds.includes(args.category) ? args.category : 'Software';
    const rules = rulesForKind(category).map(({ id, title, severity, cwe, lang, hint }) => ({ id, title, severity, cwe, lang, hint }));
    const ai = await checkAiHealth();
    return { content: [{ type: 'text', text: JSON.stringify({ category, count: rules.length, aiHealth: ai, rules }, null, 2) }] };
  }
  throw new Error(`Unknown tool "${name}"`);
}

function send(msg) {
  process.stdout.write(`${JSON.stringify(msg)}\n`);
}

function startServer() {
  const rl = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
  rl.on('line', async (line) => {
    const trimmed = line.trim();
    if (!trimmed) return;
    let req;
    try {
      req = JSON.parse(trimmed);
    } catch {
      send({ jsonrpc: '2.0', id: null, error: { code: -32700, message: 'Parse error' } });
      return;
    }
    const { id, method, params } = req;
    try {
      if (method === 'initialize') {
        send({
          jsonrpc: '2.0',
          id,
          result: {
            protocolVersion: '2024-11-05',
            capabilities: { tools: {} },
            serverInfo: { name: 'osien-probe-mcp', version: pkg.version },
          },
        });
      } else if (method === 'notifications/initialized') {
        // Notification; no response required.
      } else if (method === 'tools/list') {
        send({ jsonrpc: '2.0', id, result: { tools: TOOLS } });
      } else if (method === 'tools/call') {
        const result = await handleToolCall(params?.name, params?.arguments);
        send({ jsonrpc: '2.0', id, result });
      } else if (id !== undefined) {
        send({ jsonrpc: '2.0', id, error: { code: -32601, message: `Method not found: ${method}` } });
      }
    } catch (error) {
      if (id !== undefined) {
        send({ jsonrpc: '2.0', id, error: { code: -32603, message: error?.message || String(error) } });
      }
    }
  });
}

if (require.main === module) startServer();

module.exports = { TOOLS, handleToolCall };
