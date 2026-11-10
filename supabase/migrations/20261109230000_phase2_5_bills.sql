-- Phase 2.5: bills, bill_items, order bill linkage
-- Apply after 20261109211000_phase2_thai_product_names.sql

-- access_codes: one customer may have multiple codes; optional label
alter table public.access_codes
  add column if not exists label text;

do $$
declare
  v_attnum smallint;
  v_conname text;
begin
  select attnum
  into v_attnum
  from pg_attribute
  where attrelid = 'public.access_codes'::regclass
    and attname = 'customer_id'
    and not attisdropped;

  select conname
  into v_conname
  from pg_constraint
  where conrelid = 'public.access_codes'::regclass
    and contype = 'u'
    and conkey = array[v_attnum];

  if v_conname is not null then
    execute format('alter table public.access_codes drop constraint %I', v_conname);
  end if;
end $$;

create table if not exists public.bills (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.customers(id),
  status text not null default 'open' check (status in ('open', 'closed')),
  opened_at timestamptz not null default now(),
  closed_at timestamptz,
  total numeric(10,2) not null default 0 check (total >= 0)
);

create unique index if not exists bills_one_open_per_customer
  on public.bills (customer_id)
  where status = 'open';

create table if not exists public.bill_items (
  bill_id uuid not null references public.bills(id) on delete cascade,
  product_id uuid not null references public.products(id),
  product_name text not null,
  unit_price numeric(10,2) not null check (unit_price > 0),
  qty integer not null check (qty > 0),
  primary key (bill_id, product_id)
);

alter table public.orders
  add column if not exists bill_id uuid references public.bills(id);

alter table public.orders
  add column if not exists business_date date;

-- Backfill existing orders: one closed bill per existing order.
do $$
declare
  r record;
  v_bill_id uuid;
  v_migrated int := 0;
begin
  for r in
    select o.id, o.customer_id, o.total, o.created_at
    from public.orders o
    where o.bill_id is null
    order by o.created_at, o.id
  loop
    insert into public.bills (customer_id, status, opened_at, closed_at, total)
    values (r.customer_id, 'closed', r.created_at, r.created_at, r.total)
    returning id into v_bill_id;

    update public.orders
    set bill_id = v_bill_id,
        business_date = coalesce(business_date, r.created_at::date)
    where id = r.id;

    insert into public.bill_items as bi (bill_id, product_id, product_name, unit_price, qty)
    select v_bill_id, oi.product_id, min(oi.product_name), min(oi.unit_price), sum(oi.qty)::int
    from public.order_items oi
    where oi.order_id = r.id
    group by oi.product_id
    on conflict (bill_id, product_id) do update
      set qty = bi.qty + excluded.qty;

    v_migrated := v_migrated + 1;
  end loop;

  raise notice 'phase2_5 backfill: % order(s) migrated to closed bills', v_migrated;
end $$;

-- Defensive: every order gets a business date before the NOT NULL constraint.
update public.orders
set business_date = created_at::date
where business_date is null;

alter table public.orders
  alter column business_date set default current_date;

alter table public.orders
  alter column business_date set not null;

alter table public.bills enable row level security;
alter table public.bill_items enable row level security;

revoke all on public.bills from public;
revoke all on public.bills from anon;
revoke all on public.bill_items from public;
revoke all on public.bill_items from anon;

grant select on public.bills to authenticated;
grant select on public.bill_items to authenticated;

drop policy if exists bills_select_admin on public.bills;
create policy bills_select_admin on public.bills
  for select to authenticated
  using (public.is_admin());

drop policy if exists bill_items_select_admin on public.bill_items;
create policy bill_items_select_admin on public.bill_items
  for select to authenticated
  using (public.is_admin());

