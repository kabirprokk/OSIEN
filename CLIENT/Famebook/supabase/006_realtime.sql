-- FameBook live updates over Supabase Realtime websockets.
-- Run this file in Supabase SQL Editor after 005_favorite_crew.sql.
-- Without this, postgres_changes delivers nothing and the app falls back
-- to periodic refresh only.

alter publication supabase_realtime add table public.bookings;
alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.notifications;
alter publication supabase_realtime add table public.profiles;
alter publication supabase_realtime add table public.crew_profiles;
alter publication supabase_realtime add table public.booking_requirements;
alter publication supabase_realtime add table public.favorite_crew;
