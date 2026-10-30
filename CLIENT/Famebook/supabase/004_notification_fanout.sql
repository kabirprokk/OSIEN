-- FameBook cross-device notification fan-out.
-- Run this file in Supabase SQL Editor after 003_crew_availability_fix.sql.

-- Booking flow writes notifications for other users (client notifies crew of
-- new requests, accepting crew notifies the client), so inserts must be
-- allowed for any signed-in user. Reads stay restricted to the recipient.
drop policy if exists notifications_insert_participant on public.notifications;
create policy notifications_insert_participant on public.notifications for insert
with check (auth.uid() is not null);
