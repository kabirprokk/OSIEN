-- Fix crew availability toggle crash: allow self-creation of crew_profiles.
-- Run this file in Supabase SQL Editor after 002_admin_role_management.sql.

drop policy if exists crew_profiles_self_insert on public.crew_profiles;
create policy crew_profiles_self_insert on public.crew_profiles for insert
with check (user_id = auth.uid());

drop policy if exists crew_profiles_admin_insert on public.crew_profiles;
create policy crew_profiles_admin_insert on public.crew_profiles for insert
with check (public.current_role() = 'ADMIN');

drop policy if exists crew_profiles_admin_update on public.crew_profiles;
create policy crew_profiles_admin_update on public.crew_profiles for update
using (public.current_role() = 'ADMIN')
with check (public.current_role() = 'ADMIN');
