import { useEffect, useMemo, useState } from 'react'
import { Activity, ArrowUpRight, Bug, Check, ChevronDown, CircleHelp, Code2, Download, ExternalLink, Eye, FileCode2, Folder, Gauge, GitBranch, Globe2, History, Layers3, Play, Plus, RefreshCw, Search, Shield, ShieldCheck, SlidersHorizontal, Terminal, Trash2, X } from 'lucide-react'
import './App.css'
import './Working.css'
import './UIOverrides.css'

type Kind = 'Website' | 'Software' | 'API / backend' | 'Mobile app' | 'Library' | 'Data / ML'
type AiAssessment = { verdict: 'likely_real' | 'likely_false_positive' | 'uncertain'; confidence: number; rationale: string; evidence: string; reviewAdvice: string; model: string; reviewedAt: string }
type Finding = { id: string; title: string; severity: string; file: string; line: number; excerpt: string; ruleId?: string; cwe?: string; hint?: string; confidence?: string; reason?: string; isNew?: boolean; aiAssessment?: AiAssessment; aiReviewError?: string }
type TestTarget = { file: string; runner: 'node' | 'npm' | 'pytest' | 'go' | 'cargo'; names?: string[]; command: string }
type SeverityCounts = { critical: number; high: number; medium: number; low: number; info: number }
type ScanHistoryEntry = { id: string; completedAt: string; score: number; filesScanned: number; findingsCount: number; durationMs?: number; severityCounts?: SeverityCounts }
type Scan = { id: string; completedAt: string; durationMs?: number; filesScanned: number; rulesEvaluated: number; rulesAvailable?: number; languagesScanned?: string[]; severityCounts?: SeverityCounts; scoreDelta?: number; findings: Finding[]; truncated: boolean; testCommand: string | null; testFiles?: TestTarget[]; testFilesTruncated?: boolean; testResults?: TestResult[]; score: number; testResult?: TestResult }
type TestResult = { file?: string; command: string; exitCode: number; output: string; durationMs?: number }
type Project = { id: string; name: string; root: string; kind: Kind; addedAt: string; latestRun: Scan | null; scanHistory?: ScanHistoryEntry[] }
type ContextLine = { number: number; text: string; isTarget: boolean }
type SourceContext = { context: string; lines?: ContextLine[]; totalLines: number }
type AiHealth = { available: boolean; modelInstalled: boolean; model: string; endpoint: string; models: string[]; message: string }
type Route = 'Projects' | 'Test runs' | 'Findings' | 'Security' | 'Environments' | 'Settings'
type ConfirmState = {
  title: string
  body: string
  detail?: string
  confirmLabel: string
  tone: 'danger' | 'primary'
  onConfirm: () => Promise<void> | void
} | null
type Bridge = {
  pickFolder(): Promise<string | null>
  listProjects(): Promise<Project[]>
  addProject(p: { root: string; kind: Kind }): Promise<Project>
  cloneRepo(p: { url: string; kind: Kind }): Promise<Project>
  removeProject(id: string): Promise<Project>
  openProject(id: string): Promise<boolean>
  openDataFolder(): Promise<boolean>
  scanProject(id: string): Promise<Scan>
  verifyFinding(p: { projectId: string; findingId: string }): Promise<AiAssessment>
  getFindingContext?(p: { projectId: string; findingId: string }): Promise<SourceContext>
  checkAiStatus?(): Promise<AiHealth>
  exportReport?(p: { projectId?: string; format: 'sarif' | 'markdown' | 'json' }): Promise<{ saved: boolean; filePath: string | null }>
  runProjectTests(id: string): Promise<TestResult>
  runProjectTestFile(p: { projectId: string; file: string }): Promise<TestResult>
  cancelAiReview?(projectId: string): Promise<boolean>
  aiReviewStatus?(): Promise<Record<string, { total: number; done: number; scanId: string }>>
  onAiReviewProgress?(cb: (p: ReviewProgress) => void): () => void
}
type ReviewProgress = { projectId: string; scanId: string; total: number; done: number; state: 'running' | 'done' | 'cancelled' | 'idle' }
declare global { interface Window { deepcheck?: Bridge } }

const kinds: { name: Kind; detail: string; Icon: typeof Globe2 }[] = [
  { name: 'Website', detail: 'Web UI, accessibility, security', Icon: Globe2 },
  { name: 'Software', detail: 'Application source and dependencies', Icon: Code2 },
  { name: 'API / backend', detail: 'Services, auth, and data handling', Icon: Terminal },
  { name: 'Mobile app', detail: 'Mobile source and configuration', Icon: Layers3 },
  { name: 'Library', detail: 'Public API and package configuration', Icon: FileCode2 },
  { name: 'Data / ML', detail: 'Pipelines, data, and model code', Icon: Activity },
]
const icons: Record<Kind, typeof Globe2> = Object.fromEntries(kinds.map(({ name, Icon }) => [name, Icon])) as Record<Kind, typeof Globe2>
const nav: { name: Route; Icon: typeof Layers3 }[] = [
  { name: 'Projects', Icon: Layers3 },
  { name: 'Test runs', Icon: Activity },
  { name: 'Findings', Icon: Bug },
  { name: 'Security', Icon: Shield },
  { name: 'Environments', Icon: Terminal },
  { name: 'Settings', Icon: SlidersHorizontal },
]
const dateLabel = (value?: string | null) => value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : 'Not scanned'

