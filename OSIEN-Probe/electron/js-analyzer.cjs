'use strict';

const { parse } = require('@babel/parser');

const USER_INPUT = /(?:req\.(?:body|query|params|headers|cookies)|request\.(?:body|query|params|headers|cookies)|searchParams\.|URLSearchParams|location\.(?:search|hash)|event\.target\.value|e\.target\.value|process\.argv|stdin|userInput|user_input|untrusted|payload|message)/i;
const SANITIZER = /\b(?:DOMPurify\.sanitize|sanitizeHtml|escapeHtml|escapeRegExp|encodeURIComponent|encodeURI|Number|parseInt|parseFloat|Boolean|validator\.escape|path\.basename|pick|omit|html\.escape|shlex\.quote|ast\.literal_eval)\s*\(/;
const SENSITIVE_NAME = /(?:password|passwd|pwd|secret|token|api[_-]?key|private[_-]?key|jwt|credential|auth[_-]?header|session[_-]?id)/i;
const SECRET_FN_NAME = /(?:token|secret|password|salt|nonce|key|session|otp|auth|csrf|uuid)/i;
const FILE_SINKS = new Set(['readFile', 'readFileSync', 'createReadStream', 'sendFile', 'download', 'writeFile', 'writeFileSync', 'unlink', 'unlinkSync', 'open', 'openSync', 'copyFile', 'rename']);
const KNOWN_GLOBALS = new Set([
  'window', 'document', 'console', 'process', 'require', 'module', 'exports', 'global', 'globalThis',
  'Buffer', 'URL', 'URLSearchParams', 'FormData', 'fetch', 'setTimeout', 'setInterval', 'clearTimeout',
  'clearInterval', 'setImmediate', 'clearImmediate', 'queueMicrotask', 'Promise', 'Error', 'TypeError',
  'RangeError', 'SyntaxError', 'ReferenceError', 'JSON', 'Math', 'Object', 'Array', 'String', 'Number',
  'Boolean', 'Date', 'RegExp', 'Map', 'Set', 'WeakMap', 'WeakSet', 'Symbol', 'BigInt', 'Int8Array',
  'Uint8Array', 'parseInt', 'parseFloat', 'isNaN', 'isFinite', 'encodeURIComponent', 'decodeURIComponent',
  'encodeURI', 'decodeURI', 'atob', 'btoa', 'navigator', 'location', 'history', 'localStorage',
  'sessionStorage', 'alert', 'confirm', 'prompt', 'performance', 'crypto', 'AbortController',
  'AbortSignal', 'Headers', 'Request', 'Response', 'Event', 'CustomEvent', 'HTMLElement', 'Node',
  'Element', 'describe', 'it', 'test', 'expect', 'beforeEach', 'afterEach', 'beforeAll', 'afterAll',
  'vi', 'jest', 'arguments', 'undefined', 'NaN', 'Infinity', 'eval', 'Function', 'Reflect', 'Proxy',
  'Intl', 'WebAssembly', 'this', 'super', '__dirname', '__filename',
  'getComputedStyle', 'requestAnimationFrame', 'cancelAnimationFrame', 'requestIdleCallback',
  'cancelIdleCallback', 'matchMedia', 'IntersectionObserver', 'ResizeObserver', 'MutationObserver',
  'XMLHttpRequest', 'FileReader', 'Blob', 'File', 'TextEncoder', 'TextDecoder', 'structuredClone',
  'EventTarget', 'NodeList', 'DOMParser', 'getSelection', 'scrollTo', 'scrollBy',
]);

function staticText(node) {
  return node?.type === 'StringLiteral'
    || node?.type === 'NumericLiteral'
    || (node?.type === 'TemplateLiteral' && node.expressions.length === 0);
}

function memberName(node) {
  if (node?.type !== 'MemberExpression' && node?.type !== 'OptionalMemberExpression') return '';
  if (!node.computed && node.property?.type === 'Identifier') return node.property.name;
  if (node.computed && node.property?.type === 'StringLiteral') return node.property.value;
  return '';
}

function identifierName(node) {
  if (!node) return '';
  if (node.type === 'Identifier') return node.name;
  if (node.type === 'MemberExpression' || node.type === 'OptionalMemberExpression') return memberName(node);
  return '';
}

function walkSubtree(node, visitor) {
  if (!node || typeof node !== 'object') return;
  if (visitor(node) === false) return;
  for (const [key, value] of Object.entries(node)) {
    if (key === 'loc' || key === 'start' || key === 'end' || key === 'extra' || key === 'errors' || key === 'comments' || key === 'tokens') continue;
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item && typeof item === 'object' && typeof item.type === 'string') walkSubtree(item, visitor);
      }
    } else if (value && typeof value === 'object' && typeof value.type === 'string') {
      walkSubtree(value, visitor);
    }
  }
}

function collectIdentifierNames(node, out = new Set()) {
  walkSubtree(node, (sub) => {
    if (sub.type === 'Identifier') out.add(sub.name);
  });
  return out;
}

function collectAssignedTargets(node, out = new Set()) {
  walkSubtree(node, (sub) => {
    if (sub.type === 'AssignmentExpression' && sub.left?.type === 'Identifier') out.add(sub.left.name);
    if (sub.type === 'UpdateExpression' && sub.argument?.type === 'Identifier') out.add(sub.argument.name);
  });
  return out;
}

function isSimpleBody(block) {
  return Array.isArray(block?.body) ? block.body : block ? [block] : [];
}

function collectPatternBindings(pattern, out) {
  if (!pattern) return;
  if (pattern.type === 'Identifier') { out.add(pattern.name); return; }
  if (pattern.type === 'AssignmentPattern') { collectPatternBindings(pattern.left, out); return; }
  if (pattern.type === 'RestElement') { collectPatternBindings(pattern.argument, out); return; }
  if (pattern.type === 'ArrayPattern') { for (const el of pattern.elements) collectPatternBindings(el, out); return; }
  if (pattern.type === 'ObjectPattern') {
    for (const prop of pattern.properties) {
      if (prop.type === 'RestElement') collectPatternBindings(prop.argument, out);
      else if (prop.type === 'ObjectProperty') collectPatternBindings(prop.value, out);
    }
  }
}

