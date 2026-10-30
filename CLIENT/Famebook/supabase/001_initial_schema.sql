-- FameBook initial Supabase schema.
-- Run this file in Supabase SQL Editor before enabling the remote repositories.

create type public.user_role as enum ('CLIENT', 'CREW', 'ADMIN');
create type public.booking_status as enum (
  'PENDING', 'SEARCHING_CREW', 'OFFERED', 'CONFIRMED',
  'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  email text not null,
  phone text not null default '',
  role public.user_role not null default 'CLIENT',
  company_name text,
  bio text not null default '',
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.crew_profiles (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  primary_role text not null,
  secondary_roles text[] not null default '{}',
  skills text[] not null default '{}',
  experience_years integer not null default 0 check (experience_years >= 0),
  gear_summary text not null default '',
  rating numeric(3,2) not null default 0 check (rating between 0 and 5),
  total_completed_shoots integer not null default 0 check (total_completed_shoots >= 0),
  is_available boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.bookings (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.profiles(id),
  shoot_type text not null,
  title text not null,
  description text not null default '',
  shoot_date date not null,
  start_time time not null,
  duration_hours integer not null check (duration_hours > 0),
  location_name text not null,
  location_address text not null,
  location_city text not null default 'Mumbai',
  location_notes text not null default '',
  special_instructions text not null default '',
  status public.booking_status not null default 'SEARCHING_CREW',
  assigned_crew_id uuid references public.profiles(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.booking_requirements (
  booking_id uuid not null references public.bookings(id) on delete cascade,
  crew_role text not null,
  quantity integer not null default 1 check (quantity > 0),
  equipment_notes text not null default '',
  primary key (booking_id, crew_role)
);

create table public.messages (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null references public.bookings(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  body text not null check (char_length(trim(body)) > 0),
  created_at timestamptz not null default now()
);

create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  recipient_id uuid not null references public.profiles(id) on delete cascade,
  booking_id uuid references public.bookings(id) on delete cascade,
  title text not null,
  body text not null,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);

create index bookings_client_id_idx on public.bookings(client_id);
create index bookings_status_idx on public.bookings(status);
create index bookings_assigned_crew_idx on public.bookings(assigned_crew_id);
create index messages_booking_id_created_at_idx on public.messages(booking_id, created_at);
create index notifications_recipient_created_at_idx on public.notifications(recipient_id, created_at desc);

create or replace function public.set_updated_at()
returns trigger language plpgsql security invoker set search_path = public as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
create trigger crew_profiles_updated_at before update on public.crew_profiles
for each row execute function public.set_updated_at();
create trigger bookings_updated_at before update on public.bookings
for each row execute function public.set_updated_at();

create or replace function public.current_role()
returns public.user_role language sql stable security definer set search_path = public as $$
  select role from public.profiles where id = auth.uid();
$$;

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name, email)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
    new.email
  );
  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

create or replace function public.prevent_self_role_change()
returns trigger language plpgsql security invoker set search_path = public as $$
begin
  if new.role is distinct from old.role and public.current_role() <> 'ADMIN' then
    raise exception 'Role changes are controlled by administrators';
  end if;
  return new;
end;
$$;

create trigger profiles_role_guard before update on public.profiles
for each row execute function public.prevent_self_role_change();

-- Deliberately excludes client phone and email. Crew receives only the fields
-- needed to decide whether to accept an available request.
create or replace function public.get_available_booking_requests()
returns table (
  id uuid,
  shoot_type text,
  title text,
  description text,
  shoot_date date,
  start_time time,
  duration_hours integer,
  location_name text,
  location_city text,
  location_notes text,
  special_instructions text,
  status public.booking_status,
  created_at timestamptz
) language sql stable security definer set search_path = public as $$
  select b.id, b.shoot_type, b.title, b.description, b.shoot_date,
    b.start_time, b.duration_hours, b.location_name, b.location_city,
    b.location_notes, b.special_instructions, b.status, b.created_at
  from public.bookings b
  where public.current_role() = 'CREW'
    and b.assigned_crew_id is null
    and b.status in ('SEARCHING_CREW', 'OFFERED')
  order by b.created_at desc;
$$;

-- Atomic single-assignment operation. The update predicate is the authority;
-- two concurrent crew requests cannot both claim the same booking.
create or replace function public.accept_booking(p_booking_id uuid)
returns public.bookings language plpgsql security definer set search_path = public as $$
declare
  updated_booking public.bookings;
begin
  if public.current_role() <> 'CREW' then
    raise exception 'Only crew members can accept bookings';
  end if;

  update public.bookings
  set assigned_crew_id = auth.uid(), status = 'CONFIRMED', updated_at = now()
  where id = p_booking_id
    and assigned_crew_id is null
    and status in ('SEARCHING_CREW', 'OFFERED')
  returning * into updated_booking;

  if updated_booking.id is null then
    raise exception 'REQUEST_ALREADY_ASSIGNED';
  end if;
  return updated_booking;
end;
$$;

alter table public.profiles enable row level security;
alter table public.crew_profiles enable row level security;
alter table public.bookings enable row level security;
alter table public.booking_requirements enable row level security;
alter table public.messages enable row level security;
alter table public.notifications enable row level security;

create policy profiles_read_authenticated on public.profiles for select
using (auth.uid() is not null);
create policy profiles_update_self on public.profiles for update
using (id = auth.uid()) with check (id = auth.uid());

create policy crew_profiles_read_authenticated on public.crew_profiles for select
using (auth.uid() is not null);
create policy crew_profiles_update_self on public.crew_profiles for update
using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy bookings_client_create on public.bookings for insert
with check (client_id = auth.uid() and public.current_role() = 'CLIENT');
create policy bookings_participant_read on public.bookings for select
using (client_id = auth.uid() or assigned_crew_id = auth.uid() or public.current_role() = 'ADMIN');
create policy bookings_client_cancel on public.bookings for update
using (client_id = auth.uid()) with check (client_id = auth.uid());
create policy bookings_crew_status on public.bookings for update
using (assigned_crew_id = auth.uid()) with check (assigned_crew_id = auth.uid());

create policy booking_requirements_participant_read on public.booking_requirements for select
using (exists (
  select 1 from public.bookings b
  where b.id = booking_id
    and (b.client_id = auth.uid() or b.assigned_crew_id = auth.uid() or public.current_role() = 'ADMIN')
));
create policy booking_requirements_client_create on public.booking_requirements for insert
with check (exists (select 1 from public.bookings b where b.id = booking_id and b.client_id = auth.uid()));

create policy messages_participant_read on public.messages for select
using (exists (
  select 1 from public.bookings b
  where b.id = booking_id and (b.client_id = auth.uid() or b.assigned_crew_id = auth.uid() or public.current_role() = 'ADMIN')
));
create policy messages_participant_send on public.messages for insert
with check (sender_id = auth.uid() and exists (
  select 1 from public.bookings b
  where b.id = booking_id and (b.client_id = auth.uid() or b.assigned_crew_id = auth.uid())
));

create policy notifications_self_read on public.notifications for select
using (recipient_id = auth.uid());
create policy notifications_self_update on public.notifications for update
using (recipient_id = auth.uid()) with check (recipient_id = auth.uid());

grant execute on function public.accept_booking(uuid) to authenticated;
grant execute on function public.get_available_booking_requests() to authenticated;
