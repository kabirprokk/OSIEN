'use strict';

const { parse, kind, registerDynamicLanguage } = require('@ast-grep/napi');

function loadParsers() {
  return {
    python: require('@ast-grep/lang-python'),
    go: require('@ast-grep/lang-go'),
    rust: require('@ast-grep/lang-rust'),
    java: require('@ast-grep/lang-java'),
    kotlin: require('@ast-grep/lang-kotlin'),
    cpp: require('@ast-grep/lang-cpp'),
    csharp: require('@ast-grep/lang-csharp'),
    ruby: require('@ast-grep/lang-ruby'),
    php: require('@ast-grep/lang-php'),
    swift: require('@ast-grep/lang-swift'),
    bash: require('@ast-grep/lang-bash'),
  };
}

const languageByExtension = {
  '.py': 'python', '.go': 'go', '.rs': 'rust', '.java': 'java',
  '.kt': 'kotlin', '.kts': 'kotlin', '.c': 'cpp', '.h': 'cpp',
  '.cc': 'cpp', '.cpp': 'cpp', '.hpp': 'cpp', '.cs': 'csharp',
  '.rb': 'ruby', '.php': 'php', '.swift': 'swift',
  '.sh': 'bash', '.bash': 'bash', '.zsh': 'bash',
};