function App() {
  const bridge = window.deepcheck
  const [projects, setProjects] = useState<Project[]>([])
  const [route, setRoute] = useState<Route>('Projects')
  const [loading, setLoading] = useState(() => Boolean(bridge))
  const [dialog, setDialog] = useState(false)
  const [source, setSource] = useState<'folder' | 'repo'>('folder')
  const [url, setUrl] = useState('')
  const [folder, setFolder] = useState('')
  const [kind, setKind] = useState<Kind | null>(null)
  const [busy, setBusy] = useState('')
  const [message, setMessage] = useState(() => bridge ? '' : 'DeepCheck requires its Windows desktop app.')
  const [filter, setFilter] = useState('')
  const [category, setCategory] = useState('All categories')
  const [page, setPage] = useState(0)
  const [selectedProject, setSelectedProject] = useState<Project | null>(null)
  const [workspaceOpen, setWorkspaceOpen] = useState(false)
  const [help, setHelp] = useState(false)
  const [confirmClear, setConfirmClear] = useState(false)
  const [confirmState, setConfirmState] = useState<ConfirmState>(null)
  const [aiHealth, setAiHealth] = useState<AiHealth | null>(null)
  const [reviews, setReviews] = useState<Record<string, ReviewProgress>>({})

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setDialog(false)
        setHelp(false)
        setConfirmClear(false)
        setConfirmState(null)
        setWorkspaceOpen(false)
        return
      }
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        const input = document.querySelector<HTMLInputElement>('.search input')
        input?.focus()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  useEffect(() => {
    if (!bridge?.onAiReviewProgress) return undefined
    const off = bridge.onAiReviewProgress(p => {
      setReviews(current => {
        const next = { ...current }
        if (p.state === 'running') next[p.projectId] = p
        else delete next[p.projectId]
        return next
      })
      bridge.listProjects().then(fresh => {
        setProjects(fresh)
        setSelectedProject(current => current ? fresh.find(item => item.id === current.id) || null : null)
      }).catch(() => undefined)
    })
    return off
  }, [bridge])

  useEffect(() => {
    if (!bridge) return undefined
    let active = true
    bridge.listProjects().then(list => { if (active) setProjects(list) }).catch(e => { if (active) setMessage(errorText(e)) }).finally(() => { if (active) setLoading(false) })
    if (bridge.checkAiStatus) {
      bridge.checkAiStatus().then(status => { if (active) setAiHealth(status) }).catch(() => undefined)
    }
    return () => { active = false }
  }, [bridge])

  const findings = useMemo(() => projects.flatMap(p => (p.latestRun?.findings || []).map(f => ({ ...f, projectName: p.name, projectId: p.id }))), [projects])
  const shown = useMemo(() => projects.filter(p => `${p.name} ${p.kind} ${p.root}`.toLowerCase().includes(filter.toLowerCase()) && (category === 'All categories' || p.kind === category)), [projects, filter, category])
  const pages = Math.max(1, Math.ceil(shown.length / 8))
  const pageRows = shown.slice(page * 8, page * 8 + 8)
  const totalChecks = projects.reduce((n, p) => n + (p.latestRun?.rulesEvaluated || 0), 0)
  const scannedProjects = projects.filter(p => p.latestRun)
  const average = scannedProjects.length ? Math.round(scannedProjects.reduce((n, p) => n + (p.latestRun?.score || 0), 0) / scannedProjects.length) : null

  const reload = async () => {
    const fresh = await bridge!.listProjects()
    setProjects(fresh)
    setSelectedProject(current => current ? fresh.find(p => p.id === current.id) || null : null)
    return fresh
  }
  const refreshAll = async () => {
    setBusy('refresh')
    setMessage('')
    try {
      await reload()
      if (bridge?.checkAiStatus) setAiHealth(await bridge.checkAiStatus())
    } catch (e) {
      setMessage(errorText(e))
    } finally {
      setBusy('')
    }
  }
  const scan = async (p: Project) => {
    const resetRun = (run: Scan): Scan => ({ ...run, testResult: undefined, findings: run.findings.map(({ aiAssessment: _assessment, aiReviewError: _error, ...finding }) => finding) })
    setBusy(`scan:${p.id}`)
    setMessage('')
    setProjects(current => current.map(item => item.id === p.id && item.latestRun ? { ...item, latestRun: resetRun(item.latestRun) } : item))
    setSelectedProject(current => current?.id === p.id && current.latestRun ? { ...current, latestRun: resetRun(current.latestRun) } : current)
    try {
      await bridge!.scanProject(p.id)
      const fresh = await reload()
      setSelectedProject(fresh.find(item => item.id === p.id) || null)
      setRoute('Test runs')
    } catch (e) {
      setMessage(errorText(e))
    } finally {
      setBusy('')
    }
  }
  const verifyFinding = async (projectId: string, findingId: string) => {
    setBusy(`verify:${findingId}`)
    setMessage('')
    try {
      await bridge!.verifyFinding({ projectId, findingId })
      await reload()
    } catch (e) {
      setMessage(errorText(e))
    } finally {
      setBusy('')
    }
  }
  const exportReport = async (format: 'sarif' | 'markdown' | 'json', projectId?: string) => {
    if (!bridge?.exportReport) return
    setBusy(`export:${format}`)
    setMessage('')
    try {
      const res = await bridge.exportReport({ projectId, format })
      if (res.saved && res.filePath) setMessage(`Report exported to ${res.filePath}`)
    } catch (e) {
      setMessage(errorText(e))
    } finally {
      setBusy('')
    }
  }
  const runTests = async (p: Project) => {
    if (!p.latestRun?.testCommand) return
    const cmd = p.latestRun.testCommand
    setConfirmState({
      title: `Run ${p.name} test suite?`,
      body: 'This executes the project’s own test script on this device.',
      detail: `${cmd} · ${p.root}`,
      confirmLabel: 'Run full suite',
      tone: 'primary',
      onConfirm: async () => {
        setBusy(`test:${p.id}`)
        setMessage('')
        try {
          await bridge!.runProjectTests(p.id)
          const fresh = await reload()
          setSelectedProject(fresh.find(item => item.id === p.id) || null)
          setRoute('Test runs')
        } catch (e) {
          setMessage(errorText(e))
        } finally {
          setBusy('')
        }
      },
    })
  }
  const runTestFile = async (p: Project, target: TestTarget) => {
    setConfirmState({
      title: `Run ${target.file}?`,
      body: 'Test code can modify files or access the network. Only run tests from projects you trust.',
      detail: target.command,
      confirmLabel: 'Run test file',
      tone: 'primary',
      onConfirm: async () => {
        setBusy(`testfile:${p.id}:${target.file}`)
        setMessage('')
        try {
          await bridge!.runProjectTestFile({ projectId: p.id, file: target.file })
          const fresh = await reload()
          setSelectedProject(fresh.find(item => item.id === p.id) || null)
        } catch (e) {
          setMessage(errorText(e))
        } finally {
          setBusy('')
        }
      },
    })
  }
  const addProject = async () => {
    if (!kind) return
    setBusy('add')
    setMessage('')
    try {
      const p = source === 'repo' ? await bridge!.cloneRepo({ url, kind }) : await bridge!.addProject({ root: folder, kind })
      setProjects(current => [p, ...current])
      setDialog(false)
      setKind(null)
      setFolder('')
      setUrl('')
      if (source === 'repo') {
        setBusy(`scan:${p.id}`)
        try {
          const result = await bridge!.scanProject(p.id)
          const refreshed = await bridge!.listProjects()
          setProjects(refreshed)
          setSelectedProject(refreshed.find(item => item.id === p.id) || { ...p, latestRun: result })
          setRoute('Test runs')
          setMessage('Repository cloned and scanned. Review detected test command before running tests.')
        } catch (e) {
          setMessage(errorText(e))
        }
      }
    } catch (e) {
      setMessage(errorText(e))
    } finally {
      setBusy('')
    }
  }
  const removeProject = (p: Project) => {
    setConfirmState({
      title: `Remove ${p.name} from OSIEN Probe?`,
      body: 'OSIEN Probe will forget this connected project entry and its scan history. Your files on disk will not be deleted.',
      detail: p.root,
      confirmLabel: 'Remove project',
      tone: 'danger',
      onConfirm: async () => {
        try {
          await bridge!.removeProject(p.id)
          setProjects(await bridge!.listProjects())
          if (selectedProject?.id === p.id) setSelectedProject(null)
        } catch (e) {
          setMessage(errorText(e))
        }
      },
    })
  }
  const clearProjects = async () => {
    for (const p of projects) await bridge!.removeProject(p.id)
    setProjects([])
    setConfirmClear(false)
    setRoute('Projects')
  }
  const navTo = (item: Route) => { setRoute(item); setPage(0); setMessage('') }
  const securityCount = findings.filter(f => ['critical', 'high'].includes(f.severity)).length

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><img className="brand-logo" src="./logo.png" alt="OSIEN Probe"/><span>OSIEN Probe</span></div>
      <div className="workspace-wrap">
        <button className="workspace" onClick={() => setWorkspaceOpen(v => !v)}>
          <div className="workspace-icon"><Folder size={15}/></div>
          <div><strong>Local workspace</strong><small>{projects.length} {projects.length === 1 ? 'project' : 'projects'}</small></div>
          <ChevronDown size={15}/>
        </button>
        {workspaceOpen && <div className="workspace-menu">
          <div className="workspace-menu-title">Local workspace</div>
          <div className="workspace-menu-row"><span>Projects</span><b>{projects.length}</b></div>
          <div className="workspace-menu-row"><span>Scanned files</span><b>{projects.reduce((n, p) => n + (p.latestRun?.filesScanned || 0), 0).toLocaleString()}</b></div>
          <button onClick={() => { setWorkspaceOpen(false); void bridge!.openDataFolder() }}>Open data folder</button>
          <button onClick={() => { setWorkspaceOpen(false); void refreshAll() }}>Refresh projects</button>
          <button onClick={() => { setWorkspaceOpen(false); setRoute('Settings') }}>Settings</button>
        </div>}
      </div>
      <div className="nav-label">Workspace</div>
      <nav>
        {nav.slice(0, 4).map(({ name, Icon }) => <button key={name} className={route === name ? 'active' : ''} onClick={() => navTo(name)}>
          <Icon size={17}/>{name}
          {name === 'Projects' && <span className="nav-count">{projects.length}</span>}
          {name === 'Findings' && <span className="nav-count">{findings.length}</span>}
          {name === 'Security' && securityCount > 0 && <span className="nav-count issue">{securityCount}</span>}
        </button>)}
      </nav>
      <div className="nav-label tools-label">Tools</div>
      <nav>{nav.slice(4).map(({ name, Icon }) => <button key={name} className={route === name ? 'active' : ''} onClick={() => navTo(name)}><Icon size={17}/>{name}</button>)}</nav>
      <div className="side-bottom">
        <div className="plan">
          <div className="plan-top"><span>Scanned source files</span><span>{projects.reduce((n, p) => n + (p.latestRun?.filesScanned || 0), 0).toLocaleString()}</span></div>
          <small>Across connected projects</small>
        </div>
        <div className="profile">
          <div className="avatar"><img className="avatar-logo" src="./logo.png" alt="OSIEN Probe"/></div>
          <div><strong>Local analysis</strong><small>{aiHealth?.modelInstalled ? `AI ready (${aiHealth.model})` : 'Data stays on this device'}</small></div>
        </div>
      </div>
    </aside>
    <main className="main-area">
      <header className="topbar">
        <div className="breadcrumbs">Local workspace <span>/</span> <b>{route}</b></div>
        <div className="top-actions">
          <button className="icon-button" aria-label="Help" onClick={() => setHelp(true)}><CircleHelp size={17}/></button>
          <span className="top-divider"/>
          <span className="local-indicator"><i/> On this device</span>
        </div>
      </header>
      <section className="content">
        <div className="page-heading">
          <div>
            <div className="eyebrow"><span className="live-dot"/> LOCAL CODE ANALYSIS</div>
            <h1>{route}</h1>
            <p>{pageDescription(route)}</p>
          </div>
          <div className="heading-actions">
            {selectedProject && route === 'Test runs' && <button className="filter-button" onClick={() => setSelectedProject(null)}>Show all projects</button>}
            <button className="filter-button" onClick={() => void refreshAll()} disabled={!!busy}><RefreshCw size={14} className={busy === 'refresh' ? 'spinning' : ''}/> Refresh data</button>
            {route === 'Projects' && <button className="primary" onClick={() => { setDialog(true); setMessage('') }}><Plus size={17}/> Add project</button>}
          </div>
        </div>
        {message && <div className="global-message" role="alert">{message}<button className="icon-button" onClick={() => setMessage('')}><X size={15}/></button></div>}
        {Object.values(reviews).map(r => {
          const name = projects.find(p => p.id === r.projectId)?.name || 'project'
          return <div className="review-banner" key={r.projectId}>
            <RefreshCw size={13} className="spinning"/>
            <span>Local AI is double-checking {name}'s critical/high findings in the background · {r.done}/{r.total} done. Results are already usable.</span>
            <button className="filter-button" onClick={() => void bridge?.cancelAiReview?.(r.projectId)}>Stop AI review</button>
          </div>
        })}
        {loading ? <div className="empty-state">Loading saved projects…</div> : route === 'Projects' ? <>
          <div className="metrics">
            <Metric label="Connected projects" value={projects.length.toString()} note="Saved on this device"/>
            <Metric label="Checks evaluated" value={totalChecks.toLocaleString()} note="From completed scans"/>
            <Metric label="Open findings" value={findings.length.toString()} note={securityCount ? `${securityCount} critical/high severity` : 'Latest scan per project'}/>
            <Metric label="Average scan score" value={average === null ? '—' : `${average}/100`} note={average === null ? 'Run a scan to calculate' : 'From completed scans'} featured/>
          </div>
          <div className="section-heading">
            <div><h2>Your projects</h2><span>{shown.length} shown</span></div>
            <div className="list-actions">
              <label className="search"><Search size={15}/><input aria-label="Search projects" placeholder="Search projects" value={filter} onChange={e => { setFilter(e.target.value); setPage(0) }}/></label>
              <select className="filter-button" aria-label="Filter by category" value={category} onChange={e => { setCategory(e.target.value); setPage(0) }}>
                <option>All categories</option>
                {kinds.map(k => <option key={k.name}>{k.name}</option>)}
              </select>
            </div>
          </div>
          <div className="project-table">
            <div className="table-head"><span>Project</span><span>Category</span><span>Latest scan</span><span>Checks</span><span>Score</span><span>Actions</span></div>
            {pageRows.map(p => {
              const Icon = icons[p.kind]
              const delta = p.latestRun?.scoreDelta
              return <div className="project-row" key={p.id}>
                <button className="project-name project-open" onClick={() => void bridge!.openProject(p.id)} title="Open project folder">
                  <div className="project-icon icon-code"><Icon size={17}/></div>
                  <div><strong>{p.name}</strong><small>{p.root}</small></div>
                </button>
                <div><span className="category"><span className="category-dot"/>{p.kind}</span></div>
                <div className="run-state"><span className={`state-dot ${p.latestRun ? '' : 'queued'}`}/><span>{dateLabel(p.latestRun?.completedAt)}</span></div>
                <div className="checks-count">{p.latestRun ? p.latestRun.rulesEvaluated.toLocaleString() : '—'} <span>{p.latestRun ? 'checks' : ''}</span></div>
                <div className="score-cell">
                  {p.latestRun ? <>
                    <div className={`score-ring ${p.latestRun.score >= 80 ? 'score-good' : p.latestRun.score >= 50 ? 'score-warn' : 'score-bad'}`}><span>{p.latestRun.score}</span></div>
                    <span className="score-label">{typeof delta === 'number' && delta !== 0 ? `${delta > 0 ? '+' : ''}${delta}` : 'Score'}</span>
                  </> : <span className="muted">—</span>}
                </div>
                <div className="row-actions">
                  <button className="row-action" aria-label={`Scan ${p.name}`} title="Run source scan" disabled={busy === `scan:${p.id}`} onClick={() => void scan(p)}>
                    {busy === `scan:${p.id}` ? <RefreshCw size={14} className="spinning"/> : <Play size={14}/>}
                  </button>
                  <button className="row-action danger-action" aria-label={`Remove ${p.name}`} title="Remove project" onClick={() => void removeProject(p)}>
                    <Trash2 size={14}/>
                  </button>
                </div>
              </div>
            })}
            {!pageRows.length && <div className="empty-state">{projects.length ? 'No projects match these filters' : <>No projects connected yet<br/><button className="text-action" onClick={() => setDialog(true)}>Add a folder or Git repository</button></>}</div>}
          </div>
          <div className="table-foot">
            <span>Page {page + 1} of {pages}</span>
            <div>
              <button disabled={page === 0} onClick={() => setPage(n => Math.max(0, n - 1))}>Previous</button>
              <button disabled={page + 1 >= pages} onClick={() => setPage(n => Math.min(pages - 1, n + 1))}>Next</button>
            </div>
          </div>
          <div className="bottom-note">
            <div className="note-icon"><Gauge size={18}/></div>
            <div><strong>Scans report what they actually inspect</strong><p>Source checks are static analysis; your test suite only runs when you choose Run tests</p></div>
            <button onClick={() => setHelp(true)}>Scan details <ArrowUpRight size={14}/></button>
          </div>
        </> : route === 'Findings' || route === 'Security' ? (
          <FindingList
            findings={route === 'Security' ? findings.filter(f => ['critical', 'high'].includes(f.severity)) : findings}
            projects={projects}
            busy={busy}
            bridge={bridge}
            onVerify={verifyFinding}
            onExport={exportReport}
            onOpen={p => { setSelectedProject(p); setRoute('Test runs') }}
          />
        ) : route === 'Test runs' ? (
          <Runs projects={projects} selected={selectedProject} busy={busy} onScan={scan} onTest={runTests} onRunFile={runTestFile} onExport={exportReport}/>
        ) : route === 'Environments' ? (
          <Environments projects={projects} aiHealth={aiHealth} onCheckAi={refreshAll} onOpen={p => void bridge!.openProject(p.id)} onTest={runTests} busy={busy}/>
        ) : (
          <div className="settings-panel">
            <h2>Local data &amp; AI review</h2>
            <p>Connected project metadata, scan history, and latest scan results are stored in this Windows user profile. Project source files are not copied for local-folder projects.</p>
            {aiHealth && <div className="settings-row">
              <div>
                <strong>Local Ollama review engine ({aiHealth.model})</strong>
                <small>{aiHealth.message}</small>
              </div>
              <button className="filter-button" onClick={() => void refreshAll()} disabled={busy === 'refresh'}>Check status</button>
            </div>}
            <div className="settings-row">
              <div><strong>Export workspace report</strong><small>Save all current project findings as SARIF 2.1.0 or Markdown</small></div>
              <div className="export-actions">
                <button className="filter-button" onClick={() => void exportReport('sarif')} disabled={!findings.length || !!busy}><Download size={13}/> SARIF</button>
                <button className="filter-button" onClick={() => void exportReport('markdown')} disabled={!findings.length || !!busy}><Download size={13}/> Markdown</button>
              </div>
            </div>
            <div className="settings-row">
              <div><strong>Remove all connected projects</strong><small>Does not delete project folders or cloned repositories</small></div>
              <button className="filter-button danger-button" onClick={() => setConfirmClear(true)} disabled={!projects.length}>Remove all</button>
            </div>
          </div>
        )}
      </section>
      <footer><span>OSIEN Probe <span className="footer-sep">·</span> Local desktop app</span><span><span className="footer-status"/> Local analysis</span></footer>
    </main>
    {dialog && <div className="overlay" onMouseDown={e => { if (e.target === e.currentTarget && busy !== 'add') setDialog(false) }}>
      <section className="dialog">
        <div className="dialog-head">
          <div><div className="dialog-icon"><Plus size={18}/></div><h2>Add a project</h2><p>Connect a local folder or clone a repository</p></div>
          <button className="icon-button" onClick={() => setDialog(false)}><X size={18}/></button>
        </div>
        <div className="source-tabs">
          <button className={source === 'folder' ? 'selected' : ''} onClick={() => setSource('folder')}><Folder size={16}/> Local folder</button>
          <button className={source === 'repo' ? 'selected' : ''} onClick={() => setSource('repo')}><GitBranch size={16}/> Git repository</button>
        </div>
        {source === 'repo' ? (
          <label className="field-label">Repository URL<input className="text-field" placeholder="https://github.com/owner/repository" value={url} onChange={e => setUrl(e.target.value)}/></label>
        ) : (
          <div className="folder-picker">
            <div><Folder size={17}/><span>{folder || 'No folder selected'}</span></div>
            <button onClick={async () => { try { const picked = await bridge!.pickFolder(); if (picked) setFolder(picked) } catch (e) { setMessage(errorText(e)) } }}>Browse</button>
          </div>
        )}
        <div className="field-label category-label">Project category <span>Required</span></div>
        <div className="kind-grid">
          {kinds.map(({ name, detail, Icon }) => <button key={name} className={`kind-option ${kind === name ? 'chosen' : ''}`} onClick={() => setKind(name)}>
            <Icon size={17}/><span><strong>{name}</strong><small>{detail}</small></span>{kind === name && <Check size={15}/>}
          </button>)}
        </div>
        {message && <div className="form-error">{message}</div>}
        <div className="dialog-foot">
          <span>Scans inspect files; they do not execute project code</span>
          <button className="primary" disabled={!kind || busy === 'add' || (source === 'folder' ? !folder : !url)} onClick={() => void addProject()}>
            {busy === 'add' ? 'Adding…' : source === 'repo' ? 'Clone and add' : 'Add project'} <ArrowUpRight size={15}/>
          </button>
        </div>
      </section>
    </div>}
    {help && <InfoDialog onClose={() => setHelp(false)}/>}
    {confirmClear && <div className="overlay" onMouseDown={e => { if (e.target === e.currentTarget) setConfirmClear(false) }}><section className="dialog confirm-dialog">
      <div className="dialog-brand"><img className="dialog-logo" src="./logo.png" alt="OSIEN Probe"/><div><h2>Remove all projects?</h2><p>OSIEN Probe will forget all connected entries and scan results.</p></div></div>
      <p>Your folders and cloned repositories on disk will not be deleted.</p>
      <div className="dialog-foot">
        <button className="filter-button" onClick={() => setConfirmClear(false)}>Cancel</button>
        <button className="danger-button" onClick={() => void clearProjects()}>Remove all</button>
      </div>
    </section></div>}
    {confirmState && <div className="overlay" onMouseDown={e => { if (e.target === e.currentTarget) setConfirmState(null) }}><section className="dialog confirm-dialog">
      <div className="dialog-brand"><img className="dialog-logo" src="./logo.png" alt="OSIEN Probe"/><div><h2>{confirmState.title}</h2><p>{confirmState.body}</p></div></div>
      {confirmState.detail && <code className="confirm-detail">{confirmState.detail}</code>}
      <div className="dialog-foot">
        <button className="filter-button" onClick={() => setConfirmState(null)}>Cancel</button>
        <button
          className={confirmState.tone === 'danger' ? 'danger-button' : 'primary'}
          onClick={() => {
            const action = confirmState.onConfirm
            setConfirmState(null)
            void action()
          }}
        >
          {confirmState.confirmLabel}
        </button>
      </div>
    </section></div>}
  </div>
}

