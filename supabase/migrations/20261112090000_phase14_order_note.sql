-- Phase 14: optional note from the customer to the shop, stored per order.
-- New error code: ZZ014 note_invalid (note longer than 200 characters).
-- create_order keeps every previous behaviour. The three-argument function holds
-- the logic; the original two-argument signature stays as a delegating wrapper so
-- existing SQL tests and older web clients keep working without overloading.

alter table public.orders
  add column if not exists note text;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conrelid = 'public.orders'::regclass
      and conname = 'orders_note_length'
  ) then
    alter table public.orders
      add constraint orders_note_length
      check (note is null or char_length(note) between 1 and 200);
  end if;
end $$;

comment on column public.orders.note is
  'ข้อความกำกับจากลูกค้าถึงร้าน ต่อการยืนยัน 1 ครั้ง (ไม่เกิน 200 ตัวอักษร)';

create or replace function public.create_order(p_code text, p_items jsonb, p_note text)
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
  v_note text;
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

  v_note := nullif(btrim(p_note), '');

  if v_note is not null and char_length(v_note) > 200 then
    raise exception using errcode = 'ZZ014', message = 'note_invalid';
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

  insert into public.orders (customer_id, total, bill_id, business_date, note)
  values (v_code.customer_id, v_total, v_bill_id, (now() at time zone 'Asia/Bangkok')::date, v_note)
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

revoke all on function public.create_order(text, jsonb, text) from public;
grant execute on function public.create_order(text, jsonb, text) to anon, authenticated;

-- Backward-compatible wrapper: existing two-argument callers keep working and
-- store the order without a note. The wrapper delegates so the logic lives once.
create or replace function public.create_order(p_code text, p_items jsonb)
returns jsonb
language sql
security definer
set search_path = ''
as $$
  select public.create_order(p_code, p_items, null);
$$;

revoke all on function public.create_order(text, jsonb) from public;
grant execute on function public.create_order(text, jsonb) to anon, authenticated;
