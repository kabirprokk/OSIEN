-- FameGo fixed role assignment (one-time, safe to re-run).
-- Run in Supabase Dashboard > SQL Editor.
--
-- kabirsayed.k@gmail.com -> CLIENT (can book shoots)
-- famebros.studio@gmail.com -> CREW (receives shoot requests + full-screen popups)
--
-- Uses auth.users email lookup so it works regardless of user id.

do $$
declare
  v_client_id uuid;
  v_crew_id uuid;
begin
  select id into v_client_id from auth.users where lower(email) = lower('kabirsayed.k@gmail.com') limit 1;
  select id into v_crew_id from auth.users where lower(email) = lower('famebros.studio@gmail.com') limit 1;

  if v_client_id is not null then
    update public.profiles
      set role = 'CLIENT', email = 'kabirsayed.k@gmail.com', updated_at = now()
      where id = v_client_id;
  end if;

  if v_crew_id is not null then
    update public.profiles
      set role = 'CREW', email = 'famebros.studio@gmail.com', updated_at = now()
      where id = v_crew_id;

    -- Crew phones need a crew_profiles row to appear available + accept shoots.
    insert into public.crew_profiles (user_id, primary_role, is_available, verification_status)
      values (v_crew_id, 'CINEMATOGRAPHER', true, 'VERIFIED')
      on conflict (user_id) do update set
        is_available = true,
        verification_status = 'VERIFIED',
        updated_at = now();
  end if;

  if v_client_id is null then
    raise notice 'client email not signed up yet: kabirsayed.k@gmail.com';
  end if;
  if v_crew_id is null then
    raise notice 'crew email not signed up yet: famebros.studio@gmail.com';
  end if;
end $$;
