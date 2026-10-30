-- FameGo admin user management (safe to re-run).
-- Run once in Supabase Dashboard > SQL Editor.
--
-- The app's Admin > Users panel lists every profile and switches roles.
-- profiles_select / profiles_update already allow admins; this file adds the
-- missing half: an admin may create the crew_profiles stub when promoting a
-- user to CREW (the self-serve insert policy only covers one's own row).

drop policy if exists crew_profiles_admin_insert on public.crew_profiles;
create policy crew_profiles_admin_insert on public.crew_profiles
  for insert to authenticated
  with check (public.is_admin());

drop policy if exists crew_profiles_admin_delete on public.crew_profiles;
create policy crew_profiles_admin_delete on public.crew_profiles
  for delete to authenticated
  using (public.is_admin());
