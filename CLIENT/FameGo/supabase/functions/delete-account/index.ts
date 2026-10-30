// FameGo delete-account Edge Function: REAL backend profile wipe.
//
// DEPLOY (one time):
//   1. supabase functions deploy delete-account
//      (SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY are auto-provided.)
//
// CALL (from the app, authenticated):
//   POST /functions/v1/delete-account  (empty body)
//   with the USER's access token as Bearer. The function resolves the user
//   from the token — a caller can only ever delete their own account.
//
// WHAT IT WIPES (in FK-safe order): device tokens, favorites, saved
// locations, notifications, support threads, ratings given, shoot-done
// signals, chat messages sent, live locations, own assignments, own client
// bookings, crew profile, profile row, then the auth.users row itself
// (which cascades anything left). Returns {deleted:true}.
//

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

serve(async (req) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "POST only" }), { status: 405 });
  }
  const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!jwt) return new Response(JSON.stringify({ error: "Missing token" }), { status: 401 });

  const admin = createClient(SUPABASE_URL, SERVICE_KEY);
  const { data: { user }, error: userErr } = await admin.auth.getUser(jwt);
  if (userErr || !user) {
    return new Response(JSON.stringify({ error: "Invalid session" }), { status: 401 });
  }
  const uid = user.id;

  try {
    // Crew ids owned by this user (for assignments + live rows).
    const { data: crewRows } = await admin
      .from("crew_profiles").select("id").eq("user_id", uid);
    const crewIds: string[] = (crewRows ?? []).map((r: { id: string }) => r.id);

    await admin.from("device_tokens").delete().eq("user_id", uid);
    await admin.from("favorite_crew").delete().eq("client_id", uid);
    await admin.from("saved_locations").delete().eq("user_id", uid);
    await admin.from("notifications").delete().eq("target_user_id", uid);
    await admin.from("support_messages").delete().eq("user_id", uid);
    await admin.from("crew_ratings").delete().eq("client_id", uid);
    await admin.from("shoot_done_signals").delete().eq("user_id", uid);
    await admin.from("chat_messages").delete().eq("sender_id", uid);
    for (const cid of crewIds) {
      await admin.from("crew_live_locations").delete().eq("crew_id", cid);
      await admin.from("booking_assignments").delete().eq("crew_id", cid);
      await admin.from("crew_ratings").delete().eq("crew_id", cid);
    }
    await admin.from("bookings").delete().eq("client_id", uid);
    await admin.from("crew_profiles").delete().eq("user_id", uid);
    await admin.from("profiles").delete().eq("id", uid);

    const { error: delErr } = await admin.auth.admin.deleteUser(uid);
    if (delErr) throw delErr;
    return new Response(JSON.stringify({ deleted: true }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e?.message ?? e) }), { status: 500 });
  }
});