function collectScopeBindings(statements, out) {
  for (const stmt of statements || []) {
    if (!stmt) continue;
    if (stmt.type === 'VariableDeclaration') {
      for (const decl of stmt.declarations) collectPatternBindings(decl.id, out);
    } else if (stmt.type === 'FunctionDeclaration' || stmt.type === 'ClassDeclaration') {
      if (stmt.id?.name) out.add(stmt.id.name);
    } else if (stmt.type === 'ImportDeclaration') {
      for (const spec of stmt.specifiers || []) {
        if (spec.local?.name) out.add(spec.local.name);
      }
    } else if (stmt.type === 'ExportNamedDeclaration' || stmt.type === 'ExportDefaultDeclaration') {
      collectScopeBindings([stmt.declaration], out);
    }
  }
}

function checkBlockStatements(statements, emit) {
  let terminated = false;
  for (const stmt of statements) {
    if (terminated && stmt.type !== 'FunctionDeclaration' && stmt.type !== 'EmptyStatement') {
      emit({
        ruleId: 'js-unreachable-code',
        title: 'Unreachable code',
        severity: 'low',
        cwe: 'CWE-561',
        hint: 'Remove dead statements after return, throw, break, or continue.',
      }, stmt, 'high', 'Statement follows an unconditional control-flow exit in the same block.');
      terminated = false;
    }
    if (['ReturnStatement', 'ThrowStatement', 'BreakStatement', 'ContinueStatement'].includes(stmt.type)) {
      terminated = true;
    }
  }
}

const VALID_TYPEOF = new Set(['undefined', 'object', 'boolean', 'number', 'bigint', 'string', 'symbol', 'function']);
const NUMERIC_MEMBER = new Set(['length', 'size', 'count', 'index', 'byteLength']);

function isNumericExpr(node, numericVars) {
  if (!node) return false;
  if (node.type === 'NumericLiteral') return true;
  if (node.type === 'Identifier') return numericVars.has(node.name);
  if (node.type === 'MemberExpression' && NUMERIC_MEMBER.has(memberName(node))) return true;
  if (node.type === 'UnaryExpression' && ['+', '-', '~'].includes(node.operator)) return isNumericExpr(node.argument, numericVars);
  if (node.type === 'BinaryExpression' && ['+', '-', '*', '/', '%', '**'].includes(node.operator)) {
    return isNumericExpr(node.left, numericVars) && isNumericExpr(node.right, numericVars);
  }
  if (node.type === 'CallExpression') {
    const name = node.callee?.type === 'Identifier' ? node.callee.name : memberName(node.callee);
    if (['Number', 'parseInt', 'parseFloat'].includes(name)) return true;
    if (node.callee?.object?.name === 'Math') return true;
  }
  return false;
}

function isSafeHtmlValue(node, numericVars, safeHtmlVars) {
  if (!node) return false;
  if (staticText(node) || isNumericExpr(node, numericVars)) return true;
  if (node.type === 'Identifier') return safeHtmlVars.has(node.name);
  if (node.type === 'TemplateLiteral') {
    return node.expressions.every((expr) => isSafeHtmlValue(expr, numericVars, safeHtmlVars));
  }
  if (node.type === 'BinaryExpression' && node.operator === '+') {
    return isSafeHtmlValue(node.left, numericVars, safeHtmlVars) && isSafeHtmlValue(node.right, numericVars, safeHtmlVars);
  }
  if (node.type === 'ConditionalExpression') {
    return isSafeHtmlValue(node.consequent, numericVars, safeHtmlVars) && isSafeHtmlValue(node.alternate, numericVars, safeHtmlVars);
  }
  return false;
}

