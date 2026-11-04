-- Cookr — initial Supabase schema
-- Run in the Supabase SQL editor on a fresh project.
-- Assumes Supabase Auth (auth.users) is the identity source.

-- ---------------------------------------------------------------
-- Extensions
-- ---------------------------------------------------------------
create extension if not exists "pgcrypto";
create extension if not exists "pg_trgm";

-- ---------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------
create type meal_slot as enum ('breakfast', 'lunch', 'dinner', 'snack');

-- ---------------------------------------------------------------
-- profiles — one row per account, created automatically on signup
-- ---------------------------------------------------------------
create table profiles (
  id          uuid primary key references auth.users on delete cascade,
  display_name text,
  avatar_url  text,
  created_at  timestamptz not null default now()
);

create or replace function handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)));
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

-- ---------------------------------------------------------------
-- source_cache — shared, deduplicated result of parsing a recipe URL.
-- Not owned by any user: parse a URL once, every account benefits.
-- Written only by the edge function (service role).
-- ---------------------------------------------------------------
create table source_cache (
  url_hash    text primary key,              -- sha256 of the normalized URL
  url         text not null,
  normalized_url text not null,
  payload     jsonb not null,                -- parsed recipe, pre-import shape
  parser      text not null,                 -- 'json-ld' | 'microdata' | 'llm'
  fetched_at  timestamptz not null default now()
);

create index source_cache_normalized_url_idx on source_cache (normalized_url);

