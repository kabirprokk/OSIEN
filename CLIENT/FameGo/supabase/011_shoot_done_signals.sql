-- FameGo Bluetooth proximity Shoot-Done signals.
-- Run once in Supabase Dashboard > SQL Editor. Safe to re-run.
--
-- HOW it proves the wrap: at the venue, BOTH phones press "Shoot Done".
-- Each phone BLE-advertises a booking-specific ID and scans for it; only
-- physically-nearby phones (~10-30m) can discover each other. On discovery
-- each phone inserts its row here. When rows from BOTH sides exist, the
-- booking flips to COMPLETED and both parties get "Shoot is Done".

create table if not exists public.shoot_done_signals (
  booking_id uuid not null references public.bookings(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default '' check (role in ('', 'CLIENT', 'CREW', 'ADMIN')),
  created_at timestamptz not null default now(),
  primary key (booking_id, user_id)
);

create index if not exists shoot_done_booking_idx
  on public.shoot_done_signals(booking_id);

alter table public.shoot_done_signals enable row level security;

-- Participants of the booking can record and read signals; admins see all.
drop policy if exists shoot_done_select on public.shoot_done_signals;
create policy shoot_done_select on public.shoot_done_signals for select
to authenticated using (
  public.is_admin()
  or user_id = (select auth.uid())
  or exists (select 1 from public.bookings b where b.id = booking_id and b.client_id = (select auth.uid()))
  or exists (
    select 1 from public.booking_assignments a
    join public.crew_profiles c on c.id = a.crew_id
    where a.booking_id = shoot_done_signals.booking_id and c.user_id = (select auth.uid())
  )
);

drop policy if exists shoot_done_insert on public.shoot_done_signals;
create policy shoot_done_insert on public.shoot_done_signals for insert
to authenticated with check (
  user_id = (select auth.uid())
  and (
    exists (select 1 from public.bookings b where b.id = booking_id and b.client_id = (select auth.uid()))
    or exists (
      select 1 from public.booking_assignments a
      join public.crew_profiles c on c.id = a.crew_id
      where a.booking_id = shoot_done_signals.booking_id and c.user_id = (select auth.uid())
    )
  )
);

grant select, insert on public.shoot_done_signals to authenticated;
