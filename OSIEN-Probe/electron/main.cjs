'use strict';
const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const path = require('node:path');
const fs = require('node:fs/promises');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const execFileAsync = promisify(execFile);
// ipcMain is undefined when this file is required by the test runner.
const ipc = typeof ipcMain?.handle === 'function' ? ipcMain.handle.bind(ipcMain) : () => {};
const { scanProjectRoot, toSarifReport, toMarkdownReport, kinds } = require('./scanner.cjs');
const { verifyFinding, reviewFindings, getSourceContext, checkAiHealth } = require('./ai-review.cjs');
const isInside = (root, target) => { const rel = path.relative(root, target); return rel === '' || (!rel.startsWith(`..${path.sep}`) && rel !== '..' && !path.isAbsolute(rel)); };
const statePath = () => path.join(app.getPath('userData'), 'deepcheck-projects.json');
async function readProjects() { try { return JSON.parse(await fs.readFile(statePath(), 'utf8')); } catch { return []; } }
async function saveProjects(projects) { await fs.mkdir(path.dirname(statePath()), { recursive: true }); await fs.writeFile(statePath(), JSON.stringify(projects, null, 2), 'utf8'); }
function createWindow() {
  const win = new BrowserWindow({ width: 1440, height: 920, minWidth: 1000, minHeight: 680, backgroundColor: '#101112', title: 'OSIEN Probe', icon: path.join(__dirname, '../build/icon.png'), webPreferences: { preload: path.join(__dirname, 'preload.cjs'), contextIsolation: true, nodeIntegration: false } });
  if (process.env.VITE_DEV_SERVER_URL) win.loadURL(process.env.VITE_DEV_SERVER_URL); else win.loadFile(path.join(__dirname, '../dist/index.html'));
}
if (process.versions.electron && typeof ipcMain?.handle === 'function') {
  app.whenReady().then(() => { createWindow(); app.on('activate', () => { if (!BrowserWindow.getAllWindows().length) createWindow(); }); }).catch(() => app.quit());
  app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
}
// IPC handlers remain registered only in the app process; tests call scanProjectRoot directly.
ipc('pick-folder', async () => { const result = await dialog.showOpenDialog({ properties: ['openDirectory'] }); return result.canceled ? null : result.filePaths[0]; });
ipc('list-projects', readProjects);
async function persistProject(rootPath, kind, displayName) {
  if (!kinds.includes(kind)) throw new Error('Choose a project category');
  const root = path.resolve(rootPath); const stat = await fs.stat(root).catch(() => null);
  if (!stat?.isDirectory()) throw new Error('The selected folder no longer exists');
  const projects = await readProjects();
  if (projects.some(p => path.resolve(p.root).toLowerCase() === root.toLowerCase())) throw new Error('This project is already connected');
  const project = { id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, name: displayName || path.basename(root), root, kind, addedAt: new Date().toISOString(), latestRun: null, scanHistory: [] };
  projects.unshift(project); await saveProjects(projects); return project;
}
ipc('add-project', async (_event, payload) => {
  if (!payload || !kinds.includes(payload.kind) || typeof payload.root !== 'string') throw new Error('Choose a folder and project category');
  return persistProject(payload.root, payload.kind);
});
async function ensureGit() {
  const hasGit = () => execFileAsync('git', ['--version'], { windowsHide: true }).then(() => true).catch(() => false);
  if (await hasGit()) return;
  if (process.platform === 'win32') {
    await execFileAsync('winget', ['install', '--id', 'Git.Git', '-e', '--source', 'winget', '--accept-package-agreements', '--accept-source-agreements'], { timeout: 300000, windowsHide: true }).catch(() => null);
    if (await hasGit()) return;
    throw new Error('Git is not installed. Install Git for Windows from https://git-scm.com/download/win, restart DeepCheck, then retry cloning.');
  }
  const hint = process.platform === 'darwin'
    ? 'Install Git via Xcode Command Line Tools (xcode-select --install) or Homebrew (brew install git).'
    : 'Install Git via your package manager (e.g. sudo apt install git).';
  throw new Error(`Git is not installed. ${hint}`);
}
ipc('clone-repo', async (_event, payload) => {
  await ensureGit();
  const url = payload?.url;
  if (typeof url !== 'string' || !/^https:\/\/(github\.com|gitlab\.com|bitbucket\.org)\/[\w.-]+\/[\w.-]+(?:\.git)?$/.test(url)) throw new Error('Enter a public GitHub, GitLab, or Bitbucket repository URL');
  const repoName = url.replace(/\.git$/i, '').split('/').pop() || 'repository';
  const safeFolder = `${repoName.replace(/[^A-Za-z0-9._-]/g, '-')}-${Date.now()}`;
  const target = path.join(app.getPath('userData'), 'repositories', safeFolder); await fs.mkdir(path.dirname(target), { recursive: true });
  try { await execFileAsync('git', ['clone', '--depth', '1', url, target], { timeout: 120000, windowsHide: true }); }
  catch (error) { throw new Error(`Git clone failed: ${error.stderr || error.message}`); }
  return persistProject(target, payload.kind, repoName);
});
ipc('remove-project', async (_event, id) => { const projects = await readProjects(); const index = projects.findIndex(p => p.id === id); if (index < 0) throw new Error('Project not found'); const [removed] = projects.splice(index, 1); await saveProjects(projects); return removed; });
ipc('open-data-folder', async () => { const error = await shell.openPath(app.getPath('userData')); if (error) throw new Error(error); return true; });
ipc('open-project', async (_event, id) => { const p = (await readProjects()).find(item => item.id === id); if (!p) throw new Error('Project not found'); const error = await shell.openPath(p.root); if (error) throw new Error(error); return true; });
ipc('scan-project', async (_event, id) => {
  const projects = await readProjects(); const project = projects.find(p => p.id === id); if (!project) throw new Error('Project not found');
  const priorFindingIds = new Set((project.latestRun?.findings || []).map((f) => f.id));
  const priorScore = typeof project.latestRun?.score === 'number' ? project.latestRun.score : null;
  // A fresh source scan starts a new run; don't leave test output from the prior scan attached.
  if (project.latestRun) {
    delete project.latestRun.testResult;
    delete project.latestRun.testResults;
    for (const finding of project.latestRun.findings || []) { delete finding.aiAssessment; delete finding.aiReviewError; }
    await saveProjects(projects);
  }
  const result = await scanProjectRoot(project.root, project.kind);
  result.projectId = project.id;
  if (priorScore !== null) {
    result.scoreDelta = result.score - priorScore;
    for (const finding of result.findings) {
      finding.isNew = !priorFindingIds.has(finding.id);
    }
  }
  const history = Array.isArray(project.scanHistory) ? project.scanHistory : [];
  history.unshift({
    id: result.id,
    completedAt: result.completedAt,
    score: result.score,
    filesScanned: result.filesScanned,
    findingsCount: result.findings.length,
    durationMs: result.durationMs,
    severityCounts: result.severityCounts,
  });
  project.scanHistory = history.slice(0, 10);
  project.latestRun = result; await saveProjects(projects);
  // Return static results immediately; AI review runs in the background so the UI never blocks.
  startBackgroundReview(project.id, project.root, result);
  return result;
});
// ---- Background AI review -------------------------------------------------
// Only security-relevant findings are reviewed automatically (local CPU models
// take ~10-60s per finding). Lower severities can still be reviewed on demand.
const AUTO_REVIEW_SEVERITIES = new Set(['critical', 'high']);
const AUTO_REVIEW_LIMIT = 15;
const reviewJobs = new Map(); // projectId -> { cancelled, total, done, scanId }
let writeQueue = Promise.resolve();
function withProjects(mutator) {
  writeQueue = writeQueue.then(async () => {
    const fresh = await readProjects();
    const changed = await mutator(fresh);
    if (changed !== false) await saveProjects(fresh);
  }).catch(() => undefined);
  return writeQueue;
}
function broadcast(channel, payload) {
  for (const win of BrowserWindow.getAllWindows?.() || []) {
    if (!win.isDestroyed()) win.webContents.send(channel, payload);
  }
}
function startBackgroundReview(projectId, root, scan) {
  const previous = reviewJobs.get(projectId);
  if (previous) previous.cancelled = true;
  const queue = scan.findings.filter((f) => AUTO_REVIEW_SEVERITIES.has(f.severity)).slice(0, AUTO_REVIEW_LIMIT);
  const job = { cancelled: false, total: queue.length, done: 0, scanId: scan.id };
  reviewJobs.set(projectId, job);
  const progress = (state) => broadcast('ai-review-progress', { projectId, scanId: scan.id, total: job.total, done: job.done, state });
  if (!queue.length) { reviewJobs.delete(projectId); progress('idle'); return; }
  progress('running');
  (async () => {
    await reviewFindings(root, queue, {
      shouldStop: () => job.cancelled,
      onFinding: async (finding) => {
        if (job.cancelled) return;
        job.done++;
        await withProjects((fresh) => {
          const target = fresh.find((p) => p.id === projectId)?.latestRun;
          if (!target || target.id !== scan.id) return false;
          const stored = target.findings.find((f) => f.id === finding.id);
          if (!stored) return false;
          if (finding.aiAssessment) { stored.aiAssessment = finding.aiAssessment; delete stored.aiReviewError; }
          else if (finding.aiReviewError) { stored.aiReviewError = finding.aiReviewError; }
          return true;
        });
        progress('running');
      },
    });
    if (reviewJobs.get(projectId) === job) reviewJobs.delete(projectId);
    progress(job.cancelled ? 'cancelled' : 'done');
  })().catch(() => { reviewJobs.delete(projectId); progress('done'); });
}
ipc('cancel-ai-review', async (_event, projectId) => {
  const job = reviewJobs.get(projectId);
  if (job) job.cancelled = true;
  return Boolean(job);
});
ipc('ai-review-status', async () => Object.fromEntries([...reviewJobs.entries()].map(([id, j]) => [id, { total: j.total, done: j.done, scanId: j.scanId }])));
ipc('verify-finding', async (_event, payload) => {
  const { projectId, findingId } = payload || {};
  const projects = await readProjects();
  const project = projects.find((item) => item.id === projectId);
  if (!project?.latestRun) throw new Error('Scan this project before verifying a finding.');
  const finding = project.latestRun.findings.find((item) => item.id === findingId);
  if (!finding) throw new Error('Finding is no longer present in the latest scan. Run a fresh scan.');
  const assessment = await verifyFinding(project.root, finding);
  await withProjects((fresh) => {
    const stored = fresh.find((p) => p.id === projectId)?.latestRun?.findings.find((f) => f.id === findingId);
    if (!stored) return false;
    stored.aiAssessment = assessment;
    delete stored.aiReviewError;
    return true;
  });
  return assessment;
});
ipc('get-finding-context', async (_event, payload) => {
  const { projectId, findingId } = payload || {};
  const projects = await readProjects();
  const project = projects.find((item) => item.id === projectId);
  if (!project?.latestRun) throw new Error('Scan this project before viewing source context.');
  const finding = project.latestRun.findings.find((item) => item.id === findingId);
  if (!finding) throw new Error('Finding is no longer present in the latest scan.');
  return getSourceContext(project.root, finding.file, finding.line, 6);
});
ipc('check-ai-status', async () => checkAiHealth());
ipc('export-report', async (_event, payload) => {
  const format = payload?.format || 'sarif';
  const projects = await readProjects();
  const project = payload?.projectId ? projects.find((item) => item.id === payload.projectId) : null;
  const scan = project?.latestRun || {
    completedAt: new Date().toISOString(),
    filesScanned: projects.reduce((sum, p) => sum + (p.latestRun?.filesScanned || 0), 0),
    rulesEvaluated: projects.reduce((sum, p) => sum + (p.latestRun?.rulesEvaluated || 0), 0),
    score: projects.filter((p) => p.latestRun).length
      ? Math.round(projects.reduce((sum, p) => sum + (p.latestRun?.score || 0), 0) / projects.filter((p) => p.latestRun).length)
      : 100,
    findings: projects.flatMap((p) => (p.latestRun?.findings || []).map((f) => ({ ...f, file: `${p.name}/${f.file}` }))),
  };
  const ext = format === 'sarif' ? 'sarif.json' : format === 'markdown' ? 'md' : 'json';
  const baseName = project ? `${project.name}-deepcheck.${ext}` : `deepcheck-workspace.${ext}`;
  const saveResult = await dialog.showSaveDialog({
    defaultPath: baseName,
    filters: [
      format === 'sarif' ? { name: 'SARIF 2.1.0 Report', extensions: ['json', 'sarif'] }
        : format === 'markdown' ? { name: 'Markdown Report', extensions: ['md'] }
          : { name: 'JSON Report', extensions: ['json'] },
    ],
  });
  if (saveResult.canceled || !saveResult.filePath) return { saved: false, filePath: null };
  const body = format === 'sarif'
    ? JSON.stringify(toSarifReport(scan, project || { name: 'Workspace' }), null, 2)
    : format === 'markdown'
      ? toMarkdownReport(scan, project || { name: 'Workspace' })
      : JSON.stringify({ project: project ? { name: project.name, root: project.root, kind: project.kind } : null, scan }, null, 2);
  await fs.writeFile(saveResult.filePath, body, 'utf8');
  return { saved: true, filePath: saveResult.filePath };
});
ipc('run-project-tests', async (_event, id) => {
  const projects = await readProjects(); const project = projects.find(p => p.id === id); if (!project?.latestRun?.testCommand) throw new Error('No supported project test command was detected.');
  const command = project.latestRun.testCommand;
  const exec = (file, args) => execFileAsync(file, args, { cwd: project.root, timeout: 10 * 60 * 1000, windowsHide: true, maxBuffer: 2 * 1024 * 1024 });
  try {
    const runNpm = (args) => process.platform === 'win32'
      ? exec(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', ['npm', ...args].join(' ')])
      : exec('npm', args);
    const result = command === 'npm test'
      ? await runNpm(['test'])
      : command === 'python -m pytest'
        ? await exec(process.platform === 'win32' ? 'python' : 'python3', ['-m', 'pytest'])
        : command === 'cargo test'
          ? await exec('cargo', ['test'])
          : await exec('go', ['test', './...']);
    project.latestRun.testResult = { command, exitCode: 0, output: `${result.stdout}\n${result.stderr}`.trim().slice(-50000) };
  } catch (error) { project.latestRun.testResult = { command, exitCode: error.code ?? 1, output: `${error.stdout || ''}\n${error.stderr || error.message}`.trim().slice(-50000) }; }
  await saveProjects(projects); return project.latestRun.testResult;
});
ipc('run-project-test-file', async (_event, payload) => {
  const projects = await readProjects();
  const project = projects.find((item) => item.id === payload?.projectId);
  const run = project?.latestRun;
  if (!run) throw new Error('Scan this project before running its test files.');
  const target = run.testFiles?.find((item) => item.file === payload?.file);
  if (!target || !['node', 'npm', 'pytest', 'go', 'cargo'].includes(target.runner)) throw new Error('That file is not in the detected test plan.');

  const root = await fs.realpath(project.root);
  const relative = path.normalize(target.file);
  if (path.isAbsolute(relative) || relative.split(path.sep).includes('..') || (target.runner === 'npm' && !/^[\w .\\/-]+$/.test(target.file))) {
    throw new Error('The detected test path is not safe to execute.');
  }
  const fullPath = await fs.realpath(path.resolve(root, relative));
  if (!isInside(root, fullPath) || !(await fs.stat(fullPath)).isFile()) throw new Error('The detected test file is outside the project or no longer exists.');

  const startedAt = Date.now();
  const options = { cwd: root, timeout: 10 * 60 * 1000, windowsHide: true, maxBuffer: 2 * 1024 * 1024 };
  let result;
  try {
    let output;
    if (target.runner === 'node') {
      output = await execFileAsync(process.execPath, ['--test', relative], {
        ...options,
        env: { ...process.env, ELECTRON_RUN_AS_NODE: '1' },
      });
    } else if (target.runner === 'pytest') {
      output = await execFileAsync(process.platform === 'win32' ? 'python' : 'python3', ['-m', 'pytest', relative], options);
    } else if (target.runner === 'npm') {
      output = process.platform === 'win32'
        ? await execFileAsync(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', `npm test -- "${target.file}"`], options)
        : await execFileAsync('npm', ['test', '--', target.file], options);
    } else if (target.runner === 'cargo') {
      if (!/^tests[\\/][\w.-]+\.rs$/i.test(target.file)) throw new Error('Cargo target path is invalid.');
      output = await execFileAsync('cargo', ['test', '--test', path.basename(target.file, '.rs')], options);
    } else {
      if (!Array.isArray(target.names) || !target.names.length || target.names.some((name) => !/^(?:Test|Example|Fuzz)[A-Za-z0-9_]*$/.test(name))) {
        throw new Error('Go test names are invalid.');
      }
      const packagePath = `./${path.posix.dirname(target.file)}`.replace(/\/\.$/, '');
      const filter = `^(?:${target.names.join('|')})$`;
      output = await execFileAsync('go', ['test', packagePath, '-run', filter], options);
    }
    result = { file: target.file, command: target.command, exitCode: 0, output: `${output.stdout}\n${output.stderr}`.trim().slice(-50000), durationMs: Date.now() - startedAt };
  } catch (error) {
    result = { file: target.file, command: target.command, exitCode: error.code ?? 1, output: `${error.stdout || ''}\n${error.stderr || error.message}`.trim().slice(-50000), durationMs: Date.now() - startedAt };
  }

  const results = run.testResults || (run.testResults = []);
  const prior = results.findIndex((item) => item.file === result.file);
  if (prior >= 0) results.splice(prior, 1);
  results.push(result);
  await saveProjects(projects);
  return result;
});

module.exports = { scanProjectRoot, kinds };
