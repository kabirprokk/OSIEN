import 'dotenv/config';
import crypto from 'node:crypto';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import multer from 'multer';
import os from 'node:os';
import fs from 'node:fs/promises';
import path from 'node:path';
import { createClient } from '@supabase/supabase-js';
import { decryptJson, encryptJson, signState, verifyState } from './crypto.js';
import { exchangeGoogleCode, getYouTubeChannel, youtubeAuthorizationUrl } from './google.js';
import { uploadThumbnail, uploadVideoResumable, validAccessToken, waitForVideoProcessing } from './youtube-upload.js';
import { createPkcePair, exchangeXCode, getXUser, xAuthorizationUrl } from './x.js';
import { createXPost, uploadXMedia, validXAccessToken } from './x-publish.js';

const required = [
  'FRONTEND_URL', 'SUPABASE_URL', 'SUPABASE_SECRET_KEY',
  'TOKEN_ENCRYPTION_KEY', 'STATE_SIGNING_SECRET'
];
const missing = required.filter(name => !process.env[name]);
if (missing.length) throw new Error(`Missing environment variables: ${missing.join(', ')}`);

const app = express();
const port = Number(process.env.PORT || 10000);
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SECRET_KEY, {
  auth: { persistSession: false, autoRefreshToken: false }
});
const uploadJobs = new Map();
const upload = multer({
  dest: path.join(os.tmpdir(), 'social-flow-uploads'),
  limits: { fileSize: 2 * 1024 * 1024 * 1024, files: 2 }
});

app.set('trust proxy', 1);
app.use(helmet({ crossOriginResourcePolicy: false }));
app.use(cors({ origin: process.env.FRONTEND_URL, credentials: false }));
app.use(express.json({ limit: '1mb' }));

async function requireUser(req, res, next) {
  const token = req.headers.authorization?.replace(/^Bearer\s+/i, '');
  if (!token) return res.status(401).json({ error: 'Authentication required' });
  const { data, error } = await supabase.auth.getUser(token);
  if (error || !data.user) return res.status(401).json({ error: 'Invalid or expired session' });
  req.user = data.user;
  next();
}

app.get('/', (_req, res) => res.json({
  ok: true,
  service: 'social-flow-api',
  message: 'Social Flow API is running. Open the frontend application to use Social Flow.'
}));
app.get('/health', (_req, res) => res.json({ ok: true, service: 'social-flow-api' }));

app.get('/api/connections', requireUser, async (req, res) => {
  const { data, error } = await supabase
    .from('platform_connections')
    .select('id, platform, platform_account_id, account_name, avatar_url, created_at, updated_at')
    .eq('user_id', req.user.id);
  if (error) return res.status(500).json({ error: 'Unable to load connected accounts' });
  res.json({ connections: data });
});

app.delete('/api/connections/:id', requireUser, async (req, res) => {
  const { data, error } = await supabase
    .from('platform_connections')
    .delete()
    .eq('id', req.params.id)
    .eq('user_id', req.user.id)
    .select('id, platform')
    .maybeSingle();
  if (error) return res.status(500).json({ error: 'Unable to disconnect this account' });
  if (!data) return res.status(404).json({ error: 'Connected account not found' });
  res.json({ disconnected: data });
});

async function removeUploadFiles(files) {
  const paths = Object.values(files || {}).flat().map(file => file.path);
  await Promise.all(paths.map(filePath => fs.unlink(filePath).catch(() => {})));
}