const rules = [
  { lang: 'python', pattern: 'eval($A)', id: 'polyglot-python-eval', title: 'Dynamic Python evaluation', severity: 'high', cwe: 'CWE-95', hint: 'Avoid eval on data; parse it or use a restricted mapping.' },
  { lang: 'python', pattern: 'exec($A)', id: 'polyglot-python-exec', title: 'Dynamic Python execution', severity: 'high', cwe: 'CWE-95', hint: 'Avoid exec on dynamic input; use explicit operations.' },
  { lang: 'python', pattern: 'pickle.loads($A)', id: 'polyglot-python-pickle', title: 'Unsafe Python deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Do not load pickle data from untrusted sources.' },
  { lang: 'python', pattern: 'pickle.load($A)', id: 'polyglot-python-pickle', title: 'Unsafe Python deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Do not load pickle data from untrusted sources.' },
  { lang: 'python', pattern: 'marshal.loads($A)', id: 'polyglot-python-marshal', title: 'Unsafe Python marshal deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Do not unmarshal untrusted bytecode payloads.' },
  { lang: 'python', pattern: 'marshal.load($A)', id: 'polyglot-python-marshal', title: 'Unsafe Python marshal deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Do not unmarshal untrusted bytecode payloads.' },
  { lang: 'python', pattern: 'yaml.load($A)', id: 'polyglot-python-yaml-load', title: 'Unsafe YAML deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Use safe_load for YAML that is not fully trusted.' },
  { lang: 'python', pattern: 'os.system($A)', id: 'polyglot-python-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Use subprocess with an argument list and no shell.' },
  { lang: 'python', pattern: 'subprocess.run($A, shell=True)', id: 'polyglot-python-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Use subprocess with an argument list and no shell.' },
  { lang: 'python', pattern: 'cursor.execute($Q.format($$$))', id: 'polyglot-python-sql-format', title: 'SQL built with .format()', severity: 'high', cwe: 'CWE-89', hint: 'Pass query parameters as a tuple/list to cursor.execute instead of .format().' },
  { lang: 'python', pattern: 'cursor.execute($Q % $A)', id: 'polyglot-python-sql-percent', title: 'SQL built with % formatting', severity: 'high', cwe: 'CWE-89', hint: 'Pass query parameters as a tuple/list to cursor.execute instead of %.' },
  { lang: 'python', pattern: 'requests.get($URL)', id: 'polyglot-python-requests-timeout', title: 'HTTP request without timeout', severity: 'low', cwe: 'CWE-400', hint: 'Pass timeout=... so hung upstreams cannot stall the worker indefinitely.' },
  { lang: 'python', pattern: 'requests.post($URL)', id: 'polyglot-python-requests-timeout', title: 'HTTP request without timeout', severity: 'low', cwe: 'CWE-400', hint: 'Pass timeout=... so hung upstreams cannot stall the worker indefinitely.' },
  { lang: 'python', pattern: 'assert ($A, $B)', id: 'polyglot-python-assert-tuple', title: 'Assert on non-empty tuple (always true)', severity: 'high', cwe: 'CWE-670', hint: 'Remove the parentheses: "assert cond, msg" instead of "assert (cond, msg)".' },
  { lang: 'python', pattern: '$X is "$S"', id: 'polyglot-python-is-literal', title: 'Identity check (is) against string literal', severity: 'medium', cwe: 'CWE-595', hint: 'Use == for value equality; "is" checks object identity and fails unpredictably.' },
  { lang: 'python', pattern: 'except Exception:\n    pass', id: 'polyglot-python-except-pass', title: 'Silent broad exception swallow', severity: 'medium', cwe: 'CWE-390', hint: 'Log or handle the exception instead of swallowing all Exception types.' },
  { lang: 'go', pattern: 'exec.Command("sh", "-c", $CMD)', id: 'polyglot-go-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid invoking a shell with dynamically assembled commands.' },
  { lang: 'go', pattern: 'exec.Command("bash", "-c", $CMD)', id: 'polyglot-go-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid invoking a shell with dynamically assembled commands.' },
  { lang: 'go', pattern: 'template.HTML($A)', id: 'polyglot-go-html', title: 'Unescaped HTML rendering', severity: 'high', cwe: 'CWE-79', hint: 'Do not convert untrusted strings to trusted HTML.' },
  { lang: 'go', pattern: 'InsecureSkipVerify: true', id: 'polyglot-go-tls-insecure', title: 'Go TLS verification disabled', severity: 'high', cwe: 'CWE-295', hint: 'Keep InsecureSkipVerify false outside isolated local tests.' },
  { lang: 'go', pattern: 'md5.New()', id: 'polyglot-go-weak-hash', title: 'Weak cryptographic hash (MD5)', severity: 'medium', cwe: 'CWE-327', hint: 'Use sha256.New() or stronger for integrity/security hashing.' },
  { lang: 'go', pattern: 'sha1.New()', id: 'polyglot-go-weak-hash', title: 'Weak cryptographic hash (SHA-1)', severity: 'medium', cwe: 'CWE-327', hint: 'Use sha256.New() or stronger for integrity/security hashing.' },
  { lang: 'go', pattern: 'http.ListenAndServe($ADDR, $HANDLER)', id: 'polyglot-go-cleartext-http', title: 'Go cleartext HTTP server', severity: 'medium', cwe: 'CWE-319', hint: 'Prefer http.ListenAndServeTLS or terminate TLS at a trusted reverse proxy.' },
  { lang: 'rust', pattern: 'Command::new("sh").arg("-c").arg($CMD)', id: 'polyglot-rust-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid passing dynamic command text through a shell.' },
  { lang: 'rust', pattern: 'Command::new("bash").arg("-c").arg($CMD)', id: 'polyglot-rust-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid passing dynamic command text through a shell.' },
  { lang: 'rust', pattern: 'std::mem::transmute($A)', id: 'polyglot-rust-transmute', title: 'Unsafe Rust memory transmute', severity: 'medium', cwe: 'CWE-704', hint: 'Prefer safe conversions or explicit pointer casts over transmute.' },
  { lang: 'rust', pattern: 'std::mem::uninitialized()', id: 'polyglot-rust-uninit', title: 'Deprecated std::mem::uninitialized()', severity: 'high', cwe: 'CWE-908', hint: 'Use MaybeUninit<T> instead of mem::uninitialized(), which causes instant UB.' },
  { lang: 'rust', pattern: 'std::mem::uninitialized::<$T>()', id: 'polyglot-rust-uninit', title: 'Deprecated std::mem::uninitialized()', severity: 'high', cwe: 'CWE-908', hint: 'Use MaybeUninit<T> instead of mem::uninitialized(), which causes instant UB.' },
  { lang: 'java', pattern: 'new ObjectInputStream($A).readObject()', id: 'polyglot-java-deserialize', title: 'Unsafe Java deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Avoid native object deserialization for untrusted data.' },
  { lang: 'java', pattern: 'Runtime.getRuntime().exec($CMD)', id: 'polyglot-java-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Use a fixed executable and validated argument list.' },
  { lang: 'java', pattern: 'MessageDigest.getInstance("MD5")', id: 'polyglot-java-weak-hash', title: 'Weak cryptographic hash (MD5)', severity: 'medium', cwe: 'CWE-327', hint: 'Use SHA-256 or stronger.' },
  { lang: 'java', pattern: 'MessageDigest.getInstance("SHA-1")', id: 'polyglot-java-weak-hash', title: 'Weak cryptographic hash (SHA-1)', severity: 'medium', cwe: 'CWE-327', hint: 'Use SHA-256 or stronger.' },
  { lang: 'java', pattern: '$STMT.executeQuery($Q + $A)', id: 'polyglot-java-sql-concat', title: 'Java SQL string concatenation', severity: 'high', cwe: 'CWE-89', hint: 'Use PreparedStatement with bound parameters.' },
  { lang: 'java', pattern: 'catch (Exception $E) {}', id: 'polyglot-java-empty-catch', title: 'Empty Java catch block', severity: 'medium', cwe: 'CWE-390', hint: 'Log or rethrow caught exceptions instead of ignoring them.' },
  { lang: 'kotlin', pattern: 'Runtime.getRuntime().exec($CMD)', id: 'polyglot-kotlin-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Use a fixed executable and validated argument list.' },
  { lang: 'kotlin', pattern: 'ObjectInputStream($A).readObject()', id: 'polyglot-kotlin-deserialize', title: 'Unsafe Kotlin deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Avoid ObjectInputStream on untrusted streams.' },
  { lang: 'ruby', pattern: 'Marshal.load($A)', id: 'polyglot-ruby-deserialize', title: 'Unsafe Ruby deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Do not deserialize untrusted data with Marshal.' },
  { lang: 'ruby', pattern: 'YAML.load($A)', id: 'polyglot-ruby-yaml-load', title: 'Unsafe Ruby YAML.load', severity: 'high', cwe: 'CWE-502', hint: 'Use YAML.safe_load for untrusted input.' },
  { lang: 'ruby', pattern: 'eval($A)', id: 'polyglot-ruby-eval', title: 'Dynamic Ruby evaluation', severity: 'high', cwe: 'CWE-95', hint: 'Avoid evaluating dynamically supplied Ruby code.' },
  { lang: 'ruby', pattern: 'system($A)', id: 'polyglot-ruby-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Pass a fixed executable and separate validated arguments.' },
  { lang: 'php', pattern: 'eval', id: 'polyglot-php-eval', title: 'Dynamic PHP evaluation', severity: 'high', cwe: 'CWE-95', hint: 'Avoid evaluating dynamically supplied PHP code.' },
  { lang: 'php', pattern: 'unserialize', id: 'polyglot-php-deserialize', title: 'Unsafe PHP deserialization', severity: 'high', cwe: 'CWE-502', hint: 'Avoid unserialize on untrusted data; use a safe data format.' },
  { lang: 'php', pattern: 'system', id: 'polyglot-php-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid shell APIs or strictly validate an argument array.' },
  { lang: 'php', pattern: 'shell_exec', id: 'polyglot-php-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid shell APIs or strictly validate an argument array.' },
  { lang: 'php', pattern: 'passthru', id: 'polyglot-php-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid shell APIs or strictly validate an argument array.' },
  { lang: 'php', pattern: 'exec', id: 'polyglot-php-shell', title: 'Shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid shell APIs or strictly validate an argument array.' },
  { lang: 'cpp', pattern: 'gets($A)', id: 'polyglot-cpp-gets', title: 'Unbounded C++ input read', severity: 'high', cwe: 'CWE-787', hint: 'Use a bounded input API.' },
  { lang: 'cpp', pattern: 'strcpy($A, $B)', id: 'polyglot-cpp-copy', title: 'Unbounded C++ string copy', severity: 'medium', cwe: 'CWE-120', hint: 'Use a bounds-checked copy and validate destination capacity.' },
  { lang: 'cpp', pattern: 'strcat($A, $B)', id: 'polyglot-cpp-strcat', title: 'Unbounded C++ string concatenation', severity: 'medium', cwe: 'CWE-120', hint: 'Use bounded string concatenation and verify buffer capacity.' },
  { lang: 'cpp', pattern: 'sprintf($BUF, $$$)', id: 'polyglot-cpp-sprintf', title: 'Unbounded C/C++ sprintf', severity: 'high', cwe: 'CWE-120', hint: 'Use snprintf with explicit buffer size.' },
  { lang: 'cpp', pattern: 'scanf("%s", $BUF)', id: 'polyglot-cpp-scanf', title: 'Unbounded C/C++ scanf("%s")', severity: 'high', cwe: 'CWE-120', hint: 'Specify a field width limit or use fgets to prevent stack buffer overflow.' },
  { lang: 'cpp', pattern: 'system($CMD)', id: 'polyglot-cpp-system', title: 'C/C++ shell command execution', severity: 'high', cwe: 'CWE-78', hint: 'Avoid system(); invoke an executable directly without a shell.' },
  { lang: 'csharp', pattern: 'new BinaryFormatter()', id: 'polyglot-csharp-binaryformatter', title: 'Unsafe .NET binary formatter', severity: 'high', cwe: 'CWE-502', hint: 'Replace BinaryFormatter with a safe, constrained format.' },
  { lang: 'csharp', pattern: 'new LosFormatter()', id: 'polyglot-csharp-losformatter', title: 'Unsafe .NET LosFormatter', severity: 'high', cwe: 'CWE-502', hint: 'Avoid LosFormatter on untrusted payloads.' },
  { lang: 'csharp', pattern: 'MD5.Create()', id: 'polyglot-csharp-weak-hash', title: 'Weak cryptographic hash (MD5)', severity: 'medium', cwe: 'CWE-327', hint: 'Use SHA256.Create() or stronger.' },
  { lang: 'csharp', pattern: 'SHA1.Create()', id: 'polyglot-csharp-weak-hash', title: 'Weak cryptographic hash (SHA-1)', severity: 'medium', cwe: 'CWE-327', hint: 'Use SHA256.Create() or stronger.' },
  { lang: 'csharp', pattern: 'Process.Start($CMD)', id: 'polyglot-csharp-process-start', title: 'Process launch in .NET', severity: 'medium', cwe: 'CWE-78', hint: 'Validate executable paths and disable UseShellExecute for untrusted input.' },
  { lang: 'swift', pattern: 'NSKeyedUnarchiver.unarchiveObject(with: $A)', id: 'polyglot-swift-deserialize', title: 'Non-coding-secure Swift unarchiver', severity: 'high', cwe: 'CWE-502', hint: 'Use NSSecureCoding and unarchivedObject(ofClass:from:).' },
  { lang: 'swift', pattern: 'evaluateJavaScript($A)', id: 'polyglot-swift-js-eval', title: 'Dynamic JavaScript evaluation in WebView', severity: 'medium', cwe: 'CWE-79', hint: 'Never interpolate untrusted data into evaluateJavaScript.' },
  { lang: 'bash', pattern: 'eval $CMD', id: 'polyglot-bash-eval', title: 'Dynamic shell evaluation', severity: 'high', cwe: 'CWE-95', hint: 'Avoid eval; keep data separate from executable commands.' },
];

