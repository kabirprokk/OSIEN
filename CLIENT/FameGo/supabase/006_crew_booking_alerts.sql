-- FameGo crew booking alerts (old-app parity, new-style delivery).
-- Run once in Supabase Dashboard > SQL Editor AFTER schema.sql + 002_notification_fanout.sql.
-- Safe to re-run.
--
-- What it does (mirrors the old app's "crew sees every paid request" behaviour):
-- 1. When a booking is INSERTED as PAID+SEARCHING_CREW, or an existing booking
--    flips to PAID+SEARCHING_CREW, every CREW profile gets a notifications row.
--    -> In-app bell + realtime + foreground full-screen popup work immediately,
--       with zero Firebase setup.
-- 2. Killed-app wake still needs FCM: supabase/functions/push-send delivers the
--    same event to device_tokens / crew_requests topic. Wire it with pg_net
--    (optional block at the bottom) or a Database Webhook on bookings.
--
-- Old app reference: bookings pool (status=SEARCHING_CREW) + incomingShootRequests
-- + notifications bell. This trigger is the server half that the old app lacked.

-- 1. Fan-out function: one notifications row per crew member.
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
  -- Only paid bookings looking for crew alert the crew pool.
  if new.payment_status <> 'PAID' or new.status <> 'SEARCHING_CREW' then
    return new;
  end if;
  -- On UPDATE, only fire when the booking *became* payable (not every edit).
  if tg_op = 'UPDATE' then
    if old.payment_status = new.payment_status and old.status = new.status then
      return new;
    end if;
    if old.payment_status = 'PAID' and old.status = 'SEARCHING_CREW' then
      return new;
    end if;
  end if;

  v_title := 'New shoot request';
  v_body := coalesce(new.shoot_title, 'New shoot')
    || ' • ' || coalesce(new.venue_name, 'Venue TBA')
    || ' • ' || coalesce(new.shoot_date::text, '');

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

-- 2. Realtime: crew bell + booking pool stay live (idempotent guards).
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
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'bookings'
  ) then
    alter publication supabase_realtime add table public.bookings;
  end if;
end $$;

-- 3. OPTIONAL killed-app push via pg_net -> push-send Edge Function.
-- Uncomment after `supabase secrets set PUSH_SEND_URL / PUSH_SEND_KEY` and enabling pg_net.
--
-- create extension if not exists pg_net;
-- create or replace function public.push_paid_booking_to_fcm()
-- returns trigger language plpgsql security definer set search_path = public, net as $$
-- begin
--   if new.payment_status <> 'PAID' or new.status <> 'SEARCHING_CREW' then return new; end if;
--   if tg_op = 'UPDATE' and old.payment_status = 'PAID' and old.status = 'SEARCHING_CREW' then return new; end if;
--   perform net.http_post(
--     url := current_setting('app.push_send_url', true),
--     headers := jsonb_build_object(
--       'Content-Type', 'application/json',
--       'Authorization', 'Bearer ' || current_setting('app.push_send_key', true)
--     ),
--     body := jsonb_build_object(
--       'topic', 'crew_requests',
--       'title', 'New shoot request',
--       'body', coalesce(new.shoot_title, 'New shoot') || ' • ' || coalesce(new.venue_name, ''),
--       'booking_id', new.id::text
--     )
--   );
--   return new;
-- end; $$;
-- drop trigger if exists bookings_push_to_fcm on public.bookings;
-- create trigger bookings_push_to_fcm
--   after insert or update of payment_status, status on public.bookings
--   for each row execute function public.push_paid_booking_to_fcm();
