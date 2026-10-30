import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  AlertCircle, ArrowUpRight, Check, CheckCircle2, ChevronDown, CircleUserRound,
  CloudUpload, Eye, EyeOff, FileImage, FileVideo, Hash,
  Info, KeyRound, LayoutGrid, Link2, LockKeyhole, LogOut, Mail, Menu, MoreHorizontal, Plus, Send, ShieldCheck,
  Sparkles, Trash2, Upload, WandSparkles, X, Zap
} from 'lucide-react';
import { createClient } from '@supabase/supabase-js';
import './styles.css';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;
const apiUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');
const supabase = supabaseUrl && supabaseKey ? createClient(supabaseUrl, supabaseKey) : null;

const Youtube = ({ size = 18 }) => <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor"><path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31 31 0 0 0 0 12a31 31 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31 31 0 0 0 24 12a31 31 0 0 0-.5-5.8ZM9.6 15.6V8.4l6.3 3.6-6.3 3.6Z"/></svg>;
const Instagram = ({ size = 18 }) => <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none"/></svg>;
const Google = ({ size = 18 }) => <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.4Z"/><path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1a5.8 5.8 0 0 1-5.5-4H3.2v2.6A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.5 14a6 6 0 0 1 0-3.9V7.5H3.2a10 10 0 0 0 0 9.1L6.5 14Z"/><path fill="#EA4335" d="M12 6.1c1.5 0 2.8.5 3.8 1.5l2.9-2.8A9.7 9.7 0 0 0 3.2 7.5l3.3 2.6a5.8 5.8 0 0 1 5.5-4Z"/></svg>;

const ACCOUNTS = [
  { id: 'youtube', name: 'YouTube', handle: 'Social Flow Studio', icon: Youtube, tone: 'red', audience: 'Public' },
  { id: 'instagram', name: 'Instagram', handle: '@socialflow', icon: Instagram, tone: 'pink', audience: 'Everyone' },
  { id: 'x', name: 'X', handle: '@socialflowhq', icon: X, tone: 'black', audience: 'Everyone' },
  { id: 'facebook', name: 'Facebook', handle: 'Social Flow', icon: CircleUserRound, tone: 'blue', audience: 'Public' },
];
const LIVE_PLATFORMS = ['youtube', 'x'];
const readDraft = () => {
  try { return JSON.parse(localStorage.getItem('social-flow-draft')) || {}; }
  catch { return {}; }
};

function Brand() {
  return <div className="brand"><div className="brand-mark"><span /><span /><span /></div><b>Social Flow</b></div>;
}

function PlatformIcon({ account, size = 18 }) {
  const Icon = account.icon;
  return <span className={`platform-icon ${account.tone}`}><Icon size={size} strokeWidth={2.2} /></span>;
}

function AuthScreen() {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const submit = async e => {
    e.preventDefault();
    setBusy(true); setError(''); setMessage('');
    if (!supabase) {
      setError('Authentication is not configured on this deployment.');
      setBusy(false); return;
    }
    const result = mode === 'login'
      ? await supabase.auth.signInWithPassword({ email, password })
      : await supabase.auth.signUp({
          email, password,
          options: { emailRedirectTo: window.location.origin }
        });
    if (result.error) setError(result.error.message);
    else if (mode === 'signup' && !result.data.session) {
      setMessage('Check your inbox to confirm your email, then come back to sign in.');
    }
    setBusy(false);
  };

  const signInWithGoogle = async () => {
    setBusy(true); setError(''); setMessage('');
    if (!supabase) {
      setError('Authentication is not configured on this deployment.');
      setBusy(false); return;
    }
    const { error: oauthError } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: window.location.origin,
        queryParams: { prompt: 'select_account' }
      }
    });
    if (oauthError) {
      setError(oauthError.message);
      setBusy(false);
    }
  };

  return <div className="auth-page">
    <div className="auth-glow one" /><div className="auth-glow two" />
    <div className="auth-brand"><Brand /><span>Publish with clarity.</span></div>
    <section className="auth-card">
      <div className="auth-kicker"><Sparkles size={14}/> Your publishing workspace</div>
      <h1>{mode === 'login' ? 'Welcome back.' : 'Start your flow.'}</h1>
      <p>{mode === 'login' ? 'Sign in to create and publish your next post.' : 'Create an account to publish everywhere from one place.'}</p>
      <button className="google-auth-button" type="button" disabled={busy} onClick={signInWithGoogle}><Google/>{mode === 'login' ? 'Continue with Google' : 'Sign up with Google'}</button>
      <div className="auth-divider"><span>or continue with email</span></div>
      <form onSubmit={submit}>
        <label><span>Email address</span><div><Mail size={17}/><input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com"/></div></label>
        <label><span>Password</span><div><LockKeyhole size={17}/><input type="password" required minLength={6} value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 6 characters"/></div></label>
        {error && <div className="auth-alert error">{error}</div>}
        {message && <div className="auth-alert success"><CheckCircle2 size={15}/>{message}</div>}
        <button className="auth-submit" disabled={busy}>{busy ? <><span className="spinner"/> Please wait…</> : mode === 'login' ? 'Sign in to Social Flow' : 'Create my account'}</button>
      </form>
      <div className="auth-switch">{mode === 'login' ? "New to Social Flow?" : 'Already have an account?'} <button onClick={() => {setMode(mode === 'login' ? 'signup' : 'login'); setError(''); setMessage('')}}>{mode === 'login' ? 'Create account' : 'Sign in'}</button></div>
      <small className="auth-secure"><KeyRound size={13}/> Secured by encrypted authentication</small>
    </section>
  </div>;
}

