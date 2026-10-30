-- FameGo crew-first pipeline: find crew BEFORE money moves.
-- Run once in Supabase Dashboard > SQL Editor AFTER schema.sql (+002/006).
-- Safe to re-run.
--
-- New pipeline:
--   1. Client creates booking UNPAID (payment_status PENDING, SEARCHING_CREW).
--   2. Crew sees + accepts unpaid requests; booking flips CONFIRMED (still PENDING).
--   3. Client pays (payment_status PAID). Cancel is free within 10 minutes of
--      booking creation; after that no refund — support only (app-enforced).
--   Paradox solved: if no crew accepts, no money was ever taken.

-- 1. Crew can now see + accept unpaid open requests.
drop policy if exists bookings_select on public.bookings;
create policy bookings_select on public.bookings for select to authenticated
using (
  client_id = (select auth.uid()) or public.is_admin() or
  (public.is_crew() and status = 'SEARCHING_CREW'
    and payment_status in ('PENDING', 'PAID')) or
  exists (
    select 1 from public.booking_assignments a
    join public.crew_profiles c on c.id = a.crew_id
    where a.booking_id = bookings.id and c.user_id = (select auth.uid())
  )
);

-- 2. Accept works on unpaid requests; payment stays as it was.
create or replace function public.accept_booking(p_booking_id uuid)
returns public.booking_assignments
language plpgsql security definer set search_path = '' as $$
declare
  v_crew public.crew_profiles;
  v_booking public.bookings;
  v_profile public.profiles;
  v_assignment public.booking_assignments;
begin
  select * into v_crew from public.crew_profiles where user_id = auth.uid() and is_available for update;
  if v_crew.id is null then raise exception 'crew_not_available'; end if;
  select * into v_booking from public.bookings where id = p_booking_id for update;
  if v_booking.id is null or v_booking.status <> 'SEARCHING_CREW'
    or v_booking.payment_status not in ('PENDING', 'PAID') then
    raise exception 'booking_not_available';
  end if;
  select * into v_profile from public.profiles where id = auth.uid();
  insert into public.booking_assignments (
    booking_id, crew_id, role, name_snapshot, phone_snapshot, gear_snapshot, rating_snapshot, is_verified_snapshot
  ) values (
    v_booking.id, v_crew.id, v_crew.primary_role, v_profile.full_name, v_profile.phone,
    v_crew.gear_summary, v_crew.rating, v_crew.verification_status = 'VERIFIED'
  ) returning * into v_assignment;
  update public.bookings set status = 'CONFIRMED' where id = v_booking.id;
  update public.crew_profiles set is_available = false where id = v_crew.id;
  return v_assignment;
end;
$$;

-- 3. Client may mark their own CONFIRMED booking paid (was admin/client-update
--    blocked for non-clients; keep it tight: owner or admin only).
drop policy if exists bookings_client_update on public.bookings;
create policy bookings_client_update on public.bookings for update to authenticated
using (client_id = auth.uid() or public.is_admin())
with check ((client_id = auth.uid() and public.is_client()) or public.is_admin());

-- 4. Crew alerts + fan-out now fire for unpaid open requests too (was PAID only).
create or replace function public.fanout_paid_booking_to_crew()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_title text;
  v_body text;
begin
  -- Paid OR fresh unpaid bookings looking for crew alert the crew pool.
  if new.status <> 'SEARCHING_CREW' then
    return new;
  end if;
  if new.payment_status not in ('PENDING', 'PAID') then
    return new;
  end if;
  -- On UPDATE, only fire when the booking *became* searchable (not every edit).
  if tg_op = 'UPDATE' then
    if old.status = 'SEARCHING_CREW' and old.payment_status = new.payment_status then
      return new;
    end if;
    if old.status = 'SEARCHING_CREW' then
      return new;
    end if;
  end if;

  v_title := 'New shoot request';
  v_body := coalesce(new.shoot_title, 'New shoot')
    || ' • ' || coalesce(new.venue_name, 'Venue TBA')
    || ' • ' || coalesce(new.shoot_date::text, '')
    || case when new.payment_status = 'PENDING' then ' • pay on accept' else '' end;

  insert into public.notifications (target_user_id, title, message, booking_id)
  select p.id, v_title, v_body, new.id
  from public.profiles p
  where p.role = 'CREW'
    and p.id <> new.client_id
  on conflict do nothing;

  return new;
end;
$$;

drop trigger if exists bookings_fanout_to_crew on public.bookings;
create trigger bookings_fanout_to_crew
  after insert or update of payment_status, status on public.bookings
  for each row execute function public.fanout_paid_booking_to_crew();
