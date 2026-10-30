-- FameGo cross-device notification fan-out (Famebook 004 pattern).
-- Run this file once in Supabase Dashboard > SQL Editor AFTER schema.sql.
-- Safe to re-run: every statement is guarded.
--
-- What it enables:
-- 1. Any signed-in participant can INSERT a notifications row for the other
--    party (booking accepted / cancelled / completed / new chat message).
--    Reads stay restricted to the recipient (existing notifications_select).
-- 2. Realtime publishes notifications + booking_assignments, so the bell and
--    crew/client screens update live instead of on refresh timers.

-- 1. Participant inserts (reads stay recipient-only).
drop policy if exists notifications_insert_participant on public.notifications;
create policy notifications_insert_participant on public.notifications for insert
with check (auth.uid() is not null);

-- 2. Realtime publication for the fan-out tables.
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'notifications'
  ) then
    alter publication supabase_realtime add table public.notifications;
  end if;
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'booking_assignments'
  ) then
    alter publication supabase_realtime add table public.booking_assignments;
  end if;
end $$;
