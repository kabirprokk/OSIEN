-- FameGo repair: payment + plan columns on bookings.
-- Run once in Supabase Dashboard > SQL Editor if you see:
--   PGRST204 "Could not find the 'payment_reference' column of 'bookings'"
-- (happens when schema.sql was first run before these columns existed).
-- Safe to re-run: every statement is guarded. PostgREST reloads its schema
-- cache automatically after DDL, so the 400 disappears on the next request.

alter table public.bookings add column if not exists plan_code text not null default 'BRONZE_3H';
alter table public.bookings add column if not exists plan_name text not null default 'Bronze 3 Hour';
alter table public.bookings add column if not exists plan_price_paise integer not null default 299900;
alter table public.bookings add column if not exists payment_status text not null default 'PENDING';
alter table public.bookings add column if not exists payment_reference text not null default '';

-- Keep the checks in sync when the table predates them.
do $$
begin
  alter table public.bookings drop constraint if exists bookings_plan_code_check;
  alter table public.bookings add constraint bookings_plan_code_check
    check (plan_code in ('BRONZE_90', 'BRONZE_3H', 'BRONZE_6H'));
exception when others then null;
end $$;

do $$
begin
  alter table public.bookings drop constraint if exists bookings_payment_status_check;
  alter table public.bookings add constraint bookings_payment_status_check
    check (payment_status in ('PENDING', 'PAID', 'FAILED', 'REFUNDED'));
exception when others then null;
end $$;

-- Verify: this must return all five columns.
select column_name from information_schema.columns
where table_schema = 'public' and table_name = 'bookings'
  and column_name in ('plan_code', 'plan_name', 'plan_price_paise', 'payment_status', 'payment_reference');
