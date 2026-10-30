import fs from 'node:fs/promises';
import { decryptJson, encryptJson } from './crypto.js';

const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const CHUNK_SIZE = 8 * 1024 * 1024;

async function googleError(response, fallback) {
  const body = await response.json().catch(() => ({}));
  return new Error(body.error?.message || body.error_description || body.error || fallback);
}

export async function validAccessToken(supabase, connection) {
  const tokens = decryptJson(connection.encrypted_tokens);
  const expiresAt = connection.token_expires_at ? new Date(connection.token_expires_at).getTime() : 0;
  if (tokens.access_token && expiresAt > Date.now() + 5 * 60 * 1000) return tokens.access_token;
  if (!tokens.refresh_token) throw new Error('YouTube authorization expired. Reconnect the channel.');

  const response = await fetch(TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: process.env.GOOGLE_CLIENT_ID,
      client_secret: process.env.GOOGLE_CLIENT_SECRET,
      refresh_token: tokens.refresh_token,
      grant_type: 'refresh_token'
    })
  });
  const refreshed = await response.json();
  if (!response.ok) throw new Error(refreshed.error_description || 'Unable to refresh YouTube authorization');

  const nextTokens = { ...tokens, ...refreshed, refresh_token: tokens.refresh_token };
  const nextExpiry = new Date(Date.now() + refreshed.expires_in * 1000).toISOString();
  const { error } = await supabase.from('platform_connections').update({
    encrypted_tokens: encryptJson(nextTokens),
    token_expires_at: nextExpiry,
    updated_at: new Date().toISOString()
  }).eq('id', connection.id);
  if (error) throw new Error('Unable to securely update YouTube authorization');
  return refreshed.access_token;
}

export async function uploadVideoResumable({ accessToken, file, metadata, onProgress }) {
  const createResponse = await fetch(
    'https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status',
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json; charset=UTF-8',
        'X-Upload-Content-Length': String(file.size),
        'X-Upload-Content-Type': file.mimetype
      },
      body: JSON.stringify({
        snippet: {
          title: metadata.title,
          description: metadata.description,
          tags: metadata.tags
        },
        status: { privacyStatus: metadata.privacy }
      })
    }
  );
  if (!createResponse.ok) throw await googleError(createResponse, 'Unable to initialize YouTube upload');
  const uploadUrl = createResponse.headers.get('location');
  if (!uploadUrl) throw new Error('YouTube did not provide an upload session');

  const handle = await fs.open(file.path, 'r');
  let offset = 0;
  let video;
  try {
    while (offset < file.size) {
      const length = Math.min(CHUNK_SIZE, file.size - offset);
      const chunk = Buffer.allocUnsafe(length);
      const { bytesRead } = await handle.read(chunk, 0, length, offset);
      if (!bytesRead) throw new Error('The uploaded video could not be read');
      const end = offset + bytesRead - 1;
      const response = await fetch(uploadUrl, {
        method: 'PUT',
        redirect: 'manual',
        headers: {
          'Content-Type': file.mimetype,
          'Content-Length': String(bytesRead),
          'Content-Range': `bytes ${offset}-${end}/${file.size}`
        },
        body: bytesRead === chunk.length ? chunk : chunk.subarray(0, bytesRead)
      });
      if (response.status === 308) {
        offset += bytesRead;
        onProgress(Math.min(99, Math.round((offset / file.size) * 100)));
        continue;
      }
      if (!response.ok) throw await googleError(response, 'YouTube video upload failed');
      video = await response.json();
      offset = file.size;
      onProgress(100);
    }
  } finally {
    await handle.close();
  }
  if (!video?.id) throw new Error('YouTube finished the transfer without returning a video ID');
  return video;
}

export async function uploadThumbnail(accessToken, videoId, thumbnail) {
  if (!thumbnail) return;
  const bytes = await fs.readFile(thumbnail.path);
  const response = await fetch(
    `https://www.googleapis.com/upload/youtube/v3/thumbnails/set?videoId=${encodeURIComponent(videoId)}&uploadType=media`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': thumbnail.mimetype,
        'Content-Length': String(bytes.length)
      },
      body: bytes
    }
  );
  if (!response.ok) throw await googleError(response, 'Video uploaded, but the custom thumbnail failed');
}

export async function waitForVideoProcessing(accessToken, videoId, onStatus) {
  const deadline = Date.now() + 30 * 60 * 1000;
  while (Date.now() < deadline) {
    const response = await fetch(
      `https://www.googleapis.com/youtube/v3/videos?part=status,processingDetails&id=${encodeURIComponent(videoId)}`,
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
    if (!response.ok) throw await googleError(response, 'Unable to read YouTube processing status');
    const video = (await response.json()).items?.[0];
    if (!video) throw new Error('The uploaded video could not be found during processing');

    const uploadStatus = video.status?.uploadStatus;
    const processingStatus = video.processingDetails?.processingStatus;
    const progress = video.processingDetails?.processingProgress;
    let percent = null;
    if (progress?.partsTotal && progress?.partsProcessed) {
      percent = Math.min(99, Math.round((Number(progress.partsProcessed) / Number(progress.partsTotal)) * 100));
    }
    onStatus({ uploadStatus, processingStatus, percent });

    if (processingStatus === 'succeeded' || uploadStatus === 'processed') return { processed: true };
    if (['failed', 'terminated'].includes(processingStatus) || ['failed', 'rejected'].includes(uploadStatus)) {
      const reason = video.status?.failureReason || video.status?.rejectionReason || processingStatus || uploadStatus;
      throw new Error(`YouTube could not process the video: ${reason}`);
    }
    await new Promise(resolve => setTimeout(resolve, 8000));
  }
  return { processed: false };
}