async function processYouTubeUpload(job, connection, files, fields) {
  try {
    job.state = 'preparing';
    job.message = 'Preparing secure YouTube upload';
    const accessToken = await validAccessToken(supabase, connection);
    job.state = 'uploading';
    job.message = 'Uploading to YouTube';
    const video = await uploadVideoResumable({
      accessToken,
      file: files.video[0],
      metadata: {
        title: fields.title,
        description: fields.description || '',
        tags: String(fields.tags || '').split(/[,\s#]+/).filter(Boolean).slice(0, 30),
        privacy: ['public', 'private', 'unlisted'].includes(fields.privacy) ? fields.privacy : 'private'
      },
      onProgress: progress => { job.progress = progress; }
    });
    job.videoId = video.id;
    job.url = `https://www.youtube.com/watch?v=${video.id}`;
    if (files.thumbnail?.[0]) {
      job.state = 'processing';
      job.message = 'Setting custom thumbnail';
      try {
        await uploadThumbnail(accessToken, video.id, files.thumbnail[0]);
      } catch (error) {
        console.warn('YouTube thumbnail skipped:', error.message);
        job.warning = `Video published, but YouTube rejected the custom thumbnail: ${error.message}`;
      }
    }
    job.state = 'processing';
    job.progress = 99;
    job.message = 'Processing on YouTube';
    const processing = await waitForVideoProcessing(accessToken, video.id, status => {
      job.state = 'processing';
      job.progress = status.percent ?? 99;
      job.message = status.percent
        ? `Processing on YouTube (${status.percent}%)`
        : 'Processing on YouTube';
    });
    if (!processing.processed) {
      job.warning = [job.warning, 'YouTube is still processing this video. Check YouTube Studio for final status.']
        .filter(Boolean).join(' ');
    }
    job.state = 'completed';
    job.progress = 100;
    job.message = job.warning || 'Published to YouTube';
    job.completedAt = Date.now();
  } catch (error) {
    console.error('YouTube upload failed:', error.message);
    job.state = 'failed';
    job.message = error.message;
    job.completedAt = Date.now();
  } finally {
    await removeUploadFiles(files);
  }
}

app.post('/api/youtube/uploads', requireUser, upload.fields([
  { name: 'video', maxCount: 1 },
  { name: 'thumbnail', maxCount: 1 }
]), async (req, res) => {
  const video = req.files?.video?.[0];
  if (!video) return res.status(400).json({ error: 'A video file is required' });
  if (!video.mimetype.startsWith('video/')) {
    await removeUploadFiles(req.files);
    return res.status(400).json({ error: 'YouTube publishing currently requires a video file' });
  }
  if (!String(req.body.title || '').trim()) {
    await removeUploadFiles(req.files);
    return res.status(400).json({ error: 'A YouTube title is required' });
  }
  if (!String(req.body.connection_id || '').trim()) {
    await removeUploadFiles(req.files);
    return res.status(400).json({ error: 'Select the YouTube account that should receive this video' });
  }
  const thumbnail = req.files?.thumbnail?.[0];
  if (thumbnail && (!thumbnail.mimetype.startsWith('image/') || thumbnail.size > 2 * 1024 * 1024)) {
    await removeUploadFiles(req.files);
    return res.status(400).json({ error: 'Thumbnail must be an image no larger than 2 MB' });
  }

  const { data: connection, error } = await supabase
    .from('platform_connections')
    .select('*')
    .eq('id', req.body.connection_id)
    .eq('user_id', req.user.id)
    .eq('platform', 'youtube')
    .maybeSingle();
  if (error || !connection) {
    await removeUploadFiles(req.files);
    return res.status(409).json({ error: 'Connect a YouTube channel before publishing' });
  }

  const id = crypto.randomUUID();
  const job = {
    id, userId: req.user.id, platform: 'youtube',
    state: 'queued', progress: 0, message: 'Upload queued', createdAt: Date.now()
  };
  uploadJobs.set(id, job);
  res.status(202).json({ job });
  void processYouTubeUpload(job, connection, req.files, req.body);
});

app.get('/api/youtube/uploads/:id', requireUser, (req, res) => {
  const job = uploadJobs.get(req.params.id);
  if (!job || job.userId !== req.user.id) return res.status(404).json({ error: 'Upload job not found' });
  res.json({ job });
});

async function processXPost(job, connection, file, text) {
  try {
    job.state = 'preparing';
    job.message = 'Preparing X post';
    const accessToken = await validXAccessToken(supabase, connection);
    let mediaId = null;
    if (file) {
      job.state = 'uploading';
      job.message = 'Uploading media to X';
      mediaId = await uploadXMedia(accessToken, file, progress => { job.progress = progress; });
    }
    job.state = 'publishing';
    job.progress = 99;
    job.message = 'Creating post on X';
    const post = await createXPost(accessToken, text, mediaId);
    job.state = 'completed';
    job.progress = 100;
    job.message = 'Published to X';
    job.postId = post.id;
    job.url = `https://x.com/i/status/${post.id}`;
    job.completedAt = Date.now();
  } catch (error) {
    console.error('X publishing failed:', error.message);
    job.state = 'failed';
    job.message = error.message;
    job.completedAt = Date.now();
  } finally {
    if (file?.path) await fs.unlink(file.path).catch(() => {});
  }
}

app.post('/api/x/posts', requireUser, upload.single('media'), async (req, res) => {
  const text = String(req.body.text || '').trim();
  if (!text) {
    if (req.file?.path) await fs.unlink(req.file.path).catch(() => {});
    return res.status(400).json({ error: 'Post text is required' });
  }
  if (!String(req.body.connection_id || '').trim()) {
    if (req.file?.path) await fs.unlink(req.file.path).catch(() => {});
    return res.status(400).json({ error: 'Select the X account that should publish this post' });
  }
  if (Array.from(text).length > 280) {
    if (req.file?.path) await fs.unlink(req.file.path).catch(() => {});
    return res.status(400).json({ error: 'X post text must be 280 characters or fewer' });
  }
  if (req.file) {
    const isImage = req.file.mimetype.startsWith('image/');
    const isVideo = req.file.mimetype.startsWith('video/');
    const maxSize = isVideo ? 512 * 1024 * 1024 : req.file.mimetype === 'image/gif' ? 15 * 1024 * 1024 : 5 * 1024 * 1024;
    if ((!isImage && !isVideo) || req.file.size > maxSize) {
      await fs.unlink(req.file.path).catch(() => {});
      return res.status(400).json({ error: `Unsupported X media file or size. Maximum allowed: ${Math.round(maxSize / 1024 / 1024)} MB` });
    }
  }

  const { data: connection, error } = await supabase
    .from('platform_connections')
    .select('*')
    .eq('id', req.body.connection_id)
    .eq('user_id', req.user.id)
    .eq('platform', 'x')
    .maybeSingle();
  if (error || !connection) {
    if (req.file?.path) await fs.unlink(req.file.path).catch(() => {});
    return res.status(409).json({ error: 'Connect an X account before publishing' });
  }

  const id = crypto.randomUUID();
  const job = {
    id, userId: req.user.id, platform: 'x',
    state: 'queued', progress: 0, message: 'Post queued', createdAt: Date.now()
  };
  uploadJobs.set(id, job);
  res.status(202).json({ job });
  void processXPost(job, connection, req.file, text);
});

app.get('/api/x/posts/:id', requireUser, (req, res) => {
  const job = uploadJobs.get(req.params.id);
  if (!job || job.userId !== req.user.id || job.platform !== 'x') {
    return res.status(404).json({ error: 'X publishing job not found' });
  }
  res.json({ job });
});

setInterval(() => {
  const cutoff = Date.now() - 60 * 60 * 1000;
  for (const [id, job] of uploadJobs) {
    if (job.completedAt && job.completedAt < cutoff) uploadJobs.delete(id);
  }
}, 10 * 60 * 1000).unref();

app.post('/api/oauth/youtube/start', requireUser, (req, res) => {
  if (!process.env.GOOGLE_CLIENT_ID || !process.env.GOOGLE_CLIENT_SECRET || !process.env.GOOGLE_REDIRECT_URI) {
    return res.status(503).json({ error: 'YouTube OAuth is not configured yet' });
  }
  const state = signState({
    userId: req.user.id,
    nonce: crypto.randomUUID(),
    exp: Date.now() + 10 * 60 * 1000
  });
  res.json({ url: youtubeAuthorizationUrl(state) });
});

app.get('/api/oauth/youtube/callback', async (req, res) => {
  const returnUrl = new URL(process.env.FRONTEND_URL);
  try {
    if (!process.env.GOOGLE_CLIENT_ID || !process.env.GOOGLE_CLIENT_SECRET || !process.env.GOOGLE_REDIRECT_URI) {
      throw new Error('YouTube OAuth is not configured yet');
    }
    if (req.query.error) throw new Error(String(req.query.error_description || req.query.error));
    const state = verifyState(req.query.state);
    const tokens = await exchangeGoogleCode(String(req.query.code || ''));
    const channel = await getYouTubeChannel(tokens.access_token);
    const expiresAt = tokens.expires_in ? new Date(Date.now() + tokens.expires_in * 1000).toISOString() : null;

    const { error } = await supabase.from('platform_connections').upsert({
      user_id: state.userId,
      platform: 'youtube',
      platform_account_id: channel.id,
      account_name: channel.name,
      avatar_url: channel.avatar_url,
      encrypted_tokens: encryptJson({
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        token_type: tokens.token_type,
        scope: tokens.scope
      }),
      token_expires_at: expiresAt,
      updated_at: new Date().toISOString()
    }, { onConflict: 'user_id,platform,platform_account_id' });
    if (error) throw error;
    returnUrl.searchParams.set('connected', 'youtube');
  } catch (error) {
    console.error('YouTube OAuth callback failed:', error.message);
    returnUrl.searchParams.set('oauth_error', error.message);
  }
  res.redirect(returnUrl.toString());
});

app.post('/api/oauth/x/start', requireUser, (_req, res) => {
  if (!process.env.X_CLIENT_ID || !process.env.X_CLIENT_SECRET || !process.env.X_REDIRECT_URI) {
    return res.status(503).json({ error: 'X OAuth is not configured yet' });
  }
  const pkce = createPkcePair();
  const state = signState({
    userId: _req.user.id,
    nonce: crypto.randomUUID(),
    pkce: encryptJson({ verifier: pkce.verifier }),
    exp: Date.now() + 10 * 60 * 1000
  });
  res.json({ url: xAuthorizationUrl(state, pkce.challenge) });
});

app.get('/api/oauth/x/callback', async (req, res) => {
  const returnUrl = new URL(process.env.FRONTEND_URL);
  try {
    if (!process.env.X_CLIENT_ID || !process.env.X_CLIENT_SECRET || !process.env.X_REDIRECT_URI) {
      throw new Error('X OAuth is not configured yet');
    }
    if (req.query.error) throw new Error(String(req.query.error_description || req.query.error));
    const state = verifyState(req.query.state);
    const { verifier } = decryptJson(state.pkce);
    const tokens = await exchangeXCode(String(req.query.code || ''), verifier);
    const account = await getXUser(tokens.access_token);
    const expiresAt = tokens.expires_in
      ? new Date(Date.now() + tokens.expires_in * 1000).toISOString()
      : null;

    const { error } = await supabase.from('platform_connections').upsert({
      user_id: state.userId,
      platform: 'x',
      platform_account_id: account.id,
      account_name: account.name,
      avatar_url: account.avatar_url,
      encrypted_tokens: encryptJson({
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        token_type: tokens.token_type,
        scope: tokens.scope
      }),
      token_expires_at: expiresAt,
      updated_at: new Date().toISOString()
    }, { onConflict: 'user_id,platform,platform_account_id' });
    if (error) throw error;
    returnUrl.searchParams.set('connected', 'x');
  } catch (error) {
    console.error('X OAuth callback failed:', error.message);
    returnUrl.searchParams.set('oauth_error', error.message);
  }
  res.redirect(returnUrl.toString());
});

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ error: 'Unexpected server error' });
});

app.listen(port, '0.0.0.0', () => {
  console.log(`Social Flow API listening on port ${port}`);
});