function Metric({ label, value, note, featured = false }: { label: string; value: string; note: string; featured?: boolean }) {
  return <div className={`metric ${featured ? 'featured-metric' : ''}`}><span>{label}</span><strong>{value}</strong><small>{note}</small></div>
}

function FindingList({
  findings,
  projects,
  busy,
  bridge,
  onVerify,
  onExport,
  onOpen,
}: {
  findings: (Finding & { projectId: string; projectName: string })[]
  projects: Project[]
  busy: string
  bridge?: Bridge
  onVerify(projectId: string, findingId: string): void
  onExport(format: 'sarif' | 'markdown' | 'json', projectId?: string): void
  onOpen(p: Project): void
}) {
  const [copied, setCopied] = useState(false)
  const [copiedRule, setCopiedRule] = useState('')
  const [query, setQuery] = useState('')
  const [projectFilter, setProjectFilter] = useState('All projects')
  const [severityFilter, setSeverityFilter] = useState('All severities')
  const [confidenceFilter, setConfidenceFilter] = useState('All confidence')
  const [verdictFilter, setVerdictFilter] = useState('All verdicts')
  const [openContextKey, setOpenContextKey] = useState<string | null>(null)
  const [contexts, setContexts] = useState<Record<string, SourceContext>>({})
  const [contextError, setContextError] = useState<Record<string, string>>({})

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    const order = ['critical', 'high', 'medium', 'low', 'info']
    return findings
      .filter(f => {
        if (projectFilter !== 'All projects' && f.projectId !== projectFilter) return false
        if (severityFilter !== 'All severities' && f.severity !== severityFilter) return false
        if (confidenceFilter !== 'All confidence' && (f.confidence || 'medium') !== confidenceFilter) return false
        if (verdictFilter !== 'All verdicts') {
          const v = f.aiAssessment?.verdict || 'unreviewed'
          if (v !== verdictFilter) return false
        }
        if (!q) return true
        return `${f.title} ${f.file}:${f.line} ${f.cwe || ''} ${f.ruleId || ''} ${f.excerpt} ${f.projectName}`.toLowerCase().includes(q)
      })
      .sort((a, b) => order.indexOf(a.severity) - order.indexOf(b.severity))
  }, [findings, query, projectFilter, severityFilter, confidenceFilter, verdictFilter])

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(formatFindings(filtered))
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      setCopied(false)
    }
  }

  const toggleContext = async (projectId: string, findingId: string) => {
    const key = `${projectId}:${findingId}`
    if (openContextKey === key) {
      setOpenContextKey(null)
      return
    }
    setOpenContextKey(key)
    if (contexts[key] || !bridge?.getFindingContext) return
    try {
      const ctx = await bridge.getFindingContext({ projectId, findingId })
      setContexts(prev => ({ ...prev, [key]: ctx }))
    } catch (e) {
      setContextError(prev => ({ ...prev, [key]: errorText(e) }))
    }
  }

  const copySuppression = async (ruleId?: string) => {
    const text = `// deepcheck-disable-next-line${ruleId ? ` ${ruleId}` : ''}`
    try {
      await navigator.clipboard.writeText(text)
      setCopiedRule(ruleId || '*')
      setTimeout(() => setCopiedRule(''), 1400)
    } catch {
      setCopiedRule('')
    }
  }

  return <div className="detail-panel">
    <div className="detail-summary">
      <span>{filtered.length} of {findings.length} finding{findings.length === 1 ? '' : 's'} shown · critical/high findings are double-checked by local AI in the background</span>
      <div className="export-actions">
        {bridge?.exportReport && <>
          <button className="filter-button copy-button" disabled={!filtered.length || !!busy} onClick={() => onExport('sarif', projectFilter !== 'All projects' ? projectFilter : undefined)}><Download size={12}/> SARIF</button>
          <button className="filter-button copy-button" disabled={!filtered.length || !!busy} onClick={() => onExport('markdown', projectFilter !== 'All projects' ? projectFilter : undefined)}><Download size={12}/> Markdown</button>
        </>}
        <button className="filter-button copy-button" disabled={!filtered.length} onClick={() => void copy()}>{copied ? 'Copied' : 'Copy findings'}</button>
      </div>
    </div>
    {findings.length > 0 && <div className="severity-strip">
      {(['critical', 'high', 'medium', 'low', 'info'] as const).map(sev => {
        const count = findings.filter(f => f.severity === sev).length
        const active = severityFilter === sev
        return <button
          key={sev}
          type="button"
          className={`sev-pill severity-${sev} ${active ? 'sev-pill-active' : ''}`}
          onClick={() => setSeverityFilter(active ? 'All severities' : sev)}
        >
          <span>{sev}</span>
          <b>{count}</b>
        </button>
      })}
    </div>}
    {findings.length > 0 && <div className="triage-bar">
      <label className="search triage-search">
        <Search size={14}/>
        <input aria-label="Search findings" placeholder="Filter by file, rule, CWE, or code…" value={query} onChange={e => setQuery(e.target.value)}/>
      </label>
      <select className="filter-button" aria-label="Filter findings by project" value={projectFilter} onChange={e => setProjectFilter(e.target.value)}>
        <option value="All projects">All projects</option>
        {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select>
      <select className="filter-button" aria-label="Filter findings by severity" value={severityFilter} onChange={e => setSeverityFilter(e.target.value)}>
        <option value="All severities">All severities</option>
        <option value="critical">Critical</option>
        <option value="high">High</option>
        <option value="medium">Medium</option>
        <option value="low">Low</option>
        <option value="info">Info</option>
      </select>
      <select className="filter-button" aria-label="Filter findings by confidence" value={confidenceFilter} onChange={e => setConfidenceFilter(e.target.value)}>
        <option value="All confidence">All confidence</option>
        <option value="high">High confidence</option>
        <option value="medium">Medium confidence</option>
        <option value="low">Low confidence</option>
      </select>
      <select className="filter-button" aria-label="Filter findings by AI verdict" value={verdictFilter} onChange={e => setVerdictFilter(e.target.value)}>
        <option value="All verdicts">All verdicts</option>
        <option value="likely_real">Likely true</option>
        <option value="likely_false_positive">Likely false positive</option>
        <option value="uncertain">Uncertain (50/50)</option>
        <option value="unreviewed">Unreviewed</option>
      </select>
    </div>}
    {filtered.map(f => {
      const project = projects.find(item => item.id === f.projectId)
      const assessment = f.aiAssessment
      const label = assessment?.verdict === 'likely_real' ? 'Likely true' : assessment?.verdict === 'likely_false_positive' ? 'Likely false · possible scanner false positive' : 'Not confirmed · 50/50'
      const ctxKey = `${f.projectId}:${f.id}`
      const isExpanded = openContextKey === ctxKey
      const ctx = contexts[ctxKey]
      const ctxErr = contextError[ctxKey]
      return <div className="finding-row" key={ctxKey}>
        <span className={`severity severity-${f.severity}`}>{f.severity}</span>
        <button className="finding-open" onClick={() => { if (project) onOpen(project) }}>
          <span className="finding-main">
            <strong>
              {f.title}{f.cwe ? ` · ${f.cwe}` : ''}
              {f.isNew && <span className="new-badge">NEW</span>}
            </strong>
            <small>{f.projectName} · {f.file}:{f.line} · {f.confidence || 'medium'} scanner confidence{f.ruleId ? ` · ${f.ruleId}` : ''}</small>
            <code>{f.excerpt}</code>
            {f.reason && <small>{f.reason}</small>}
            {f.hint && <small className="finding-hint">Fix: {f.hint}</small>}
          </span>
          <ExternalLink size={15}/>
        </button>
        <div className="ai-review-action">
          <div className="finding-toolbar">
            {bridge?.getFindingContext && <button className="filter-button" onClick={() => void toggleContext(f.projectId, f.id)}>
              <Eye size={12}/> {isExpanded ? 'Hide code context' : 'Inspect code'}
            </button>}
            {f.ruleId && <button className="filter-button" onClick={() => void copySuppression(f.ruleId)}>
              {copiedRule === f.ruleId ? 'Copied directive' : `Copy suppress (${f.ruleId})`}
            </button>}
          </div>
          {isExpanded && <div className="context-inspector">
            {ctx?.lines ? <pre className="context-code">{ctx.lines.map(line => (
              <div key={line.number} className={`context-line ${line.isTarget ? 'context-line-target' : ''}`}>
                <span className="context-lineno">{line.number}</span>
                <span>{line.text || ' '}</span>
              </div>
            ))}</pre> : ctxErr ? <small className="run-ai-error">{ctxErr}</small> : <small>Loading source lines…</small>}
          </div>}
          {assessment ? <div className={`ai-assessment verdict-${assessment.verdict}`}>
            <strong>{label} · {assessment.confidence}%</strong>
            <span>{assessment.rationale}</span>
            {assessment.evidence && <small>Evidence: {assessment.evidence}</small>}
            {assessment.reviewAdvice && <small>Review: {assessment.reviewAdvice}</small>}
            <small>{assessment.model} · Local only · Snippet context only</small>
            <button className="filter-button" disabled={busy === `verify:${f.id}`} onClick={() => onVerify(f.projectId, f.id)}>{busy === `verify:${f.id}` ? 'Rechecking…' : 'Recheck finding'}</button>
          </div> : f.aiReviewError ? <div className="ai-assessment ai-review-error">
            <strong>AI review unavailable</strong>
            <span>{f.aiReviewError}</span>
            <small>Finding remains a scanner result and is not AI-confirmed.</small>
            <button className="filter-button" disabled={busy === `verify:${f.id}`} onClick={() => onVerify(f.projectId, f.id)}>Retry review</button>
          </div> : <div className="ai-assessment">
            <strong>{['critical', 'high'].includes(f.severity) ? 'AI review queued' : 'Not AI-reviewed'}</strong>
            <span>{['critical', 'high'].includes(f.severity) ? 'Critical/high findings are double-checked automatically in the background.' : 'Lower-severity findings are reviewed on demand to keep scans fast.'}</span>
            <button className="filter-button" disabled={busy === `verify:${f.id}`} onClick={() => onVerify(f.projectId, f.id)}>{busy === `verify:${f.id}` ? 'Reviewing…' : 'Review with AI now'}</button>
          </div>}
        </div>
      </div>
    })}
    {!filtered.length && <div className="empty-state">{findings.length ? 'No findings match the selected filters' : 'No findings reported by completed scans'}</div>}
  </div>
}

