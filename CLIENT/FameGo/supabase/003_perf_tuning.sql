-- FameGo performance tuning: hot-path indexes.
-- Run once in Supabase Dashboard > SQL Editor AFTER 002_notification_fanout.sql.
-- Safe to re-run: every statement is guarded.

-- Crew assignment lookups per booking (accept flow, peer resolution).
create index if not exists assignments_booking_id_idx
  on public.booking_assignments(booking_id);

-- Bell query: newest-first per recipient.
create index if not exists notifications_target_created_idx
  on public.notifications(target_user_id, created_at desc);

-- Support inbox newest-first per user.
create index if not exists support_user_created_idx
  on public.support_messages(user_id, created_at desc);

-- Crew pool scan for open requests.
create index if not exists bookings_searching_created_idx
  on public.bookings(status, created_at desc)
  where status = 'SEARCHING_CREW';
