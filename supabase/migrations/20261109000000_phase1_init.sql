create table public.admins (
  user_id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table public.products (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  price numeric(10,2) not null check (price > 0),
  category text,
  is_available boolean not null default true
);

create table public.customers (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text,
  created_at timestamptz not null default now()
);

create table public.access_codes (
  code text primary key,
  customer_id uuid not null unique references public.customers(id) on delete cascade,
  is_active boolean not null default true,
  first_used_at timestamptz
);

create table public.orders (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.customers(id),
  total numeric(10,2) not null check (total >= 0),
  status text not null default 'placed' check (status in ('placed','cancelled')),
  created_at timestamptz not null default now()
);

create table public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders(id) on delete cascade,
  product_id uuid not null references public.products(id),
  product_name text not null,
  unit_price numeric(10,2) not null check (unit_price > 0),
  qty integer not null check (qty > 0)
);

create index order_items_order_id_idx on public.order_items (order_id);
create index orders_customer_id_idx on public.orders (customer_id);

alter table public.admins enable row level security;
alter table public.products enable row level security;
alter table public.customers enable row level security;
alter table public.access_codes enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- security definer: admins is RLS-protected, so a plain subquery from the caller would always be empty
create function public.is_admin() returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.admins where user_id = (select auth.uid())
  );
$$;

revoke all on function public.is_admin() from public;
grant execute on function public.is_admin() to authenticated;

create policy products_select_public on public.products
  for select to anon, authenticated
  using (is_available = true);

create policy products_select_admin on public.products
  for select to authenticated
  using (public.is_admin());

create policy products_insert_admin on public.products
  for insert to authenticated
  with check (public.is_admin());

create policy products_update_admin on public.products
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy products_delete_admin on public.products
  for delete to authenticated
  using (public.is_admin());

create policy admins_select_self on public.admins
  for select to authenticated
  using (user_id = (select auth.uid()));

create policy customers_select_admin on public.customers
  for select to authenticated
  using (public.is_admin());

create policy access_codes_select_admin on public.access_codes
  for select to authenticated
  using (public.is_admin());

create policy orders_select_admin on public.orders
  for select to authenticated
  using (public.is_admin());

create policy order_items_select_admin on public.order_items
  for select to authenticated
  using (public.is_admin());

create or replace function public.create_order(p_code text, p_items jsonb) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item jsonb;
  v_qty numeric;
  v_code public.access_codes;
  v_order_id uuid;
  v_total numeric;
begin
  if jsonb_typeof(p_items) is distinct from 'array' then
    raise exception using errcode = 'ZZ005', message = 'empty_cart';
  end if;

  if jsonb_array_length(p_items) = 0 then
    raise exception using errcode = 'ZZ005', message = 'empty_cart';
  end if;

  for v_item in select value from jsonb_array_elements(p_items) loop
    if jsonb_typeof(v_item) <> 'object' or jsonb_typeof(v_item -> 'qty') is distinct from 'number' then
      raise exception using errcode = 'ZZ004', message = 'invalid_qty';
    end if;

    v_qty := (v_item ->> 'qty')::numeric;

    if v_qty < 1 or v_qty <> trunc(v_qty) then
      raise exception using errcode = 'ZZ004', message = 'invalid_qty';
    end if;
  end loop;

  if exists (
    select 1
    from jsonb_array_elements(p_items) e
    where jsonb_typeof(e -> 'product_id') is distinct from 'string'
       or (e ->> 'product_id') !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
  ) then
    raise exception using errcode = 'ZZ003', message = 'product_unavailable';
  end if;

  select * into v_code from public.access_codes where code = p_code;
  if not found then
    raise exception using errcode = 'ZZ001', message = 'invalid_code';
  end if;

  if not v_code.is_active then
    raise exception using errcode = 'ZZ002', message = 'code_inactive';
  end if;

  -- lock available products so admin edits (price/availability) block until this order commits
  perform 1
  from public.products p
  where p.id in (
    select (e ->> 'product_id')::uuid
    from jsonb_array_elements(p_items) e
  )
    and p.is_available
  for share;

  if exists (
    select 1
    from (
      select distinct (e ->> 'product_id')::uuid as product_id
      from jsonb_array_elements(p_items) e
    ) a
    left join public.products p on p.id = a.product_id
    where p.id is null or p.is_available = false
  ) then
    raise exception using errcode = 'ZZ003', message = 'product_unavailable';
  end if;

  select coalesce(sum(p.price * a.qty), 0)
  into v_total
  from (
    select (e ->> 'product_id')::uuid as product_id, sum((e ->> 'qty')::int)::int as qty
    from jsonb_array_elements(p_items) e
    group by 1
  ) a
  join public.products p on p.id = a.product_id;

  insert into public.orders (customer_id, total)
  values (v_code.customer_id, v_total)
  returning id into v_order_id;

  insert into public.order_items (order_id, product_id, product_name, unit_price, qty)
  select v_order_id, p.id, p.name, p.price, a.qty
  from (
    select (e ->> 'product_id')::uuid as product_id, sum((e ->> 'qty')::int)::int as qty
    from jsonb_array_elements(p_items) e
    group by 1
  ) a
  join public.products p on p.id = a.product_id;

  update public.access_codes
  set first_used_at = now()
  where code = p_code and first_used_at is null;

  return jsonb_build_object('order_id', v_order_id, 'total', v_total);
end;
$$;

revoke all on function public.create_order(text, jsonb) from public;
grant execute on function public.create_order(text, jsonb) to anon, authenticated;