-- ---------------------------------------------------------------
-- recipes
-- ---------------------------------------------------------------
create table recipes (
  id            uuid primary key default gen_random_uuid(),
  owner_id      uuid not null references profiles(id) on delete cascade,
  title         text not null,
  description   text,
  source_url    text,
  source_name   text,                        -- e.g. "seriouseats.com"
  image_path    text,                        -- path in the 'recipe-images' storage bucket
  servings      int,
  prep_minutes  int,
  cook_minutes  int,
  total_minutes int,
  notes         text,
  is_favorite   boolean not null default false,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

create index recipes_owner_idx on recipes (owner_id, created_at desc);
create index recipes_title_trgm_idx on recipes using gin (title gin_trgm_ops);

-- ---------------------------------------------------------------
-- recipe_ingredients
-- Both the original line AND a parsed split. The split is what makes
-- grocery-list aggregation possible; raw_text is the fallback for
-- display and for lines that don't parse cleanly.
-- ---------------------------------------------------------------
create table recipe_ingredients (
  id          uuid primary key default gen_random_uuid(),
  recipe_id   uuid not null references recipes(id) on delete cascade,
  position    int not null,
  raw_text    text not null,
  quantity    numeric,                       -- null when "to taste"
  unit        text,                          -- normalized: g, ml, tsp, tbsp, cup, oz, lb, clove...
  item        text,                          -- "garlic", "olive oil"
  note        text,                          -- "finely chopped", "divided"
  section     text,                          -- "For the sauce"
  unique (recipe_id, position)
);

create index recipe_ingredients_recipe_idx on recipe_ingredients (recipe_id, position);
create index recipe_ingredients_item_idx on recipe_ingredients (item);

-- ---------------------------------------------------------------
-- recipe_steps
-- ---------------------------------------------------------------
create table recipe_steps (
  id          uuid primary key default gen_random_uuid(),
  recipe_id   uuid not null references recipes(id) on delete cascade,
  position    int not null,
  instruction text not null,
  section     text,
  unique (recipe_id, position)
);

create index recipe_steps_recipe_idx on recipe_steps (recipe_id, position);

-- ---------------------------------------------------------------
-- tags
-- ---------------------------------------------------------------
create table recipe_tags (
  recipe_id uuid not null references recipes(id) on delete cascade,
  tag       text not null,
  primary key (recipe_id, tag)
);

create index recipe_tags_tag_idx on recipe_tags (tag);

-- ---------------------------------------------------------------
-- meal_plans — one per week (or any arbitrary span)
-- ---------------------------------------------------------------
create table meal_plans (
  id         uuid primary key default gen_random_uuid(),
  owner_id   uuid not null references profiles(id) on delete cascade,
  name       text,
  week_start date not null,
  created_at timestamptz not null default now(),
  unique (owner_id, week_start)
);

create index meal_plans_owner_idx on meal_plans (owner_id, week_start desc);

create table meal_plan_entries (
  id              uuid primary key default gen_random_uuid(),
  meal_plan_id    uuid not null references meal_plans(id) on delete cascade,
  recipe_id       uuid not null references recipes(id) on delete cascade,
  scheduled_date  date not null,
  slot            meal_slot not null default 'dinner',
  servings        int,                        -- overrides recipes.servings when set
  position        int not null default 0
);

create index meal_plan_entries_plan_idx on meal_plan_entries (meal_plan_id, scheduled_date);

-- ---------------------------------------------------------------
-- grocery_lists — generated from a meal plan, then hand-editable
-- ---------------------------------------------------------------
create table grocery_lists (
  id           uuid primary key default gen_random_uuid(),
  owner_id     uuid not null references profiles(id) on delete cascade,
  meal_plan_id uuid references meal_plans(id) on delete set null,
  name         text,
  created_at   timestamptz not null default now()
);

create index grocery_lists_owner_idx on grocery_lists (owner_id, created_at desc);

create table grocery_items (
  id              uuid primary key default gen_random_uuid(),
  grocery_list_id uuid not null references grocery_lists(id) on delete cascade,
  item            text not null,
  quantity        numeric,
  unit            text,
  category        text,                       -- produce, dairy, pantry... for aisle grouping
  is_checked      boolean not null default false,
  is_manual       boolean not null default false,
  source_recipe_ids uuid[] not null default '{}',
  position        int not null default 0
);

create index grocery_items_list_idx on grocery_items (grocery_list_id, category, position);

-- ---------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------
create or replace function touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger recipes_touch_updated_at
  before update on recipes
  for each row execute function touch_updated_at();

-- ---------------------------------------------------------------
-- Row Level Security
-- Every owned table: you see and change only your own rows.
-- Child tables inherit ownership through their parent recipe/plan/list.
-- ---------------------------------------------------------------
alter table profiles            enable row level security;
alter table recipes             enable row level security;
alter table recipe_ingredients  enable row level security;
alter table recipe_steps        enable row level security;
alter table recipe_tags         enable row level security;
alter table meal_plans          enable row level security;
alter table meal_plan_entries   enable row level security;
alter table grocery_lists       enable row level security;
alter table grocery_items       enable row level security;
alter table source_cache        enable row level security;

create policy "own profile" on profiles
  for all using (id = auth.uid()) with check (id = auth.uid());

create policy "own recipes" on recipes
  for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

create policy "own recipe ingredients" on recipe_ingredients
  for all using (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()))
  with check (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()));

create policy "own recipe steps" on recipe_steps
  for all using (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()))
  with check (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()));

create policy "own recipe tags" on recipe_tags
  for all using (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()))
  with check (exists (select 1 from recipes r where r.id = recipe_id and r.owner_id = auth.uid()));

create policy "own meal plans" on meal_plans
  for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

create policy "own meal plan entries" on meal_plan_entries
  for all using (exists (select 1 from meal_plans p where p.id = meal_plan_id and p.owner_id = auth.uid()))
  with check (exists (select 1 from meal_plans p where p.id = meal_plan_id and p.owner_id = auth.uid()));

create policy "own grocery lists" on grocery_lists
  for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

create policy "own grocery items" on grocery_items
  for all using (exists (select 1 from grocery_lists l where l.id = grocery_list_id and l.owner_id = auth.uid()))
  with check (exists (select 1 from grocery_lists l where l.id = grocery_list_id and l.owner_id = auth.uid()));

-- Parse cache: any signed-in user may read it; only the edge function
-- (service role, which bypasses RLS) writes to it.
create policy "read parse cache" on source_cache
  for select using (auth.role() = 'authenticated');