let registered = false;
const RELIABLE_SYNTAX_ERROR_LANGS = new Set(['python', 'go', 'rust', 'java', 'cpp', 'csharp', 'bash']);
const rulesByLang = new Map();
for (const rule of rules) {
  const list = rulesByLang.get(rule.lang) || [];
  list.push(rule);
  rulesByLang.set(rule.lang, list);
}

function ensureParsers() {
  if (registered) return;
  registerDynamicLanguage(loadParsers());
  registered = true;
}

function analyzePolyglot(source, file, extension, suppressions) {
  const language = languageByExtension[extension];
  if (!language) return [];
  ensureParsers();
  let root;
  try {
    root = parse(language, source).root();
  } catch {
    return [];
  }

  const lines = source.split(/\r?\n/);
  const emitted = new Set();
  const findings = [];
  let syntaxErrors = [];
  if (RELIABLE_SYNTAX_ERROR_LANGS.has(language)) {
    try {
      syntaxErrors = root.findAll(kind(language, 'ERROR'));
    } catch (error) {
      syntaxErrors = error ? [] : [];
    }
  }
  for (const error of syntaxErrors.slice(0, 20)) {
    const line = error.range().start.line + 1;
    const ruleId = 'polyglot-syntax-error';
    if (suppressions && (
      suppressions.allLines.value
      || suppressions.fileWide.has(ruleId)
      || suppressions.byLine.get(line)?.has('*')
      || suppressions.byLine.get(line)?.has(ruleId)
    )) continue;
    const key = `${ruleId}:${line}`;
    if (emitted.has(key)) continue;
    emitted.add(key);
    findings.push({
      id: `${file}:${line}:${ruleId}`,
      ruleId,
      title: 'Possible syntax error',
      severity: 'medium',
      hint: 'Check this parser-reported syntax error; parser recovery may affect nearby findings.',
      confidence: 'medium',
      reason: 'The language parser produced an ERROR node at this location.',
      file,
      line,
      excerpt: String(lines[line - 1] || error.text()).trim().slice(0, 220),
    });
  }
  for (const rule of (rulesByLang.get(language) || [])) {
    let matches;
    try {
      matches = language === 'php'
        ? root.findAll(kind(language, 'function_call_expression')).filter((node) => node.namedChildren()[0]?.text() === rule.pattern)
        : root.findAll(rule.pattern);
    } catch {
      continue;
    }
    for (const match of matches) {
      const line = match.range().start.line + 1;
      const key = `${rule.id}:${line}`;
      if (emitted.has(key) || suppressions && (
        suppressions.allLines.value
        || suppressions.fileWide.has(rule.id)
        || suppressions.byLine.get(line)?.has('*')
        || suppressions.byLine.get(line)?.has(rule.id)
      )) continue;
      emitted.add(key);
      findings.push({
        id: `${file}:${line}:${rule.id}`,
        ruleId: rule.id,
        title: rule.title,
        severity: rule.severity,
        cwe: rule.cwe,
        hint: rule.hint,
        confidence: 'high',
        reason: 'Language AST matched a risky API pattern; review its input and runtime context.',
        file,
        line,
        excerpt: String(lines[line - 1] || match.text()).trim().slice(0, 220),
      });
    }
  }
  return findings;
}

module.exports = { analyzePolyglot, languageByExtension };
