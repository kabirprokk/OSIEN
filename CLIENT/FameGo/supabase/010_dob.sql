-- FameGo date-of-birth (required at signup, 8+).
-- Run once in Supabase Dashboard > SQL Editor. Safe to re-run.

alter table public.profiles add column if not exists dob date;

-- Re-create the signup trigger so the dob sent in auth metadata lands on
-- the profile row. Profiles RLS already covers the new column.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  requested_role text := coalesce(nullif(new.raw_user_meta_data ->> 'role', ''), 'CLIENT');
  requested_dob date := null;
begin
  begin
    requested_dob := nullif(new.raw_user_meta_data ->> 'dob', '')::date;
  exception when others then
    requested_dob := null;
  end;
  -- Only CLIENT/CREW can self-register; ADMIN is granted manually.
  if requested_role not in ('CLIENT', 'CREW') then
    requested_role := 'CLIENT';
  end if;
  insert into public.profiles (id, full_name, email, phone, company_name, role, avatar_initials, dob)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data ->> 'phone', ''),
    coalesce(new.raw_user_meta_data ->> 'company_name', ''),
    requested_role,
    upper(left(regexp_replace(coalesce(new.raw_user_meta_data ->> 'full_name', 'FG'), '[^A-Za-z]', '', 'g'), 2)),
    requested_dob
  ) on conflict (id) do update set
    email = excluded.email,
    dob = coalesce(excluded.dob, public.profiles.dob);
  if requested_role = 'CREW' then
    begin
      insert into public.crew_profiles (user_id, primary_role)
      values (new.id, 'ASSISTANT')
      on conflict (user_id) do nothing;
    exception when others then
      -- Never block signup if the crew stub fails; user can complete it later.
      null;
    end;
  end if;
  return new;
end;
$$;
