-- FameGo support replies (safe to re-run).
-- Run once in Supabase Dashboard > SQL Editor.
--
-- Users already write support_messages; this lets admins reply inside the
-- same thread (user_id = requester, is_from_support = true). Reads were
-- already open to both parties via support_owner.

drop policy if exists support_admin_insert on public.support_messages;
create policy support_admin_insert on public.support_messages
  for insert to authenticated
  with check (public.is_admin());