function App() {
  const [session, setSession] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [view, setView] = useState('create');
  const [selected, setSelected] = useState([]);
  const [selectedAccountIds, setSelectedAccountIds] = useState({});
  const [file, setFile] = useState(null);
  const [drag, setDrag] = useState(false);
  const [caption, setCaption] = useState(() => readDraft().caption ?? '');
  const [title, setTitle] = useState(() => readDraft().title ?? '');
  const [hashtags, setHashtags] = useState(() => readDraft().hashtags ?? '');
  const [privacy, setPrivacy] = useState(() => readDraft().privacy ?? 'private');
  const [thumbnail, setThumbnail] = useState(null);
  const [publishing, setPublishing] = useState(false);
  const [results, setResults] = useState({});
  const [aiKey, setAiKey] = useState('');
  const [savedKey, setSavedKey] = useState(false);
  const [showKey, setShowKey] = useState(false);
  const [mobileNav, setMobileNav] = useState(false);
  const [connections, setConnections] = useState([]);
  const [connectionBusy, setConnectionBusy] = useState('');
  const [connectionMessage, setConnectionMessage] = useState('');
  const [helpOpen, setHelpOpen] = useState(false);
  const [accountPickerOpen, setAccountPickerOpen] = useState(false);
  const [draftSaved, setDraftSaved] = useState(true);
  const [systemOnline, setSystemOnline] = useState(null);
  const inputRef = useRef();
  const thumbnailRef = useRef();

  useEffect(() => {
    if (!supabase) { setAuthLoading(false); return; }
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setAuthLoading(false);
    });
    const { data: listener } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
      setAuthLoading(false);
    });
    return () => listener.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (!session || !apiUrl) return;
    const loadConnections = async () => {
      const response = await fetch(`${apiUrl}/api/connections`, {
        headers: { Authorization: `Bearer ${session.access_token}` }
      });
      if (response.ok) {
        const data = await response.json();
        const items = data.connections || [];
        setConnections(items);
        const pickerKey = `social-flow-account-picker:${session.user.id}`;
        if (!sessionStorage.getItem(pickerKey)) setAccountPickerOpen(true);
      }
    };
    loadConnections();
    const params = new URLSearchParams(window.location.search);
    if (params.get('connected')) {
      const platform = params.get('connected');
      setConnectionMessage(`${platform === 'x' ? 'X' : 'YouTube'} connected successfully.`);
      window.history.replaceState({}, '', window.location.pathname);
      setTimeout(loadConnections, 300);
    } else if (params.get('oauth_error')) {
      setConnectionMessage(`Connection failed: ${params.get('oauth_error')}`);
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [session]);

  useEffect(() => {
    if (!apiUrl) { setSystemOnline(false); return; }
    fetch(`${apiUrl}/health`)
      .then(response => setSystemOnline(response.ok))
      .catch(() => setSystemOnline(false));
  }, []);

  useEffect(() => {
    setDraftSaved(false);
    const timer = setTimeout(() => {
      localStorage.setItem('social-flow-draft', JSON.stringify({ title, caption, hashtags, privacy }));
      setDraftSaved(true);
    }, 500);
    return () => clearTimeout(timer);
  }, [title, caption, hashtags, privacy]);

  const toggle = id => {
    const platformConnections = connections.filter(item => item.platform === id);
    if (!LIVE_PLATFORMS.includes(id) || !platformConnections.length) {
      setConnectionMessage(LIVE_PLATFORMS.includes(id)
        ? `Connect ${id === 'x' ? 'X' : 'YouTube'} before selecting it.`
        : `${id === 'instagram' ? 'Instagram' : 'Facebook'} integration is not configured yet.`);
      setView('accounts');
      return;
    }
    if (selected.includes(id)) {
      setSelected(items => items.filter(item => item !== id));
      setSelectedAccountIds(items => {
        const next = { ...items };
        delete next[id];
        return next;
      });
      return;
    }
    if (platformConnections.length > 1) {
      setAccountPickerOpen(true);
      return;
    }
    setSelected(items => [...items, id]);
    setSelectedAccountIds(items => ({ ...items, [id]: platformConnections[0].id }));
  };
  const chooseConnectedAccount = connection => {
    setSelectedAccountIds(items => ({ ...items, [connection.platform]: connection.id }));
    setSelected(items => items.includes(connection.platform) ? items : [...items, connection.platform]);
  };
  const confirmAccountSelection = () => {
    sessionStorage.setItem(`social-flow-account-picker:${session.user.id}`, '1');
    setAccountPickerOpen(false);
  };
  const selectConnectedAccounts = () => {
    if (selected.length === connectedLiveAccounts.length) {
      setSelected([]);
      setSelectedAccountIds({});
      return;
    }
    const ids = {};
    connectedLiveAccounts.forEach(account => {
      ids[account.id] = selectedAccountIds[account.id]
        || connections.find(connection => connection.platform === account.id)?.id;
    });
    setSelected(connectedLiveAccounts.map(account => account.id));
    setSelectedAccountIds(ids);
  };
  const loadFile = f => f && setFile({ raw: f, name: f.name, size: `${(f.size / 1024 / 1024).toFixed(1)} MB`, type: f.type, url: f.type.startsWith('image') ? URL.createObjectURL(f) : null });
  const improve = () => {
    if (!savedKey) return;
    setCaption("Give your best ideas the space they deserve. Quiet the noise, stay curious, and let meaningful work take root. 🌿");
    setHashtags('#CreativeGrowth #MindfulWork #IdeasThatMatter #StayCurious');
    setTitle('Give your best ideas room to grow');
  };
  const connectPlatform = async platform => {
    if (!apiUrl) {
      setConnectionMessage('The secure API is not configured yet.');
      return;
    }
    setConnectionBusy(platform);
    setConnectionMessage('');
    try {
      const response = await fetch(`${apiUrl}/api/oauth/${platform}/start`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${session.access_token}` }
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Unable to start connection');
      window.location.assign(data.url);
    } catch (error) {
      setConnectionMessage(error.message);
      setConnectionBusy('');
    }
  };
  const disconnectPlatform = async connection => {
    if (!apiUrl || connectionBusy) return;
    setConnectionBusy(connection.platform);
    setConnectionMessage('');
    try {
      const response = await fetch(`${apiUrl}/api/connections/${connection.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${session.access_token}` }
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Unable to disconnect account');
      setConnections(items => items.filter(item => item.id !== connection.id));
      setSelected(items => items.filter(id => id !== connection.platform));
      setSelectedAccountIds(items => {
        const next = { ...items };
        if (next[connection.platform] === connection.id) delete next[connection.platform];
        return next;
      });
      setConnectionMessage(`${connection.platform === 'x' ? 'X' : 'YouTube'} disconnected.`);
    } catch (error) {
      setConnectionMessage(error.message);
    } finally {
      setConnectionBusy('');
    }
  };
  const pollYouTubeJob = async jobId => {
    while (true) {
      await new Promise(resolve => setTimeout(resolve, 1200));
      const response = await fetch(`${apiUrl}/api/youtube/uploads/${jobId}`, {
        headers: { Authorization: `Bearer ${session.access_token}` }
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Unable to read YouTube upload status');
      const job = data.job;
      setResults(current => ({
        ...current,
        youtube: {
          progress: Math.max(30, Math.round(30 + job.progress * .7)),
          state: job.state === 'completed' ? 'published' : job.state,
          message: job.message,
          url: job.url,
          warning: job.warning
        }
      }));
      if (job.state === 'completed') return job;
      if (job.state === 'failed') throw new Error(job.message || 'YouTube upload failed');
    }
  };
  const uploadToYouTube = () => new Promise((resolve, reject) => {
    const form = new FormData();
    form.append('video', file.raw);
    if (thumbnail?.raw) form.append('thumbnail', thumbnail.raw);
    form.append('title', title);
    form.append('description', `${caption}\n\n${hashtags}`.trim());
    form.append('tags', hashtags);
    form.append('privacy', privacy);
    form.append('connection_id', selectedAccountIds.youtube || '');
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${apiUrl}/api/youtube/uploads`);
    xhr.setRequestHeader('Authorization', `Bearer ${session.access_token}`);
    xhr.upload.onprogress = event => {
      if (!event.lengthComputable) return;
      const progress = Math.max(2, Math.round((event.loaded / event.total) * 28));
      setResults(current => ({ ...current, youtube: { progress, state: 'uploading', message: 'Sending video securely' } }));
    };
    xhr.onerror = () => reject(new Error('The video could not reach the Social Flow API'));
    xhr.onload = async () => {
      let data = {};
      try { data = JSON.parse(xhr.responseText); } catch {}
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(data.error || 'YouTube upload could not be started'));
        return;
      }
      try { resolve(await pollYouTubeJob(data.job.id)); } catch (error) { reject(error); }
    };
    xhr.send(form);
  });
  const pollXJob = async jobId => {
    while (true) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      const response = await fetch(`${apiUrl}/api/x/posts/${jobId}`, {
        headers: { Authorization: `Bearer ${session.access_token}` }
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Unable to read X publishing status');
      const job = data.job;
      setResults(current => ({
        ...current,
        x: {
          progress: Math.max(30, Math.round(30 + job.progress * .7)),
          state: job.state === 'completed' ? 'published' : job.state,
          message: job.message,
          url: job.url
        }
      }));
      if (job.state === 'completed') return job;
      if (job.state === 'failed') throw new Error(job.message || 'X publishing failed');
    }
  };
  const uploadToX = () => new Promise((resolve, reject) => {
    const form = new FormData();
    if (file?.raw) form.append('media', file.raw);
    form.append('text', `${caption}\n\n${hashtags}`.trim());
    form.append('connection_id', selectedAccountIds.x || '');
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${apiUrl}/api/x/posts`);
    xhr.setRequestHeader('Authorization', `Bearer ${session.access_token}`);
    xhr.upload.onprogress = event => {
      if (!event.lengthComputable) return;
      const progress = Math.max(2, Math.round((event.loaded / event.total) * 28));
      setResults(current => ({ ...current, x: { progress, state: 'uploading', message: 'Sending post securely' } }));
    };
    xhr.onerror = () => reject(new Error('The post could not reach the Social Flow API'));
    xhr.onload = async () => {
      let data = {};
      try { data = JSON.parse(xhr.responseText); } catch {}
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(data.error || 'X publishing could not be started'));
        return;
      }
      try { resolve(await pollXJob(data.job.id)); } catch (error) { reject(error); }
    };
    xhr.send(form);
  });
  const publishEverywhere = async () => {
    setPublishing(true);
    const supported = ['youtube', 'x'];
    const initial = Object.fromEntries(selected.map(id => [id, supported.includes(id)
      ? { progress: 1, state: 'uploading', message: 'Starting publish' }
      : { progress: 0, state: 'failed', message: 'Publishing integration is not available yet' }]));
    setResults(initial);
    try {
      if (!apiUrl) throw new Error('The secure API is not configured');
      const tasks = [];
      if (selected.includes('youtube')) {
        tasks.push((async () => {
          try {
            if (!file?.raw || !file.type.startsWith('video/')) throw new Error('YouTube publishing requires a video file');
            if (!selectedAccountIds.youtube) throw new Error('Select the YouTube channel that should receive this video');
            await uploadToYouTube();
          } catch (error) {
            setResults(current => ({ ...current, youtube: { ...(current.youtube || {}), state: 'failed', message: error.message } }));
          }
        })());
      }
      if (selected.includes('x')) {
        tasks.push((async () => {
          try {
            const xText = `${caption}\n\n${hashtags}`.trim();
            if (!xText) throw new Error('Write some text before publishing to X');
            if (Array.from(xText).length > 280) throw new Error(`X post is ${Array.from(xText).length} characters; the limit is 280`);
            if (!selectedAccountIds.x) throw new Error('Select the X account that should publish this post');
            await uploadToX();
          } catch (error) {
            setResults(current => ({ ...current, x: { ...(current.x || {}), state: 'failed', message: error.message } }));
          }
        })());
      }
      if (!tasks.length) throw new Error('Select YouTube or X to publish');
      await Promise.all(tasks);
    } catch (error) {
      setResults(current => Object.fromEntries(Object.entries(current).map(([id, result]) => [
        id,
        supported.includes(id) && result.state !== 'published'
          ? { ...result, state: 'failed', message: error.message }
          : result
      ])));
    } finally {
      setPublishing(false);
    }
  };

  const nav = [
    { id: 'create', label: 'Create post', icon: Plus },
    { id: 'accounts', label: 'Accounts', icon: LayoutGrid },
  ];

  if (authLoading) return <div className="auth-loader"><div className="brand-mark"><span/><span/><span/></div><div className="spinner"/></div>;
  if (!session) return <AuthScreen />;

  const userEmail = session.user.email || 'Signed in user';
  const userInitial = userEmail.charAt(0).toUpperCase();
  const connectedLiveAccounts = ACCOUNTS.filter(account =>
    LIVE_PLATFORMS.includes(account.id) && connections.some(connection => connection.platform === account.id)
  );
  const selectedConnectionFor = platform =>
    connections.find(connection => connection.id === selectedAccountIds[platform]);
  const xPostLength = Array.from(`${caption}\n\n${hashtags}`.trim()).length;
  const signOut = async () => {
    sessionStorage.removeItem(`social-flow-account-picker:${session.user.id}`);
    await supabase?.auth.signOut();
  };

  return (
    <div className="app-shell">
      <aside className={mobileNav ? 'sidebar open' : 'sidebar'}>
        <div className="side-top"><Brand /><button className="close-nav" onClick={() => setMobileNav(false)}><X size={19}/></button></div>
        <nav>
          <p className="nav-label">Workspace</p>
          {nav.map(item => <button key={item.id} className={view === item.id ? 'nav-item active' : 'nav-item'} onClick={() => { setView(item.id); setMobileNav(false); }}><item.icon size={18}/><span>{item.label}</span>{item.id === 'accounts' && <em>{connections.length}</em>}</button>)}
        </nav>
        <div className="sidebar-foot">
          <div className="pro-card"><div className="mini-spark"><Zap size={15}/></div><b>Posting made simple.</b><p>Four platforms. One calm workspace.</p></div>
          <button className="user-row" title="Sign out" onClick={signOut}><span className="avatar">{userInitial}</span><span><b>{userEmail.split('@')[0]}</b><small>{userEmail}</small></span><LogOut size={17}/></button>
        </div>
      </aside>
      {mobileNav && <div className="scrim" onClick={() => setMobileNav(false)} />}

      <main>
        <header className="topbar">
          <button className="menu-button" onClick={() => setMobileNav(true)}><Menu size={21}/></button>
          <div><span className="eyebrow">SOCIAL FLOW</span><h1>{view === 'create' ? 'Create post' : view === 'accounts' ? 'Connected accounts' : 'AI settings'}</h1></div>
          <div className="top-actions"><span className={systemOnline === false ? 'status-dot offline' : 'status-dot'}><i /> {systemOnline === null ? 'Checking services…' : systemOnline ? 'YouTube & X ready' : 'Publishing API offline'}</span><button className="help" aria-label="Open help" onClick={() => setHelpOpen(true)}>?</button></div>
        </header>

        {view === 'create' && (
          <div className="page create-page">
            <div className="intro"><div><h2>Share something <em>worth seeing.</em></h2><p>Create once, publish to every connected destination.</p></div><span className={draftSaved ? 'draft-state saved' : 'draft-state'}>{draftSaved ? <Check size={14}/> : <span className="mini-spinner"/>} {draftSaved ? 'Draft saved locally' : 'Saving draft…'}</span></div>
            <div className="composer-grid">
              <section className="card compose-card">
                <div className="section-head"><span className="step">1</span><div><h3>Choose connected accounts</h3><p>Select the exact account that should publish</p></div>{connectedLiveAccounts.length > 0 && <button className="text-button" onClick={selectConnectedAccounts}>{selected.length === connectedLiveAccounts.length ? 'Clear' : 'Select connected'}</button>}</div>
                <div className="account-grid">
                  {ACCOUNTS.map(a => {
                    const connection = connections.find(item => item.platform === a.id);
                    const live = LIVE_PLATFORMS.includes(a.id);
                    const chosen = selectedConnectionFor(a.id);
                    return <button key={a.id} className={`${selected.includes(a.id) ? 'account-option selected' : 'account-option'} ${!connection || !live ? 'unavailable' : ''}`} onClick={() => toggle(a.id)}><PlatformIcon account={a}/><span><b>{a.name}</b><small>{chosen?.account_name || connection?.account_name || (live ? 'Connect account first' : 'Coming later')}</small></span>{connection && live ? <i className="check">{selected.includes(a.id) && <Check size={13}/>}</i> : <i className="lock-state">{live ? <Link2 size={13}/> : 'Soon'}</i>}</button>
                  })}
                </div>

                <div className="divider" />
                <div className="section-head"><span className="step">2</span><div><h3>Add your content</h3><p>{selected.includes('youtube') ? 'YouTube requires a video' : selected.includes('x') ? 'Media is optional for X' : 'Select a destination to see its requirements'}</p></div></div>
                {!file ? <div className={drag ? 'dropzone dragging' : 'dropzone'} onDragOver={e => {e.preventDefault(); setDrag(true)}} onDragLeave={() => setDrag(false)} onDrop={e => {e.preventDefault(); setDrag(false); loadFile(e.dataTransfer.files[0])}} onClick={() => inputRef.current.click()}>
                  <input ref={inputRef} type="file" accept="image/*,video/*" hidden onChange={e => loadFile(e.target.files[0])}/>
                  <div className="upload-icon"><CloudUpload size={25}/></div><b>Drop your media here</b><p>or <span>browse your files</span></p><small>{selected.includes('x') && !selected.includes('youtube') ? 'X: images 5 MB · GIFs 15 MB · videos 512 MB' : 'YouTube: video up to 2 GB in Social Flow'}</small>
                </div> : <div className="file-preview">
                  <div className="preview-thumb">{file.url ? <img src={file.url} alt="Upload preview"/> : <FileVideo size={30}/>}</div>
                  <div><b>{file.name}</b><small>{file.size} · Ready to publish</small></div><span className="ready"><Check size={13}/> Ready</span><button onClick={() => setFile(null)}><Trash2 size={17}/></button>
                </div>}

                <div className="divider" />
                <div className="section-head"><span className="step">3</span><div><h3>Polish your post</h3><p>Fine-tune the details</p></div>{savedKey && <button className="ai-button" onClick={improve}><WandSparkles size={15}/> Improve with AI</button>}</div>
                {(selected.includes('youtube') || selected.length === 0) && <label className="field"><span>Title <small>YouTube</small></span><input value={title} onChange={e => setTitle(e.target.value)} maxLength={100}/><i>{title.length}/100</i></label>}
                <label className="field"><span>Caption / description</span><textarea value={caption} onChange={e => setCaption(e.target.value)} maxLength={2200}/><i>{caption.length}/2,200</i></label>
                <label className="field hashtag-field"><span>Hashtags</span><Hash size={16}/><input value={hashtags.replaceAll('#','')} onChange={e => setHashtags(e.target.value.split(' ').map(x => x ? `#${x.replace('#','')}` : '').join(' '))}/></label>
                {selected.includes('x') && <div className={xPostLength > 280 ? 'platform-limit over' : 'platform-limit'}><X size={13}/><span>X post length</span><b>{xPostLength}/280</b></div>}
                {selected.includes('youtube') && <div className="settings-row"><label className="setting-control"><Eye size={16}/><span><small>YouTube audience</small><select value={privacy} onChange={e => setPrivacy(e.target.value)}><option value="private">Private</option><option value="unlisted">Unlisted</option><option value="public">Public</option></select></span><ChevronDown size={15}/></label><button onClick={() => thumbnailRef.current.click()}><FileImage size={16}/><span><small>Custom thumbnail</small><b>{thumbnail ? thumbnail.name : 'Auto-generated'}</b></span><ChevronDown size={15}/></button><input ref={thumbnailRef} type="file" accept="image/jpeg,image/png" hidden onChange={e => {const f=e.target.files[0]; if(f)setThumbnail({raw:f,name:f.name})}}/></div>}
              </section>

              <aside className="publish-panel">
                <div className="summary card"><div className="summary-head"><div><h3>Ready to publish?</h3><p>{selected.length} destination{selected.length !== 1 ? 's' : ''} selected</p></div><Send size={20}/></div>
                  <div className="destination-list">{selected.length ? ACCOUNTS.filter(a => selected.includes(a.id)).map(a => {
                    const r = results[a.id];
                    const chosen = selectedConnectionFor(a.id);
                    return <React.Fragment key={a.id}><div className="destination"><PlatformIcon account={a} size={15}/><div><b>{chosen?.account_name || a.name}</b><small>{a.name}</small>{r && <span className="progress-track"><i className={r.state === 'failed' ? 'failed' : ''} style={{width: `${r.state === 'failed' ? 100 : r.progress}%`}} /></span>}</div><small title={r?.message || ''} className={r?.state || ''}>{r ? (r.state === 'published' ? 'Published' : r.state === 'failed' ? 'Failed' : r.state === 'processing' ? 'Processing' : `${r.progress}%`) : 'Ready'}</small></div>{r?.url && <a className="result-link" href={r.url} target="_blank" rel="noreferrer">View on {a.name} <ArrowUpRight size={12}/></a>}{r?.warning && <p className="result-warning">{r.warning}</p>}{r?.state === 'failed' && <p className="result-error">{r.message}</p>}</React.Fragment>
                  }) : <div className="empty-state">Choose at least one destination</div>}</div>
                  <button className="publish-button" disabled={!selected.length || publishing || (!file && !selected.includes('x'))} onClick={publishEverywhere}>{publishing ? <><span className="spinner"/> Publishing…</> : <><Upload size={17}/> Publish selected</>}</button>
                  <p className="secure-note"><ShieldCheck size={13}/> OAuth-secured publishing to each platform</p>
                </div>
                <div className="tip-card"><ShieldCheck size={16}/><div><b>Direct publishing</b><p>Every post is sent only after you click Publish. No scheduling or background automation.</p></div></div>
              </aside>
            </div>
          </div>
        )}

        {view === 'accounts' && <div className="page narrow-page"><div className="settings-intro"><span className="page-icon"><LayoutGrid/></span><h2>Your connected accounts</h2><p>Every account below belongs only to <b>{userEmail}</b>.</p></div>{connectionMessage && <div className={connectionMessage.includes('successfully') || connectionMessage.includes('disconnected') ? 'connection-banner success' : 'connection-banner'}>{connectionMessage}</div>}<div className="account-list card">
          {connections.map(connection => {
            const account = ACCOUNTS.find(item => item.id === connection.platform);
            if (!account) return null;
            return <div className="account-row" key={connection.id}><PlatformIcon account={account} size={20}/><div><b>{connection.account_name}</b><small>{account.name} · Connected to this Social Flow login</small></div><span className="connected"><i/> Connected</span><button className="danger-action" disabled={connectionBusy === connection.platform} onClick={() => disconnectPlatform(connection)}>{connectionBusy === connection.platform ? 'Working…' : 'Disconnect'}</button></div>
          })}
          {LIVE_PLATFORMS.filter(platform => !connections.some(connection => connection.platform === platform)).map(platform => {
            const account = ACCOUNTS.find(item => item.id === platform);
            return <div className="account-row" key={platform}><PlatformIcon account={account} size={20}/><div><b>{account.name}</b><small>Connect another publishing account</small></div><span className="not-connected">Not connected</span><button disabled={connectionBusy === platform} onClick={() => connectPlatform(platform)}>{connectionBusy === platform ? 'Opening…' : 'Connect'}</button></div>
          })}
          {LIVE_PLATFORMS.filter(platform => connections.some(connection => connection.platform === platform)).map(platform => {
            const account = ACCOUNTS.find(item => item.id === platform);
            return <button className="connect-new" key={`more-${platform}`} disabled={!!connectionBusy} onClick={() => connectPlatform(platform)}><Plus size={16}/> Connect another {account.name} account</button>
          })}
          {ACCOUNTS.filter(account => !LIVE_PLATFORMS.includes(account.id)).map(account => <div className="account-row disabled-row" key={account.id}><PlatformIcon account={account} size={20}/><div><b>{account.name}</b><small>Integration coming later</small></div><span className="not-connected">Unavailable</span><button disabled>Coming later</button></div>)}
        </div><div className="notice"><KeyRound size={17}/><p><b>User-scoped connections</b><br/>Accounts are stored against your signed-in user ID. Other Social Flow users cannot see or publish to them.</p></div></div>}

        {view === 'ai' && <div className="page narrow-page"><div className="settings-intro"><span className="page-icon ai"><Sparkles/></span><span className={savedKey ? 'ai-status on' : 'ai-status'}><i/>{savedKey ? 'AI enabled' : 'AI disabled'}</span><h2>Optional AI assistance</h2><p>Bring your own API key for better captions, titles, and hashtags. Social Flow works perfectly without it.</p></div><div className="ai-settings card"><label className="field"><span>Provider</span><div className="select-look">OpenAI compatible <ChevronDown size={16}/></div></label><label className="field"><span>API key</span><div className="key-input"><KeyRound size={16}/><input type={showKey ? 'text' : 'password'} placeholder="Paste your API key" value={aiKey} onChange={e => setAiKey(e.target.value)}/><button onClick={() => setShowKey(!showKey)}>{showKey ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div></label><p className="privacy-copy">Your key is encrypted before storage and is only used for requests you initiate.</p><button className="save-button" disabled={!aiKey} onClick={() => setSavedKey(true)}>{savedKey ? <><Check size={16}/> Settings saved</> : 'Save AI settings'}</button></div><div className="capabilities"><p>When enabled, AI can help with</p><div><span><Check/> Caption improvement</span><span><Check/> Hashtag suggestions</span><span><Check/> Title suggestions</span><span><Check/> Grammar correction</span></div></div></div>}
      </main>
      {accountPickerOpen && <div className="modal-backdrop"><section className="help-modal account-picker"><span className="page-icon"><CircleUserRound size={21}/></span><h2>Choose publishing accounts</h2><p>Signed in as <b>{userEmail}</b>. Choose one connected account per platform. We’ll ask again after your next sign-in.</p>
        <div className="picker-groups">{LIVE_PLATFORMS.map(platform => {
          const account = ACCOUNTS.find(item => item.id === platform);
          const options = connections.filter(connection => connection.platform === platform);
          return <div className="picker-group" key={platform}><div className="picker-label"><PlatformIcon account={account} size={15}/><span>{account.name}</span><small>{options.length} connected</small></div>{options.length ? options.map(connection => <button key={connection.id} className={selectedAccountIds[platform] === connection.id ? 'picker-account selected' : 'picker-account'} onClick={() => chooseConnectedAccount(connection)}><span className="avatar small">{connection.account_name.charAt(0).toUpperCase()}</span><span><b>{connection.account_name}</b><small>{account.name} account</small></span><i>{selectedAccountIds[platform] === connection.id && <Check size={14}/>}</i></button>) : <button className="picker-empty" onClick={() => {setAccountPickerOpen(false); setView('accounts')}}><Plus size={15}/> Connect {account.name}</button>}</div>
        })}</div>
        <div className="picker-actions"><button className="secondary-button" onClick={() => {sessionStorage.setItem(`social-flow-account-picker:${session.user.id}`, '1'); setAccountPickerOpen(false)}}>Not now</button><button className="publish-button" disabled={!selected.length} onClick={confirmAccountSelection}><Check size={16}/> Use {selected.length} account{selected.length === 1 ? '' : 's'}</button></div>
      </section></div>}
      {helpOpen && <div className="modal-backdrop" onClick={() => setHelpOpen(false)}><section className="help-modal" onClick={e => e.stopPropagation()}><button className="modal-close" onClick={() => setHelpOpen(false)}><X size={18}/></button><span className="page-icon"><Info size={21}/></span><h2>How Social Flow works</h2><p>Connect an account, select its destination, prepare platform-specific content, then publish. Each platform reports progress and errors independently.</p><div className="help-grid"><div><Youtube size={18}/><span><b>YouTube</b><small>Video, title, description, privacy and optional thumbnail.</small></span></div><div><X size={18}/><span><b>X</b><small>Text, image, GIF or video. Keep text within 280 characters.</small></span></div><div className="muted"><AlertCircle size={18}/><span><b>Instagram & Facebook</b><small>Visible for roadmap clarity, but unavailable until Meta is configured.</small></span></div></div><button className="publish-button" onClick={() => setHelpOpen(false)}>Got it</button></section></div>}
    </div>
  );
}

createRoot(document.getElementById('root')).render(<App />);
