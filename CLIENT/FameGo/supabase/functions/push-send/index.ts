// FameGo push-send Edge Function: Supabase -> FCM HTTP v1 -> phones.
//
// DEPLOY (one time):
//   1. Firebase Console > Project settings > Service accounts >
//      "Generate new private key" (famego-official). Keep this file SECRET.
//   2. supabase secrets set FCM_SERVICE_ACCOUNT='<entire JSON on one line>'
//   3. supabase functions deploy push-send
//
// CALL (from a trigger, cron, or admin tool — NOT from the app):
//   POST /functions/v1/push-send  { user_ids: [...], title, body, booking_id? }
//   POST /functions/v1/push-send  { topic: "crew_requests", title, body, booking_id? }
//   with the service_role key. The function fans out via device_tokens.
//
// The Android app already writes notifications rows + registers device_tokens;
// this function is what delivers them when the app is killed/backgrounded.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SERVICE_ACCOUNT = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

async function fcmAccessToken(): Promise<string> {
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const claims = {
    iss: SERVICE_ACCOUNT.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };
  const b64 = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  const unsigned = `${b64(header)}.${b64(claims)}`;
  const key = SERVICE_ACCOUNT.private_key.replace(/\\n/g, "\n");
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    Uint8Array.from(atob(key.split("-----")[2].replace(/\s/g, "")), (c) => c.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, new TextEncoder().encode(unsigned));
  const signed = `${unsigned}.${btoa(String.fromCharCode(...new Uint8Array(sig))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")}`;
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${signed}`,
  });
  const json = await res.json();
  if (!json.access_token) throw new Error(`oauth: ${JSON.stringify(json)}`);
  return json.access_token as string;
}

serve(async (req) => {
  if (req.method !== "POST") return new Response("ok");
  try {
    const { user_ids = [], topic = null, title = "FameGo", body = "", booking_id = null } = await req.json();
    const hasTopic = typeof topic === "string" && topic.length > 0;
    const hasUsers = Array.isArray(user_ids) && user_ids.length > 0;
    if (!hasTopic && !hasUsers) {
      return Response.json({ sent: 0 });
    }
    const db = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);
    let tokens: string[] = [];
    if (hasUsers) {
      const { data: rows } = await db
        .from("device_tokens")
        .select("token")
        .in("user_id", user_ids);
      tokens = [...new Set((rows ?? []).map((r) => r.token).filter(Boolean))];
      if (tokens.length === 0 && !hasTopic) return Response.json({ sent: 0 });
    }

    const access = await fcmAccessToken();
    let sent = 0;
    // Broadcast mode: one send to the crew_requests topic (new paid bookings).
    // Direct mode: one send per device token (status flips, chat, assignment).
    const targets: Array<{ topic?: string; token?: string }> =
      hasTopic
        ? [{ topic }]
        : tokens.map((t: string) => ({ token: t }));
    for (const target of targets) {
      const res = await fetch(
        `https://fcm.googleapis.com/v1/projects/${SERVICE_ACCOUNT.project_id}/messages:send`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${access}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            message: {
              ...target,
              // High priority + data payload wakes the app even when killed;
              // the Android side renders the heads-up + full-screen popup.
              android: {
                priority: "HIGH",
                direct_boot_ok: true,
                notification: {
                  channel_id: "famego_shoots",
                  visibility: "PUBLIC",
                  default_vibrate_timings: true,
                },
              },
              notification: { title, body },
              data: {
                title,
                body,
                type: "shoot_request",
                ...(booking_id ? { booking_id: String(booking_id) } : {}),
              },
            },
          }),
        },
      );
      if (res.ok) sent++;
      else console.error("fcm", res.status, await res.text());
    }
    return Response.json({ sent });
  } catch (e) {
    console.error(e);
    return Response.json({ error: String(e) }, { status: 500 });
  }
});
