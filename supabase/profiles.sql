-- ============================================================================
-- KeepCoding · Supabase Auth — schema `profiles`, trigger e RLS
-- ----------------------------------------------------------------------------
-- Rode no SQL Editor do Supabase (Dashboard → SQL Editor → New query).
-- Idempotente: pode rodar mais de uma vez sem quebrar.
-- ============================================================================

-- 1) Tabela de perfis (1:1 com auth.users) -----------------------------------
create table if not exists public.profiles (
  id          uuid        primary key references auth.users (id) on delete cascade,
  email       text        not null,
  full_name   text,
  avatar_url  text,
  created_at  timestamptz not null default now()
);

comment on table public.profiles is
  'Perfil público do usuário, 1:1 com auth.users. Criado por trigger no signup.';

-- 2) Trigger: cria o profile automaticamente no primeiro login social --------
--    SECURITY DEFINER → roda como owner e contorna RLS na inserção.
--    Lê metadados que Google/Apple/GitHub gravam em raw_user_meta_data.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, full_name, avatar_url)
  values (
    new.id,
    new.email,
    coalesce(
      new.raw_user_meta_data ->> 'full_name',
      new.raw_user_meta_data ->> 'name',
      new.raw_user_meta_data ->> 'user_name'
    ),
    coalesce(
      new.raw_user_meta_data ->> 'avatar_url',
      new.raw_user_meta_data ->> 'picture'
    )
  )
  on conflict (id) do nothing;  -- proteção contra reexecução / corrida
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;

create trigger on_auth_user_created
  after insert on auth.users
  for each row
  execute function public.handle_new_user();

-- 3) Row Level Security ------------------------------------------------------
alter table public.profiles enable row level security;

-- 3a) Qualquer usuário AUTENTICADO pode ler perfis.
drop policy if exists "profiles_select_authenticated" on public.profiles;
create policy "profiles_select_authenticated"
  on public.profiles
  for select
  to authenticated
  using (true);

-- 3b) Usuário só atualiza o PRÓPRIO perfil.
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
  on public.profiles
  for update
  to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- (Sem policy de INSERT/DELETE para `authenticated`: inserção é só via trigger
--  SECURITY DEFINER; delete cascata vem de auth.users.)
