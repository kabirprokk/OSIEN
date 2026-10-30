-- FameBook admin role management.
-- Run this file in Supabase SQL Editor after 001_initial_schema.sql.

-- Allow admins to update any profile (role changes still guarded by trigger).
drop policy if exists profiles_admin_update on public.profiles;
create policy profiles_admin_update on public.profiles for update
using (public.current_role() = 'ADMIN')
with check (public.current_role() = 'ADMIN');

-- Admin-only role assignment. Security definer so it works under RLS.
-- Creates a crew_profiles row when promoting to CREW.
create or replace function public.assign_user_role(p_user_id uuid, p_role public.user_role)
returns public.profiles language plpgsql security definer set search_path = public as $$
declare
  updated_profile public.profiles;
begin
  if public.current_role() <> 'ADMIN' then
    raise exception 'Only admins can assign roles';
  end if;

  update public.profiles
  set role = p_role, updated_at = now()
  where id = p_user_id
  returning * into updated_profile;

  if updated_profile.id is null then
    raise exception 'User not found';
  end if;

  if p_role = 'CREW' then
    insert into public.crew_profiles (user_id, primary_role)
    values (p_user_id, 'PHOTOGRAPHER')
    on conflict (user_id) do nothing;
  end if;

  return updated_profile;
end;
$$;

grant execute on function public.assign_user_role(uuid, public.user_role) to authenticated;