-- ---------------------------------------------------------------------------
-- create_order: unchanged input/output/error codes, now creates/uses a bill
-- ---------------------------------------------------------------------------
create or replace function public.create_order(p_code text, p_items jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item jsonb;
  v_qty numeric;
  v_code public.access_codes;
  v_bill public.bills;
  v_order_id uuid;
  v_bill_id uuid;
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

  -- serialize orders per customer so only one open bill is created
  perform 1
  from public.customers
  where id = v_code.customer_id
  for update;

  select * into v_bill
  from public.bills
  where customer_id = v_code.customer_id
    and status = 'open'
  for update;

  if v_bill.id is null then
    insert into public.bills (customer_id, status)
    values (v_code.customer_id, 'open')
    returning * into v_bill;
  end if;

  v_bill_id := v_bill.id;

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

  insert into public.orders (customer_id, total, bill_id)
  values (v_code.customer_id, v_total, v_bill_id)
  returning id into v_order_id;

  insert into public.order_items (order_id, product_id, product_name, unit_price, qty)
  select v_order_id, p.id, p.name, p.price, a.qty
  from (
    select (e ->> 'product_id')::uuid as product_id, sum((e ->> 'qty')::int)::int as qty
    from jsonb_array_elements(p_items) e
    group by 1
  ) a
  join public.products p on p.id = a.product_id;

  update public.bills
  set total = total + v_total
  where id = v_bill_id;

  insert into public.bill_items as bi (bill_id, product_id, product_name, unit_price, qty)
  select v_bill_id, p.id, p.name, p.price, a.qty
  from (
    select (e ->> 'product_id')::uuid as product_id, sum((e ->> 'qty')::int)::int as qty
    from jsonb_array_elements(p_items) e
    group by 1
  ) a
  join public.products p on p.id = a.product_id
  on conflict (bill_id, product_id) do update
    set qty = bi.qty + excluded.qty;

  update public.access_codes
  set first_used_at = now()
  where code = p_code and first_used_at is null;

  return jsonb_build_object('order_id', v_order_id, 'total', v_total);
end;
$$;

revoke all on function public.create_order(text, jsonb) from public;
grant execute on function public.create_order(text, jsonb) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- my_bills: customer-facing history for the customer behind the code
-- ---------------------------------------------------------------------------
create or replace function public.my_bills(p_code text)
returns table (
  bill_id uuid,
  status text,
  business_date date,
  opened_at timestamptz,
  closed_at timestamptz,
  total numeric,
  items jsonb
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_customer_id uuid;
begin
  perform public.verify_access_code(p_code);

  select a.customer_id
  into v_customer_id
  from public.access_codes a
  where a.code = p_code;

  return query
  select
    b.id as bill_id,
    b.status,
    coalesce(
      (select min(o.business_date) from public.orders o where o.bill_id = b.id),
      b.opened_at::date
    ) as business_date,
    b.opened_at,
    b.closed_at,
    b.total,
    coalesce((
      select jsonb_agg(
        jsonb_build_object(
          'product_name', bi.product_name,
          'qty', bi.qty,
          'line_total', bi.unit_price * bi.qty
        )
        order by bi.product_name
      )
      from public.bill_items bi
      where bi.bill_id = b.id
    ), '[]'::jsonb) as items
  from public.bills b
  where b.customer_id = v_customer_id
  order by b.opened_at desc;
end;
$$;

revoke all on function public.my_bills(text) from public;
grant execute on function public.my_bills(text) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- close_bill: admin only; closed bills are a no-op
-- ---------------------------------------------------------------------------
create or replace function public.close_bill(p_bill_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_bill public.bills;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  select * into v_bill
  from public.bills
  where id = p_bill_id
  for update;

  if v_bill.id is null then
    raise exception using errcode = 'ZZ006', message = 'bill_not_open';
  end if;

  if v_bill.status = 'open' then
    update public.bills
    set status = 'closed',
        closed_at = now()
    where id = p_bill_id
    returning * into v_bill;
  end if;

  return jsonb_build_object(
    'bill_id', v_bill.id,
    'status', v_bill.status,
    'closed_at', v_bill.closed_at,
    'total', v_bill.total
  );
end;
$$;

revoke all on function public.close_bill(uuid) from public;
grant execute on function public.close_bill(uuid) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- close_all_bills: admin only; closes only the supplied ids that are still open
-- ---------------------------------------------------------------------------
create or replace function public.close_all_bills(p_bill_ids uuid[])
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_count integer;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_bill_ids is null or array_length(p_bill_ids, 1) is null then
    return 0;
  end if;

  update public.bills
  set status = 'closed',
      closed_at = now()
  where id = any(p_bill_ids)
    and status = 'open';

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke all on function public.close_all_bills(uuid[]) from public;
grant execute on function public.close_all_bills(uuid[]) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- store_summary: admin only; closed bills and customers without orders
-- ---------------------------------------------------------------------------
create or replace function public.store_summary(p_business_date date)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_total numeric;
  v_bills jsonb;
  v_without jsonb;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_business_date is null then
    return jsonb_build_object(
      'business_date', null,
      'total', 0,
      'closed_bills', '[]'::jsonb,
      'customers_without_orders', '[]'::jsonb
    );
  end if;

  select coalesce(sum(o.total), 0)
  into v_total
  from public.orders o
  join public.bills b on b.id = o.bill_id
  where b.status = 'closed'
    and o.business_date = p_business_date;

  select coalesce(jsonb_agg(jsonb_build_object(
      'bill_id', b.id,
      'customer_id', b.customer_id,
      'customer_name', c.name,
      'total', b.total,
      'closed_at', b.closed_at
    ) order by c.name, b.closed_at), '[]'::jsonb)
  into v_bills
  from public.bills b
  join public.customers c on c.id = b.customer_id
  where b.status = 'closed'
    and exists (
      select 1
      from public.orders o
      where o.bill_id = b.id
        and o.business_date = p_business_date
    );

  select coalesce(jsonb_agg(jsonb_build_object(
      'customer_id', c.id,
      'name', c.name
    ) order by c.name), '[]'::jsonb)
  into v_without
  from public.customers c
  where not exists (
    select 1
    from public.orders o
    where o.customer_id = c.id
      and o.business_date = p_business_date
  );

  return jsonb_build_object(
    'business_date', p_business_date,
    'total', v_total,
    'closed_bills', v_bills,
    'customers_without_orders', v_without
  );
end;
$$;

revoke all on function public.store_summary(date) from public;
grant execute on function public.store_summary(date) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- register_code: set/overwrite a 1-40 character label on the caller's code
-- ---------------------------------------------------------------------------
create or replace function public.register_code(p_code text, p_label text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_label text;
begin
  perform public.verify_access_code(p_code);

  if p_label is null then
    raise exception using errcode = 'ZZ007', message = 'label_invalid';
  end if;

  v_label := btrim(p_label);

  if char_length(v_label) < 1 or char_length(v_label) > 40 then
    raise exception using errcode = 'ZZ007', message = 'label_invalid';
  end if;

  update public.access_codes
  set label = v_label
  where code = p_code;

  return jsonb_build_object('code', p_code, 'label', v_label);
end;
$$;

revoke all on function public.register_code(text, text) from public;
grant execute on function public.register_code(text, text) to anon, authenticated;
