-- FameGo crash reports (safe to re-run).
-- Run once in Supabase Dashboard > SQL Editor.
-- The app posts uncaught crashes here; admins read them in Table Editor.

create table if not exists public.crash_reports (
  id uuid primary key default gen_random_uuid(),
  app_version text not null default '',
  android_version text not null default '',
  device_model text not null default '',
  stacktrace text not null default '',
  created_at timestamptz not null default now()
);

create index if not exists crash_reports_created_idx
  on public.crash_reports(created_at desc);

alter table public.crash_reports enable row level security;

-- Any install (even logged out) may report; only admins may read.
drop policy if exists crash_reports_insert on public.crash_reports;
create policy crash_reports_insert on public.crash_reports
  for insert to anon, authenticated with check (true);

drop policy if exists crash_reports_admin_select on public.crash_reports;
create policy crash_reports_admin_select on public.crash_reports
  for select to authenticated using (public.is_admin());
