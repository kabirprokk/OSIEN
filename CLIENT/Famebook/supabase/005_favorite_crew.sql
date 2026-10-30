-- FameBook favorite crew for one-tap rebooking.
-- Run this file in Supabase SQL Editor after 004_notification_fanout.sql.

create table public.favorite_crew (
  client_id uuid not null references public.profiles(id) on delete cascade,
  crew_user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (client_id, crew_user_id)
);

create index favorite_crew_client_idx on public.favorite_crew(client_id);

alter table public.favorite_crew enable row level security;

-- Clients manage only their own favorites list.
create policy favorite_crew_owner_read on public.favorite_crew for select
using (client_id = auth.uid());

create policy favorite_crew_owner_insert on public.favorite_crew for insert
with check (client_id = auth.uid());

create policy favorite_crew_owner_delete on public.favorite_crew for delete
using (client_id = auth.uid());
