-- ============================================================================
--  QuranLearner — Supabase cloud backup schema
--
--  Run this once per project: Supabase dashboard -> SQL Editor -> New query ->
--  paste the whole file -> Run. It is idempotent, so re-running is safe.
--
--  Design notes
--  ------------
--  * Every table is private to one user: RLS restricts rows to auth.uid().
--  * `updated_at` (epoch millis) is written by the app and used for
--    last-write-wins conflict resolution between devices.
--  * `server_updated_at` is set by a trigger on every insert/update and is
--    used ONLY as the incremental pull cursor, so device clock skew cannot
--    make changes invisible.
--  * `deleted` marks tombstones (currently only favorites are deleted).
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
--  1. Tables
-- ────────────────────────────────────────────────────────────────────────────

create table if not exists public.user_settings (
    user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    key               text        not null,
    value             text        not null,
    updated_at        bigint      not null default 0,
    deleted           boolean     not null default false,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, key)
);

create table if not exists public.daily_progress (
    user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    date              text        not null,
    ayahs_learned     integer     not null default 0,
    goal              integer     not null default 5,
    updated_at        bigint      not null default 0,
    deleted           boolean     not null default false,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, date)
);

create table if not exists public.ayah_progress (
    user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    surah_number      integer     not null,
    ayah_number       integer     not null,
    learned_at        text        not null default '',
    updated_at        bigint      not null default 0,
    deleted           boolean     not null default false,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, surah_number, ayah_number)
);

create table if not exists public.achievements (
    user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    id                text        not null,
    unlocked_at       text        not null default '',
    updated_at        bigint      not null default 0,
    deleted           boolean     not null default false,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id)
);

create table if not exists public.ayah_reviews (
    user_id           uuid             not null default auth.uid() references auth.users (id) on delete cascade,
    surah_number      integer          not null,
    ayah_number       integer          not null,
    next_review_date  text             not null default '',
    interval_days     integer          not null default 1,
    ease_factor       double precision not null default 2.5,
    updated_at        bigint           not null default 0,
    deleted           boolean          not null default false,
    server_updated_at timestamptz      not null default now(),
    primary key (user_id, surah_number, ayah_number)
);

create table if not exists public.favorites (
    user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    surah_number      integer     not null,
    ayah_number       integer     not null,
    added_at          text        not null default '',
    updated_at        bigint      not null default 0,
    deleted           boolean     not null default false,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, surah_number, ayah_number)
);

-- ────────────────────────────────────────────────────────────────────────────
--  2. server_updated_at trigger (authoritative pull cursor)
-- ────────────────────────────────────────────────────────────────────────────

create or replace function public.touch_server_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.server_updated_at := now();
    return new;
end;
$$;

do $$
declare
    t text;
begin
    foreach t in array array[
        'user_settings', 'daily_progress', 'ayah_progress',
        'achievements', 'ayah_reviews', 'favorites'
    ] loop
        execute format('drop trigger if exists trg_touch_server_updated_at on public.%I', t);
        execute format(
            'create trigger trg_touch_server_updated_at before insert or update on public.%I '
            'for each row execute function public.touch_server_updated_at()', t);
    end loop;
end
$$;

-- ────────────────────────────────────────────────────────────────────────────
--  3. Indexes for the pull query (server_updated_at > cursor)
-- ────────────────────────────────────────────────────────────────────────────

create index if not exists user_settings_pull_idx  on public.user_settings  (user_id, server_updated_at);
create index if not exists daily_progress_pull_idx on public.daily_progress (user_id, server_updated_at);
create index if not exists ayah_progress_pull_idx  on public.ayah_progress  (user_id, server_updated_at);
create index if not exists achievements_pull_idx   on public.achievements   (user_id, server_updated_at);
create index if not exists ayah_reviews_pull_idx   on public.ayah_reviews   (user_id, server_updated_at);
create index if not exists favorites_pull_idx      on public.favorites      (user_id, server_updated_at);

-- ────────────────────────────────────────────────────────────────────────────
--  4. Row level security: each signed-in user sees only their own rows
-- ────────────────────────────────────────────────────────────────────────────

do $$
declare
    t text;
begin
    foreach t in array array[
        'user_settings', 'daily_progress', 'ayah_progress',
        'achievements', 'ayah_reviews', 'favorites'
    ] loop
        execute format('alter table public.%I enable row level security', t);
        execute format('drop policy if exists "Users manage own rows" on public.%I', t);
        execute format(
            'create policy "Users manage own rows" on public.%I for all to authenticated '
            'using (auth.uid() = user_id) with check (auth.uid() = user_id)', t);
        execute format('grant select, insert, update, delete on public.%I to authenticated', t);
        execute format('revoke all on public.%I from anon', t);
    end loop;
end
$$;