function Runs({
  projects,
  selected,
  busy,
  onScan,
  onTest,
  onRunFile,
  onExport,
}: {
  projects: Project[]
  selected: Project | null
  busy: string
  onScan(p: Project): void
  onTest(p: Project): void
  onRunFile(p: Project, target: TestTarget): void
  onExport(format: 'sarif' | 'markdown' | 'json', projectId?: string): void
}) {
  const list = selected ? projects.filter(p => p.id === selected.id) : projects.filter(p => p.latestRun)
  return <div className="detail-panel">
    {list.map(p => <div className="run-card" key={p.id}>
      <div className="run-card-head">
        <div><strong>{p.name}</strong><small>{p.root}</small></div>
        <div className="export-actions">
          {p.latestRun && <>
            <button className="filter-button" onClick={() => onExport('sarif', p.id)} disabled={!!busy}><Download size={13}/> SARIF</button>
            <button className="filter-button" onClick={() => onExport('markdown', p.id)} disabled={!!busy}><Download size={13}/> Report</button>
          </>}
          <button className="primary" onClick={() => onScan(p)} disabled={busy === `scan:${p.id}`}>
            <RefreshCw size={14} className={busy === `scan:${p.id}` ? 'spinning' : ''}/>
            {busy === `scan:${p.id}` ? 'Scanning…' : 'Scan source'}
          </button>
        </div>
      </div>
      {p.latestRun ? <>
        <div className="run-stats">
          <span><b>{p.latestRun.filesScanned}</b> files scanned</span>
          <span><b>{p.latestRun.rulesEvaluated.toLocaleString()}</b> rule evaluations</span>
          <span><b>{p.latestRun.findings.length}</b> findings</span>
          <span>
            <b>{p.latestRun.score}</b> score
            {typeof p.latestRun.scoreDelta === 'number' && p.latestRun.scoreDelta !== 0 && (
              <em className={`delta-badge ${p.latestRun.scoreDelta > 0 ? 'delta-up' : 'delta-down'}`}>
                {p.latestRun.scoreDelta > 0 ? `+${p.latestRun.scoreDelta}` : p.latestRun.scoreDelta}
              </em>
            )}
          </span>
        </div>
        <p className="run-meta">
          Source scan completed {dateLabel(p.latestRun.completedAt)}
          {p.latestRun.durationMs ? ` · ${p.latestRun.durationMs} ms` : ''}
          {p.latestRun.languagesScanned?.length ? ` · Languages: ${p.latestRun.languagesScanned.join(', ')}` : ''}
          {p.latestRun.truncated ? ' · Finding display capped at 1,000' : ''}
        </p>
        {(p.scanHistory || []).length > 1 && <details className="history-details">
          <summary><History size={12}/> Scan history ({p.scanHistory!.length} runs)</summary>
          <div className="history-list">
            {p.scanHistory!.map(entry => <div className="history-row" key={entry.id}>
              <span>{dateLabel(entry.completedAt)}</span>
              <span>{entry.filesScanned} files</span>
              <span>{entry.findingsCount} findings</span>
              <b>Score {entry.score}/100</b>
            </div>)}
          </div>
        </details>}
        <div className="test-action">
          <div>
            <strong>Project tests</strong>
            <span>Each test file and the full suite require separate confirmation.</span>
            <code>{p.latestRun.testCommand || 'No supported test runner detected.'}</code>
          </div>
          {p.latestRun.testCommand && <button className="filter-button" disabled={busy === `test:${p.id}`} onClick={() => onTest(p)}>
            <Play size={14}/>{busy === `test:${p.id}` ? 'Running tests…' : 'Run full suite'}
          </button>}
        </div>
        {(p.latestRun.testFiles || []).length > 0 && <div className="test-file-list">
          <strong>Individual test files ({p.latestRun.testFiles!.length}{p.latestRun.testFilesTruncated ? '+' : ''})</strong>
          {p.latestRun.testFiles!.map(target => {
            const result = p.latestRun!.testResults?.find(item => item.file === target.file)
            const key = `testfile:${p.id}:${target.file}`
            return <div className="test-file-row" key={target.file}>
              <code>{target.file}</code>
              <span>{result ? result.exitCode === 0 ? 'Passed' : `Failed (${result.exitCode})` : target.command}</span>
              <button className="filter-button" disabled={!!busy} onClick={() => onRunFile(p, target)}>{busy === key ? 'Running…' : 'Run file'}</button>
              {result && <details><summary>{result.durationMs ? `${result.durationMs} ms · ` : ''}Output</summary><pre>{result.output || 'No output.'}</pre></details>}
            </div>
          })}
        </div>}
        {p.latestRun.testFilesTruncated && <p className="run-meta">Only the first 200 individually runnable test files are listed.</p>}
        {p.latestRun.testResult && <div className="terminal-output">
          <div>
            <strong>{p.latestRun.testResult.command}</strong>
            <span className={p.latestRun.testResult.exitCode === 0 ? 'test-pass' : 'test-fail'}>{p.latestRun.testResult.exitCode === 0 ? 'Passed' : `Exited ${p.latestRun.testResult.exitCode}`}</span>
          </div>
          <pre>{p.latestRun.testResult.output || 'Test command completed without output.'}</pre>
        </div>}
        {p.latestRun.findings.length > 0 && <div className="run-findings">
          <div className="run-findings-head">
            <h3>Source scan findings · automatic local AI review</h3>
            <button className="filter-button copy-button" onClick={() => void navigator.clipboard.writeText(formatFindings(p.latestRun!.findings.map(f => ({ ...f, projectName: p.name }))))}>Copy findings</button>
          </div>
          {p.latestRun.findings.map(f => <div className="run-finding" key={f.id}>
            <span className={`severity severity-${f.severity}`}>{f.severity}</span>
            <div>
              <strong>{f.title}{f.isNew && <span className="new-badge">NEW</span>}</strong>
              <small>{f.file}:{f.line}{f.ruleId ? ` · ${f.ruleId}` : ''}</small>
              <code>{f.excerpt}</code>
              {f.aiAssessment && <small className="run-ai-result">AI: {f.aiAssessment.verdict === 'likely_real' ? 'Likely true' : f.aiAssessment.verdict === 'likely_false_positive' ? 'Likely false · possible scanner false positive' : 'Not confirmed · 50/50'} · {f.aiAssessment.confidence}%</small>}
              {f.aiReviewError && <small className="run-ai-error">AI review unavailable: {f.aiReviewError}</small>}
            </div>
          </div>)}
        </div>}
      </> : <p className="run-meta">No scan has been run for this project yet.</p>}
    </div>)}
    {!list.length && <div className="empty-state">No scans yet. Add a project and choose Scan source.</div>}
  </div>
}