function analyzeJavaScriptAst(raw, file, catalog, taint, suppressions, makeFinding, suppressed, options = {}) {
  const lineOffset = options.lineOffset || 0;
  let ast;
  try {
    ast = parse(raw, {
      sourceType: 'unambiguous',
      errorRecovery: true,
      plugins: ['jsx', 'typescript'],
    });
  } catch {
    return [];
  }

  const rules = new Map(catalog.map((rule) => [rule.id, rule]));
  const lines = raw.split(/\r?\n/);
  const findings = [];
  const emitted = new Set();

  const emit = (ruleOrId, nodeOrLine, confidence = 'medium', reason) => {
    const rule = typeof ruleOrId === 'string' ? rules.get(ruleOrId) : ruleOrId;
    const localLine = typeof nodeOrLine === 'number' ? nodeOrLine : nodeOrLine?.loc?.start.line;
    if (!rule || !localLine || !lines[localLine - 1]) return;
    const reportLine = localLine + lineOffset;
    if (suppressed(suppressions, reportLine, rule.id) || emitted.has(`${rule.id}:${reportLine}`)) return;
    emitted.add(`${rule.id}:${reportLine}`);
    findings.push(makeFinding(rule, file, reportLine, lines[localLine - 1], {
      confidence,
      reason: reason || 'Syntax-aware match; validate the surrounding code.',
    }));
  };

  if (Array.isArray(ast.errors)) {
    for (const err of ast.errors.slice(0, 10)) {
      const localLine = err.loc?.line;
      if (localLine) {
        emit({
          id: 'js-syntax-error',
          title: 'JavaScript/TypeScript syntax error',
          severity: 'medium',
          cwe: 'CWE-1077',
          hint: 'Fix the syntax error reported by the parser.',
        }, localLine, 'high', err.message ? err.message.split(' (')[0] : 'Parser reported a syntax error.');
      }
    }
  }

  const isDynamicSql = (node) => {
    if (!node) return false;
    const sqlClause = /(?:\bSELECT\b[\s\S]{1,80}\bFROM\b|\bINSERT\b[\s\S]{1,40}\bINTO\b|\bUPDATE\b[\s\S]{1,40}\bSET\b|\bDELETE\b[\s\S]{1,40}\bFROM\b)/i;
    if (node.type === 'TemplateLiteral') return node.expressions.length > 0 && sqlClause.test(raw.slice(node.start, node.end));
    if (node.type === 'BinaryExpression' && node.operator === '+') {
      const source = raw.slice(node.start, node.end);
      return sqlClause.test(source) && (node.left.type !== 'StringLiteral' || node.right.type !== 'StringLiteral');
    }
    return false;
  };

  const containsInput = (node) => {
    if (!node) return false;
    const source = raw.slice(node.start, node.end);
    if (SANITIZER.test(source)) return false;
    if (USER_INPUT.test(source)) return true;
    const names = taint.byLine[(node.loc?.start.line || 1) - 1] || new Set();
    return [...names].some((name) => {
      const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      return new RegExp(`(^|[^\\w$])${escaped}(?=$|[^\\w$])`).test(source);
    });
  };

  const fnStack = [];
  const scopeStack = [];
  const topScope = new Set();
  collectScopeBindings(ast.program?.body, topScope);
  scopeStack.push(topScope);
  const declaredGlobals = new Set(topScope);
  const isDeclared = (name) => KNOWN_GLOBALS.has(name) || scopeStack.some((s) => s.has(name));

  // Pre-collect numeric variables, safe-HTML accumulators, and async function names.
  const numericVars = new Set();
  const safeHtmlVars = new Set();
  const asyncFnNames = new Set();
  const unsafeHtmlVars = new Set();
  walkSubtree(ast, (n) => {
    if ((n.type === 'FunctionDeclaration' || n.type === 'VariableDeclarator') && n.async && n.id?.name) {
      asyncFnNames.add(n.id.name);
    }
    if (n.type === 'FunctionDeclaration' && n.async && n.id?.name) asyncFnNames.add(n.id.name);
    if (n.type === 'ForStatement' && n.init?.type === 'VariableDeclaration') {
      for (const d of n.init.declarations) {
        if (d.id?.type === 'Identifier' && isNumericExpr(d.init, numericVars)) numericVars.add(d.id.name);
      }
    }
    if (n.type === 'VariableDeclarator' && n.id?.type === 'Identifier' && n.init) {
      if (isNumericExpr(n.init, numericVars)) numericVars.add(n.id.name);
      else if (staticText(n.init)) safeHtmlVars.add(n.id.name);
    }
    if (n.type === 'BinaryExpression' && ['<=', '<', '>', '>=', '!==', '===', '==', '!='].includes(n.operator)) {
      if (n.left?.type === 'Identifier' && isNumericExpr(n.right, numericVars)) numericVars.add(n.left.name);
      if (n.right?.type === 'Identifier' && isNumericExpr(n.left, numericVars)) numericVars.add(n.right.name);
    }
  });
  walkSubtree(ast, (n) => {
    if (n.type === 'AssignmentExpression' && n.left?.type === 'Identifier') {
      const name = n.left.name;
      if (n.operator === '+=' && safeHtmlVars.has(name)) {
        if (!isSafeHtmlValue(n.right, numericVars, safeHtmlVars)) unsafeHtmlVars.add(name);
      } else if (n.operator === '=') {
        if (!isSafeHtmlValue(n.right, numericVars, safeHtmlVars)) unsafeHtmlVars.add(name);
      }
    }
  });
  for (const name of unsafeHtmlVars) safeHtmlVars.delete(name);

  const visit = (node, parent, parentKey) => {
    if (!node || typeof node !== 'object') return;
    const line = node.loc?.start.line;

    if (node.type === 'Program' || node.type === 'BlockStatement') {
      checkBlockStatements(node.body || [], (meta, stmt, conf, reason) => {
        emit({ id: meta.ruleId, ...meta }, stmt, conf, reason);
      });
    }

    let pushedBlockScope = false;
    if (node.type === 'BlockStatement') {
      const blockScope = new Set();
      collectScopeBindings(node.body, blockScope);
      scopeStack.push(blockScope);
      pushedBlockScope = true;
    } else if (node.type === 'ForStatement' || node.type === 'ForInStatement' || node.type === 'ForOfStatement') {
      const loopScope = new Set();
      const decl = node.init || node.left;
      if (decl?.type === 'VariableDeclaration') {
        for (const d of decl.declarations) collectPatternBindings(d.id, loopScope);
      }
      scopeStack.push(loopScope);
      pushedBlockScope = true;
    } else if (node.type === 'CatchClause' && node.param) {
      const catchScope = new Set();
      collectPatternBindings(node.param, catchScope);
      scopeStack.push(catchScope);
      pushedBlockScope = true;
    }

    const isFn = node.type === 'FunctionDeclaration'
      || node.type === 'FunctionExpression'
      || node.type === 'ArrowFunctionExpression'
      || node.type === 'ObjectMethod'
      || node.type === 'ClassMethod';
    let fnName = '';
    if (isFn) {
      fnName = node.id?.name || '';
      const fnScope = new Set();
      if (fnName) fnScope.add(fnName);
      for (const p of node.params || []) collectPatternBindings(p, fnScope);
      if (node.body?.type === 'BlockStatement') collectScopeBindings(node.body.body, fnScope);
      scopeStack.push(fnScope);
      fnStack.push({ name: fnName, async: Boolean(node.async), params: new Set(node.params.filter((p) => p.type === 'Identifier').map((p) => p.name)) });

      // Detect fetch() response consumed without checking res.ok or res.status.
      if (node.async && node.body?.type === 'BlockStatement') {
        const bodySrc = raw.slice(node.body.start, node.body.end);
        if (/\bfetch\s*\(/.test(bodySrc) && /\.\s*(?:json|text|arrayBuffer|blob)\s*\(/.test(bodySrc) && !/\.(?:ok|status)\b/.test(bodySrc)) {
          let fetchNode = null;
          walkSubtree(node.body, (sub) => {
            if (!fetchNode && sub.type === 'CallExpression' && sub.callee?.type === 'Identifier' && sub.callee.name === 'fetch') fetchNode = sub;
          });
          if (fetchNode) {
            emit({
              id: 'js-fetch-no-ok-check',
              title: 'fetch() response parsed without status check',
              severity: 'low',
              cwe: 'CWE-252',
              hint: 'Check "if (!res.ok) throw new Error(...)" before calling res.json().',
            }, fetchNode, 'medium', 'fetch() rejects only on network failure; HTTP 4xx/5xx responses are not checked via res.ok.');
          }
        }
      }

      // Detect unconditional self-recursion (missing base-case guard).
      if (fnName && node.body?.type === 'BlockStatement') {
        let hasBranch = false;
        let selfCall = null;
        walkSubtree(node.body, (sub) => {
          if (sub !== node && (sub.type === 'FunctionDeclaration' || sub.type === 'FunctionExpression' || sub.type === 'ArrowFunctionExpression')) return false;
          if (['IfStatement', 'ConditionalExpression', 'SwitchStatement', 'LogicalExpression', 'ForStatement', 'WhileStatement', 'DoWhileStatement', 'TryStatement'].includes(sub.type)) {
            hasBranch = true;
          }
          if (!selfCall && sub.type === 'CallExpression' && sub.callee?.type === 'Identifier' && sub.callee.name === fnName) {
            selfCall = sub;
          }
        });
        if (selfCall && !hasBranch) {
          emit({
            id: 'js-unbounded-recursion',
            title: `Unconditional recursion in "${fnName}()"`,
            severity: 'high',
            cwe: 'CWE-674',
            hint: 'Add a base-case condition before recursing to avoid a RangeError stack overflow.',
          }, selfCall, 'high', `Function "${fnName}" calls itself without any conditional base-case guard.`);
        }
      }

      // Detect async read-modify-write race condition on outer/shared variables.
      if (node.async && node.body?.type === 'BlockStatement') {
        const stmts = node.body.body || [];
        let firstAwaitIdx = -1;
        for (let idx = 0; idx < stmts.length; idx++) {
          let hasAwait = false;
          walkSubtree(stmts[idx], (sub) => { if (sub.type === 'AwaitExpression') hasAwait = true; });
          if (hasAwait) { firstAwaitIdx = idx; break; }
        }
        if (firstAwaitIdx >= 0) {
          const readBeforeAwait = new Map();
          for (let idx = 0; idx < firstAwaitIdx; idx++) {
            const stmt = stmts[idx];
            if (stmt.type === 'VariableDeclaration') {
              for (const decl of stmt.declarations) {
                if (decl.id?.type === 'Identifier' && decl.init?.type === 'Identifier' && declaredGlobals.has(decl.init.name)) {
                  readBeforeAwait.set(decl.id.name, decl.init.name);
                }
              }
            }
          }
          for (let idx = firstAwaitIdx + 1; idx < stmts.length; idx++) {
            walkSubtree(stmts[idx], (sub) => {
              if (sub.type === 'AssignmentExpression' && sub.left?.type === 'Identifier' && declaredGlobals.has(sub.left.name)) {
                const rhsNames = collectIdentifierNames(sub.right);
                for (const [alias, shared] of readBeforeAwait.entries()) {
                  if (shared === sub.left.name && rhsNames.has(alias)) {
                    emit({
                      id: 'js-async-race-condition',
                      title: 'Async read-modify-write race condition',
                      severity: 'medium',
                      cwe: 'CWE-362',
                      hint: 'Re-read shared state after await or serialize concurrent writes.',
                    }, sub, 'high', `Shared variable "${shared}" is read before await and written afterwards using stale "${alias}".`);
                  }
                }
              }
            });
          }
        }
      }

      // Detect inverted parity check in isEven / isOdd functions.
      if (/^(?:isEven|isOdd)$/i.test(fnName)) {
        const expectOdd = /isOdd$/i.test(fnName);
        walkSubtree(node.body, (sub) => {
          if (sub.type === 'ReturnStatement' && sub.argument?.type === 'BinaryExpression') {
            const { operator, left, right } = sub.argument;
            if (['==', '===', '!=', '!=='].includes(operator) && left.type === 'BinaryExpression' && left.operator === '%' && left.right?.value === 2 && right?.type === 'NumericLiteral') {
              const checksOne = right.value === 1;
              const isNegated = operator.startsWith('!');
              const effectiveOdd = isNegated ? !checksOne : checksOne;
              if (effectiveOdd !== expectOdd) {
                emit({
                  id: 'js-suspicious-parity',
                  title: `Inverted parity check in ${fnName}`,
                  severity: 'medium',
                  cwe: 'CWE-670',
                  hint: `${fnName} checks the opposite remainder; verify "% 2 === ${expectOdd ? 1 : 0}".`,
                }, sub, 'high', `Function "${fnName}" returns ${effectiveOdd ? 'true for odd' : 'true for even'} numbers.`);
              }
            }
          }
        });
      }
    }

    // Off-by-one loop condition (<= .length) and splice-during-iteration.
    if (node.type === 'ForStatement') {
      if (node.test?.type === 'BinaryExpression' && node.test.operator === '<=' && memberName(node.test.right) === 'length') {
        const targetObj = raw.slice(node.test.right.object.start, node.test.right.object.end);
        emit({
          id: 'js-off-by-one-loop',
          title: 'Off-by-one loop bound (<= .length)',
          severity: 'medium',
          cwe: 'CWE-193',
          hint: 'Use "< .length" instead of "<= .length" to avoid reading past the end.',
        }, node.test, 'high', `Loop condition "<= ${targetObj}.length" iterates one element past the valid index range.`);
      }
      if (node.update?.type === 'UpdateExpression' && node.update.operator === '++' && node.update.argument?.type === 'Identifier') {
        const indexVar = node.update.argument.name;
        const bodyStmts = isSimpleBody(node.body);
        const decrementsIndex = collectAssignedTargets(node.body).has(indexVar);
        if (!decrementsIndex) {
          for (const stmt of bodyStmts) {
            walkSubtree(stmt, (sub) => {
              if (sub.type === 'CallExpression' && memberName(sub.callee) === 'splice' && sub.arguments[0]?.type === 'Identifier' && sub.arguments[0].name === indexVar) {
                emit({
                  id: 'js-array-splice-in-loop',
                  title: 'Array mutated with splice during forward loop',
                  severity: 'medium',
                  cwe: 'CWE-670',
                  hint: 'Iterate backwards, decrement the index after splice, or use Array.prototype.filter.',
                }, sub, 'high', `Splicing at "${indexVar}" while incrementing "${indexVar}++" skips the next element.`);
              }
            });
          }
        }
      }
    }

    // Prototype pollution via unguarded for..in merge.
    if (node.type === 'ForInStatement' && node.left) {
      const keyName = node.left.type === 'VariableDeclaration'
        ? node.left.declarations[0]?.id?.name
        : node.left.type === 'Identifier' ? node.left.name : '';
      if (keyName) {
        const bodyText = raw.slice(node.body.start, node.body.end);
        const hasGuard = new RegExp(`hasOwnProperty|Object\\.hasOwn|__proto${'__'}|constructor|prototype`).test(bodyText);
        if (!hasGuard) {
          let writesComputed = null;
          walkSubtree(node.body, (sub) => {
            if (sub.type === 'AssignmentExpression' && sub.left?.type === 'MemberExpression' && sub.left.computed && sub.left.property?.type === 'Identifier' && sub.left.property.name === keyName) {
              writesComputed = sub;
            }
          });
          if (writesComputed) {
            emit('js-prototype-pollution', writesComputed, 'high', `Unguarded "for..in" assigns dynamic key "${keyName}" without checking Object.hasOwn or __proto__.`);
          }
        }
      }
    }

    // Duplicate keys in object literal.
    if (node.type === 'ObjectExpression' && Array.isArray(node.properties)) {
      const seenKeys = new Set();
      for (const prop of node.properties) {
        if (prop.type !== 'ObjectProperty' || prop.computed) continue;
        const k = prop.key?.type === 'Identifier' ? prop.key.name : prop.key?.type === 'StringLiteral' ? prop.key.value : '';
        if (!k) continue;
        if (seenKeys.has(k)) {
          emit({
            id: 'js-duplicate-key',
            title: 'Duplicate object property key',
            severity: 'medium',
            cwe: 'CWE-670',
            hint: 'Remove or rename the duplicate property so the earlier value is not silently overwritten.',
          }, prop, 'high', `Property "${k}" is defined more than once in the same object literal.`);
        }
        seenKeys.add(k);
      }
    }

    // Switch statement: duplicate case values & unintentional fallthrough.
    if (node.type === 'SwitchStatement' && Array.isArray(node.cases)) {
      const seenCases = new Set();
      for (let cIdx = 0; cIdx < node.cases.length; cIdx++) {
        const sc = node.cases[cIdx];
        if (sc.test && staticText(sc.test)) {
          const val = `${sc.test.type}:${sc.test.value ?? raw.slice(sc.test.start, sc.test.end)}`;
          if (seenCases.has(val)) {
            emit({
              id: 'js-duplicate-case',
              title: 'Duplicate switch case',
              severity: 'medium',
              cwe: 'CWE-670',
              hint: 'Remove or merge the unreachable duplicate case branch.',
            }, sc, 'high', 'This switch case matches a value already handled by an earlier case.');
          }
          seenCases.add(val);
        }
        if (sc.consequent?.length && cIdx < node.cases.length - 1) {
          let lastStmt = sc.consequent[sc.consequent.length - 1];
          if (lastStmt.type === 'BlockStatement' && lastStmt.body.length) {
            lastStmt = lastStmt.body[lastStmt.body.length - 1];
          }
          const exits = ['BreakStatement', 'ReturnStatement', 'ThrowStatement', 'ContinueStatement'].includes(lastStmt.type);
          const caseText = raw.slice(sc.start, sc.end);
          if (!exits && !/fall\s*through/i.test(caseText)) {
            emit({
              id: 'js-switch-fallthrough',
              title: 'Switch case falls through without break',
              severity: 'low',
              cwe: 'CWE-484',
              hint: 'Add break, return, or a // fallthrough comment if intentional.',
            }, sc, 'medium', 'Non-empty switch case falls through into the next case.');
          }
        }
      }
    }

    // Unsafe return or throw inside finally block.
    if (node.type === 'TryStatement' && node.finalizer) {
      walkSubtree(node.finalizer, (sub) => {
        if (sub.type === 'FunctionDeclaration' || sub.type === 'FunctionExpression' || sub.type === 'ArrowFunctionExpression') return false;
        if (sub.type === 'ReturnStatement' || sub.type === 'ThrowStatement') {
          emit({
            id: 'js-return-in-finally',
            title: `${sub.type === 'ReturnStatement' ? 'return' : 'throw'} inside finally block`,
            severity: 'high',
            cwe: 'CWE-584',
            hint: 'Avoid return/throw in finally; it silently overrides try/catch return values and exceptions.',
          }, sub, 'high', 'Control-flow exit in finally discards exceptions thrown in try/catch.');
        }
      });
    }

    // new Promise(async (...) => ...) swallows async rejections.
    if (node.type === 'NewExpression' && node.callee?.type === 'Identifier' && node.callee.name === 'Promise' && node.arguments[0]?.async) {
      emit({
        id: 'js-async-promise-executor',
        title: 'Async Promise executor function',
        severity: 'high',
        cwe: 'CWE-391',
        hint: 'Use an async function directly instead of wrapping it in new Promise(async ...).',
      }, node, 'high', 'Errors thrown inside an async Promise executor are not caught by the Promise.');
    }

    // Operator precedence bug: !x in obj or !x instanceof Class.
    if (node.type === 'BinaryExpression' && (node.operator === 'in' || node.operator === 'instanceof')
      && node.left?.type === 'UnaryExpression' && node.left.operator === '!' && !node.left.extra?.parenthesized) {
      emit({
        id: 'js-negated-in-instanceof',
        title: `Negation precedence bug with "${node.operator}"`,
        severity: 'high',
        cwe: 'CWE-783',
        hint: `Wrap the expression in parentheses: !(x ${node.operator} y).`,
      }, node, 'high', `"!x ${node.operator} y" is parsed as "(!x) ${node.operator} y", not "!(x ${node.operator} y)".`);
    }

    // Promise returned by known async function used directly in if/while condition without await.
    if ((node.type === 'IfStatement' || node.type === 'WhileStatement') && node.test?.type === 'CallExpression'
      && node.test.callee?.type === 'Identifier' && asyncFnNames.has(node.test.callee.name)) {
      emit({
        id: 'js-promise-in-condition',
        title: `Async function "${node.test.callee.name}()" tested without await`,
        severity: 'high',
        cwe: 'CWE-480',
        hint: `Add "await" before ${node.test.callee.name}(); a Promise object is always truthy.`,
      }, node.test, 'high', `Calling async "${node.test.callee.name}()" without await returns a Promise, which is always truthy.`);
    }

    // Throwing a string/number literal instead of an Error object loses stack trace.
    if (node.type === 'ThrowStatement' && (node.argument?.type === 'StringLiteral' || node.argument?.type === 'NumericLiteral' || node.argument?.type === 'TemplateLiteral')) {
      emit({
        id: 'js-throw-literal',
        title: 'Throwing a primitive literal instead of Error',
        severity: 'low',
        cwe: 'CWE-397',
        hint: 'Throw new Error(...) so callers receive a stack trace and Error instance.',
      }, node, 'high', 'Throwing a non-Error literal omits the stack trace.');
    }

    // Stateful /g RegExp literal tested inside a loop.
    if ((node.type === 'WhileStatement' || node.type === 'ForStatement') && node.test) {
      walkSubtree(node.test, (sub) => {
        if (sub.type === 'CallExpression' && memberName(sub.callee) === 'test' && sub.callee?.object?.type === 'RegExpLiteral' && sub.callee.object.flags?.includes('g')) {
          emit({
            id: 'js-global-regex-in-loop',
            title: 'Stateful /g RegExp.test() in loop condition',
            severity: 'medium',
            cwe: 'CWE-670',
            hint: 'Remove the /g flag from .test() or reset lastIndex; /g mutates lastIndex on every call.',
          }, sub, 'high', 'RegExp with /g flag advances lastIndex on each .test() call, causing alternating misses.');
        }
      });
    }

    // Constant condition in if statement & duplicate else-if conditions.
    if (node.type === 'IfStatement') {
      if (node.test?.type === 'BooleanLiteral' || node.test?.type === 'NumericLiteral' || node.test?.type === 'StringLiteral') {
        emit({
          id: 'js-constant-condition',
          title: 'Constant condition in if statement',
          severity: 'medium',
          cwe: 'CWE-570',
          hint: 'Replace the constant literal condition with a real runtime check or remove dead branch.',
        }, node.test, 'high', 'if() condition is a constant literal that never changes at runtime.');
      }
      if (parent?.type !== 'IfStatement') {
        const seenBranches = new Set([raw.slice(node.test.start, node.test.end).trim()]);
        let cur = node.alternate;
        while (cur?.type === 'IfStatement') {
          const condSrc = raw.slice(cur.test.start, cur.test.end).trim();
          if (seenBranches.has(condSrc)) {
            emit({
              id: 'js-duplicate-else-if',
              title: 'Duplicate condition in else-if chain',
              severity: 'medium',
              cwe: 'CWE-561',
              hint: 'Remove or fix the unreachable duplicate else-if condition.',
            }, cur.test, 'high', `Condition "${condSrc}" was already checked in an earlier branch.`);
          }
          seenBranches.add(condSrc);
          cur = cur.alternate;
        }
      }
    }

    // Sparse array holes ([1, , 3]) and delete on a bare variable.
    if (node.type === 'ArrayExpression' && node.elements?.some((el) => el === null)) {
      emit({
        id: 'js-sparse-array-hole',
        title: 'Sparse array literal with empty slot',
        severity: 'low',
        cwe: 'CWE-670',
        hint: 'Remove the extra comma or use explicit undefined if intentional.',
      }, node, 'high', 'Double comma in array literal creates an empty slot (undefined hole).');
    }
    if (node.type === 'UnaryExpression' && node.operator === 'delete' && node.argument?.type === 'Identifier') {
      emit({
        id: 'js-delete-var',
        title: 'delete applied to a variable identifier',
        severity: 'medium',
        cwe: 'CWE-670',
        hint: 'delete only removes object properties; it throws a SyntaxError in strict mode on variables.',
      }, node, 'high', `delete ${node.argument.name} is invalid in strict mode and ineffective on local bindings.`);
    }

    // ReDoS-prone nested quantifier regex literal.
    if (node.type === 'RegExpLiteral' && /\([^)]*[+*][^)]*\)[+*]/.test(node.pattern)) {
      emit({
        id: 'js-redos-regex',
        title: 'Nested quantifier in RegExp (ReDoS risk)',
        severity: 'medium',
        cwe: 'CWE-1333',
        hint: 'Avoid nested quantifiers like (a+)+ that can cause exponential backtracking.',
      }, node, 'high', `RegExp /${node.pattern}/ contains nested quantifiers.`);
    }

    // Missing await on known async function call used as a standalone statement,
    // or discarded return value of pure/immutable string/array methods.
    if (node.type === 'ExpressionStatement' && node.expression?.type === 'CallExpression') {
      const callee = node.expression.callee;
      if (callee?.type === 'Identifier') {
        const called = callee.name;
        const activeFn = fnStack[fnStack.length - 1];
        if (activeFn?.async && asyncFnNames.has(called)) {
          emit({
            id: 'js-missing-await',
            title: `Missing await on async call "${called}()"`,
            severity: 'medium',
            cwe: 'CWE-252',
            hint: `Await ${called}() or handle its returned Promise so errors and ordering are not lost.`,
          }, node, 'high', `Async function "${called}" is called inside an async function without await or .catch().`);
        }
      } else {
        const method = memberName(callee);
        if (['trim', 'trimStart', 'trimEnd', 'toLowerCase', 'toUpperCase', 'slice', 'concat', 'filter'].includes(method)
          && callee?.object?.type === 'Identifier'
          && !['res', 'response', 'app', 'router', 'fs', 'path', 'console'].includes(callee.object.name)) {
          emit({
            id: 'js-ignored-immutable-return',
            title: `Return value of .${method}() is ignored`,
            severity: 'medium',
            cwe: 'CWE-252',
            hint: `.${method}() does not mutate in place; assign or return its result.`,
          }, node, 'high', `Calling "${callee.object.name}.${method}()" without using its return value is a no-op.`);
        }
      }
    }

    // Assignment inside if/while condition without extra parentheses.
    if ((node.type === 'IfStatement' || node.type === 'WhileStatement') && node.test?.type === 'AssignmentExpression' && !node.test.extra?.parenthesized) {
      emit({
        id: 'js-assign-in-condition',
        title: 'Assignment inside conditional expression',
        severity: 'medium',
        cwe: 'CWE-481',
        hint: 'Use === for comparison, or wrap intentional assignments in parentheses.',
      }, node.test, 'high', 'Condition uses "=" assignment instead of "==" or "===".');
    }

    // Self-comparison, NaN comparison, invalid typeof, and hardcoded credential comparison.
    if (node.type === 'BinaryExpression' && ['==', '===', '!=', '!=='].includes(node.operator)) {
      const leftSrc = raw.slice(node.left.start, node.left.end);
      const rightSrc = raw.slice(node.right.start, node.right.end);
      if (leftSrc && leftSrc === rightSrc && (node.left.type === 'Identifier' || node.left.type === 'MemberExpression')) {
        emit({
          id: 'js-self-compare',
          title: 'Value compared to itself',
          severity: 'medium',
          cwe: 'CWE-480',
          hint: 'Comparing an expression to itself is always true/false (except NaN); use Number.isNaN.',
        }, node, 'high', `"${leftSrc}" is compared with itself.`);
      }
      if ((node.left.type === 'Identifier' && node.left.name === 'NaN') || (node.right.type === 'Identifier' && node.right.name === 'NaN')) {
        emit({
          id: 'js-nan-compare',
          title: 'Direct comparison with NaN',
          severity: 'high',
          cwe: 'CWE-480',
          hint: 'Use Number.isNaN(value); NaN === NaN is always false.',
        }, node, 'high', 'Comparing against NaN with == or === always evaluates to false (or true for !==).');
      }
      const typeofSide = node.left.type === 'UnaryExpression' && node.left.operator === 'typeof'
        ? node.right
        : node.right.type === 'UnaryExpression' && node.right.operator === 'typeof' ? node.left : null;
      if (typeofSide?.type === 'StringLiteral' && !VALID_TYPEOF.has(typeofSide.value)) {
        emit({
          id: 'js-invalid-typeof',
          title: `Invalid typeof comparison "${typeofSide.value}"`,
          severity: 'high',
          cwe: 'CWE-480',
          hint: 'typeof only returns undefined, object, boolean, number, bigint, string, symbol, or function.',
        }, node, 'high', `typeof can never equal "${typeofSide.value}".`);
      }
      const leftName = identifierName(node.left);
      const rightName = identifierName(node.right);
      if ((SENSITIVE_NAME.test(leftName) && node.right.type === 'StringLiteral' && node.right.value.length > 0)
        || (SENSITIVE_NAME.test(rightName) && node.left.type === 'StringLiteral' && node.left.value.length > 0)) {
        emit({
          id: 'js-hardcoded-auth',
          title: 'Hardcoded credential comparison',
          severity: 'high',
          cwe: 'CWE-798',
          hint: 'Do not compare passwords or secrets against hardcoded literals; verify against hashed/configured credentials.',
        }, node, 'high', 'Credential variable is compared directly to a hardcoded string literal.');
      }
    }

    if (node.type === 'CallExpression' || node.type === 'OptionalCallExpression' || node.type === 'NewExpression') {
      const name = node.callee?.type === 'Identifier' ? node.callee.name : memberName(node.callee);
      if (name === 'eval') emit('js-eval', node);
      if (node.type === 'NewExpression' && name === 'Function') emit('js-new-function', node);
      if (node.type === 'NewExpression' && name === 'RegExp' && node.arguments[0] && containsInput(node.arguments[0])) {
        emit('js-regex-injection', node, 'high', 'Tracked untrusted input is compiled into a regular expression.');
      }
      if (name === 'assign' && node.callee?.object?.name === 'Object' && node.arguments.slice(1).some(containsInput)) {
        emit('js-object-assign-proto', node, 'high', 'Tracked untrusted input is merged directly into an object.');
      }
      if (name === 'write' && node.callee?.object?.name === 'document' && (node.arguments.some((arg) => !staticText(arg)) || node.arguments.some((arg) => arg.type === 'StringLiteral' && /<script|on\w+\s*=/i.test(arg.value)))) {
        emit('js-document-write', node, 'high', 'Executable HTML or dynamic content is passed to document.write.');
      }
      if (name === 'insertAdjacentHTML' && node.arguments[1] && !staticText(node.arguments[1])) {
        emit('js-insert-adjacent-html', node, 'medium', 'Dynamic content is passed to insertAdjacentHTML.');
      }
      if (['query', 'execute', 'raw'].includes(name) && node.arguments.some(isDynamicSql)) {
        emit('js-sql-concat', node, 'medium', 'SQL text includes dynamically constructed content; verify parameter binding.');
      }
      if (['exec', 'execSync'].includes(name) && node.arguments.some((arg) => (arg.type === 'TemplateLiteral' && arg.expressions.length) || (arg.type === 'BinaryExpression' && arg.operator === '+'))) {
        emit('js-exec-interpolation', node, 'medium', 'A shell command is constructed dynamically; use an argument-array API.');
      }
      if (FILE_SINKS.has(name) && node.arguments.some(containsInput)) {
        emit('js-path-traversal', node, 'high', 'Request-controlled data reaches a filesystem path.');
      }
      if (name === 'redirect' && (node.arguments.some(containsInput) || (node.arguments[0] && !staticText(node.arguments[0])))) {
        const hasInput = containsInput(node.arguments[0]);
        emit({
          id: 'js-redirect-param',
          title: hasInput ? 'Open redirect from query parameter' : 'Unvalidated redirect target',
          severity: 'medium',
          cwe: 'CWE-601',
          hint: 'Allow only same-site relative paths or an explicit origin allow-list.',
        }, node, hasInput ? 'high' : 'medium', hasInput ? 'Request-controlled data reaches a redirect call.' : 'Dynamic destination reaches a redirect call; validate against an allow-list.');
      }
      const networkRequest = name === 'fetch' || (['get', 'post', 'request'].includes(name) && /(?:axios|https?|http)/i.test(raw.slice(node.callee.start, node.callee.end)));
      if (networkRequest && containsInput(node.arguments[0])) {
        emit('js-ssrf', node, 'high', 'Request-controlled data reaches an outbound URL.');
      }
      if (name === 'startsWith' && node.arguments[0]?.type === 'StringLiteral' && /^https?:\/\/[^/]+$/i.test(node.arguments[0].value)) {
        emit({
          id: 'js-bypassable-url-check',
          title: 'Bypassable URL prefix validation',
          severity: 'medium',
          cwe: 'CWE-601',
          hint: 'Parse with new URL() and compare exact hostname instead of using startsWith.',
        }, node, 'high', `startsWith("${node.arguments[0].value}") can be bypassed by "${node.arguments[0].value}.evil.com".`);
      }
      if (name === 'random' && node.callee?.object?.name === 'Math') {
        const activeFn = fnStack[fnStack.length - 1];
        if (activeFn && SECRET_FN_NAME.test(activeFn.name)) {
          emit('js-math-random-secret', node, 'high', `Math.random() is used inside security-sensitive function "${activeFn.name}".`);
        }
      }
      if (['log', 'debug', 'info', 'warn', 'error'].includes(name) && node.callee?.object?.name === 'console') {
        const leaked = node.arguments.find((arg) => SENSITIVE_NAME.test(identifierName(arg)));
        if (leaked) {
          emit({
            id: 'js-log-sensitive',
            title: 'Sensitive credential logged to console',
            severity: 'medium',
            cwe: 'CWE-532',
            hint: 'Never log passwords, tokens, or secrets to console output.',
          }, node, 'high', `Sensitive identifier "${identifierName(leaked)}" is passed to console.${name}().`);
        }
      }
      if (node.type === 'CallExpression' && parent?.type === 'ExpressionStatement' && node.callee?.property?.name === 'then' && (node.callee?.type === 'MemberExpression' || node.callee?.type === 'OptionalMemberExpression') && node.arguments.length < 2) {
        const after = raw.slice(node.end || 0, (node.end || 0) + 24).trimStart();
        if (!new RegExp('^\\.\\s*(?:catch|finally)\\b').test(after)) {
          emit('js-promise-no-catch', node, 'medium', 'Promise chain has no rejection handler; attach .catch or provide an onRejected callback.');
        }
      }
      if (['map', 'filter', 'find', 'some', 'every', 'reduce'].includes(name) && node.arguments[0]
        && (node.arguments[0].type === 'ArrowFunctionExpression' || node.arguments[0].type === 'FunctionExpression')
        && node.arguments[0].body?.type === 'BlockStatement' && node.arguments[0].body.body.length > 0) {
        let hasReturn = false;
        walkSubtree(node.arguments[0].body, (sub) => {
          if (sub !== node.arguments[0] && (sub.type === 'FunctionDeclaration' || sub.type === 'FunctionExpression' || sub.type === 'ArrowFunctionExpression')) return false;
          if (sub.type === 'ReturnStatement' || sub.type === 'ThrowStatement') hasReturn = true;
        });
        if (!hasReturn) {
          emit({
            id: 'js-missing-array-return',
            title: `Missing return in Array.${name}() callback`,
            severity: 'high',
            cwe: 'CWE-670',
            hint: `Return a value from the .${name}() callback (or use concise arrow syntax without {}).`,
          }, node.arguments[0], 'high', `.${name}() callback uses a block body {} with no return statement, returning undefined.`);
        }
      }
      if (name === 'catch' && !(parent?.type === 'MemberExpression' && parentKey === 'object')
        && node.arguments[0] && (node.arguments[0].type === 'ArrowFunctionExpression' || node.arguments[0].type === 'FunctionExpression')) {
        const cb = node.arguments[0];
        if (cb.body?.type === 'BlockStatement' && cb.body.body.length === 0) {
          emit({
            id: 'js-empty-promise-catch',
            title: 'Empty Promise .catch() handler',
            severity: 'medium',
            cwe: 'CWE-390',
            hint: 'Log or handle the rejection instead of swallowing it silently.',
          }, node, 'medium', 'Promise .catch() handler has an empty block and silently discards errors.');
        }
      }
    }

    if (node.type === 'AssignmentExpression') {
      const sink = memberName(node.left);
      if (['innerHTML', 'outerHTML'].includes(sink)) {
        if (isSafeHtmlValue(node.right, numericVars, safeHtmlVars)) {
          if (options.safeHtmlLines && line) options.safeHtmlLines.add(line + lineOffset);
        } else {
          const taintedNames = line ? taint.byLine[line - 1] || new Set() : new Set();
        const rightSrc = raw.slice(node.right.start, node.right.end);
        const sourceVisible = USER_INPUT.test(rightSrc) || [...taintedNames].some((name) => {
          const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
          return new RegExp(`(^|[^\\w$])${escaped}(?=$|[^\\w$])`).test(rightSrc);
        });
          emit(sink === 'innerHTML' ? 'js-innerhtml' : 'js-outerhtml', node, sourceVisible ? 'high' : 'medium', sourceVisible ? 'Tracked untrusted input reaches an HTML sink.' : 'Dynamic content reaches an HTML sink; inspect its source.');
        }
      }
      const locationTarget = (memberName(node.left) === 'href' && memberName(node.left.object) === 'location')
        || memberName(node.left) === 'location';
      if (locationTarget && containsInput(node.right)) {
        emit('js-unsafe-redirect', node, 'medium', 'Request-controlled data reaches a browser redirect destination.');
      }
      if (node.left?.type === 'MemberExpression' && memberName(node.left.object) === 'prototype' && ['Object', 'Array', 'String', 'Function'].includes(node.left.object?.object?.name)) {
        emit('js-prototype-pollution', node, 'high', `Direct mutation of ${node.left.object.object.name}.prototype affects all objects globally.`);
      }
      if (sink === 'src' && !staticText(node.right) && /\bscript\b/i.test(raw)) {
        const objName = node.left.object?.name || '';
        if (objName && new RegExp(`createElement\\s*\\(\\s*["']script["']\\s*\\)`).test(raw)) {
          emit({
            id: 'js-dynamic-script-src',
            title: 'Dynamic script src assignment',
            severity: 'medium',
            cwe: 'CWE-829',
            hint: 'Allow-list script URLs or load static bundled modules with Subresource Integrity.',
          }, node, 'medium', 'A dynamically created <script> element receives a non-literal src.');
        }
      }
    }

    // Undeclared identifier check (skip type nodes, property keys, declarations, and typeof).
    if (
      node.type === 'Identifier'
      && topScope.size > 0
      && parent
      && !(parent.type === 'MemberExpression' && parentKey === 'property' && !parent.computed)
      && !(parent.type === 'OptionalMemberExpression' && parentKey === 'property' && !parent.computed)
      && !(parent.type === 'ObjectProperty' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'ObjectMethod' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'ClassMethod' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'ClassProperty' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'PropertyDefinition' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'MethodDefinition' && parentKey === 'key' && !parent.computed)
      && !(parent.type === 'ExportSpecifier' && parentKey === 'exported')
      && !(parent.type === 'LabeledStatement' && parentKey === 'label')
      && !(parent.type === 'BreakStatement' && parentKey === 'label')
      && !(parent.type === 'ContinueStatement' && parentKey === 'label')
      && !(parent.type === 'VariableDeclarator' && parentKey === 'id')
      && !(parent.type === 'FunctionDeclaration' && parentKey === 'id')
      && !(parent.type === 'FunctionExpression' && parentKey === 'id')
      && !(parent.type === 'ClassDeclaration' && parentKey === 'id')
      && !(parent.type === 'UnaryExpression' && parent.operator === 'typeof')
      && !parent.type.startsWith('TS')
      && !parent.type.startsWith('Import')
      && !isDeclared(node.name)
    ) {
      emit({
        id: 'js-undeclared-var',
        title: 'Potentially undeclared variable',
        severity: 'medium',
        cwe: 'CWE-457',
        hint: `Declare "${node.name}" or import it before use to avoid a ReferenceError.`,
      }, node, 'low', `Identifier "${node.name}" is not declared in this file or standard globals.`);
    }

    for (const [key, value] of Object.entries(node)) {
      if (key === 'loc' || key === 'start' || key === 'end' || key === 'extra' || key === 'errors' || key === 'comments' || key === 'tokens') continue;
      if (Array.isArray(value)) value.forEach((item) => visit(item, node, key));
      else if (value && typeof value === 'object' && typeof value.type === 'string') visit(value, node, key);
    }

    if (isFn) { fnStack.pop(); scopeStack.pop(); }
    if (pushedBlockScope) scopeStack.pop();
  };

  visit(ast, null, '');
  return findings;
}

module.exports = { analyzeJavaScriptAst };