function Environments({
  projects,
  aiHealth,
  onCheckAi,
  onOpen,
  onTest,
  busy,
}: {
  projects: Project[]
  aiHealth: AiHealth | null
  onCheckAi(): void
  onOpen(p: Project): void
  onTest(p: Project): void
  busy: string
}) {
  return <div className="detail-panel">
    {aiHealth && <div className="environment-row">
      <div className="project-icon icon-web"><ShieldCheck size={17}/></div>
      <div className="environment-main">
        <strong>Local AI review engine · {aiHealth.model}</strong>
        <small>{aiHealth.endpoint}</small>
        <span>{aiHealth.message}</span>
      </div>
      <button className="filter-button" onClick={onCheckAi} disabled={busy === 'refresh'}><RefreshCw size={14}/> Check AI</button>
    </div>}
    {projects.map(p => <div className="environment-row" key={p.id}>
      <div className="project-icon icon-code"><Code2 size={17}/></div>
      <div className="environment-main">
        <strong>{p.name}</strong>
        <small>{p.root}</small>
        <span>{p.latestRun?.testCommand ? `Detected: ${p.latestRun.testCommand}` : 'Run a scan to detect available test scripts'}</span>
      </div>
      <button className="filter-button" onClick={() => onOpen(p)}><Folder size={14}/> Open folder</button>
      {p.latestRun?.testCommand && <button className="filter-button" disabled={busy === `test:${p.id}`} onClick={() => onTest(p)}><Play size={14}/> Run tests</button>}
    </div>)}
    {!projects.length && <div className="empty-state">Add a project to inspect its available test environment</div>}
  </div>
}

function InfoDialog({ onClose }: { onClose(): void }) {
  return <div className="overlay" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
    <section className="dialog">
      <div className="dialog-head">
        <div className="dialog-brand"><img className="dialog-logo" src="./logo.png" alt="OSIEN Probe"/><div><h2>About OSIEN Probe scans</h2><p>Local source checks, AI review, and project tests are separate steps</p></div></div>
        <button className="icon-button" onClick={onClose}><X size={18}/></button>
      </div>
      <div className="help-copy">
        <p><strong>Source scan</strong> walks supported source files, skips common generated/dependency folders, and evaluates the displayed rule set plus multi-language AST checks and same-file taint tracking. It does not run code or claim to prove a project is bug-free.</p>
        <p><strong>Background AI review</strong> double-checks up to 15 critical/high findings after each scan without blocking results. Lower-severity findings can be reviewed on demand. Each local AI request receives only that finding and a small nearby source excerpt. Results are guidance, not proof.</p>
        <p><strong>Run tests</strong> starts a detected project test command only after you confirm. Test scripts can execute arbitrary code, so only run scripts from projects you trust.</p>
        <p>Source code is not uploaded. Review requests go only to local Ollama.</p>
      </div>
      <div className="dialog-foot"><span>DeepCheck · Local analysis</span><button className="primary" onClick={onClose}>Close</button></div>
    </section>
  </div>
}

function formatFindings(findings: (Finding & { projectName?: string; projectId?: string })[]) {
  return findings.map(f => `[${f.severity.toUpperCase()}] ${f.title}${f.cwe ? ` (${f.cwe})` : ''} · confidence ${f.confidence || 'medium'}\nProject: ${f.projectName || ''}\nFile: ${f.file}:${f.line}\nCode: ${f.excerpt}${f.reason ? `\nWhy: ${f.reason}` : ''}${f.hint ? `\nSuggested review: ${f.hint}` : ''}`).join('\n\n')
}
function pageDescription(route: Route) {
  return ({
    Projects: 'Connect folders and inspect source on this device',
    'Test runs': 'Review scans, history, and run detected project test scripts',
    Findings: 'Search, filter, inspect code context, and export findings from latest scans',
    Security: 'High-severity security heuristics from latest scans',
    Environments: 'Local AI review engine status, detected project folders, and test commands',
    Settings: 'Manage local project data and workspace exports',
  })[route]
}
function errorText(e: unknown) { return e instanceof Error ? e.message : String(e) }
export default App
