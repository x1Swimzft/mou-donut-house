-- Phase 5 E2E fixture (manual, not part of the rolling-back test suite).
-- Same data as phase5-evidence/ACCEPTANCE.md:
--   bill A: closed today, 7.00 (1 order, สังขยา x2)
--   bill B: open, spans yesterday + today; bill_total 17.50,
--           yesterday 7.00 (สังขยา x2) + today 10.50 (ไส้กรอก x3)
--
-- Run in the Supabase SQL editor as postgres (bypasses RLS).
-- Running the whole file = cleanup + seed, safe to re-run.
-- To clean up only, run just the CLEANUP block.
-- Business dates follow Asia/Bangkok "today", matching the app's default day.

begin;

-- ---------------------------------------------------------------------------
-- CLEANUP (fixed ids only; never touches other customers)
-- ---------------------------------------------------------------------------
delete from public.order_items
where order_id in (
  'eeeeeeee-0000-0000-0000-00000000e001',
  'eeeeeeee-0000-0000-0000-00000000e002',
  'eeeeeeee-0000-0000-0000-00000000e003'
);

delete from public.bill_items
where bill_id in (
  'eeeeeeee-0000-0000-0000-00000000b001',
  'eeeeeeee-0000-0000-0000-00000000b002'
);

delete from public.orders
where id in (
  'eeeeeeee-0000-0000-0000-00000000e001',
  'eeeeeeee-0000-0000-0000-00000000e002',
  'eeeeeeee-0000-0000-0000-00000000e003'
);

delete from public.bills
where id in (
  'eeeeeeee-0000-0000-0000-00000000b001',
  'eeeeeeee-0000-0000-0000-00000000b002'
);

delete from public.customers
where id in (
  'eeeeeeee-0000-0000-0000-00000000c001',
  'eeeeeeee-0000-0000-0000-00000000c002'
);

-- ---------------------------------------------------------------------------
-- SEED
-- ---------------------------------------------------------------------------
do $$
declare
  v_today date := (now() at time zone 'Asia/Bangkok')::date;
  v_yesterday date := v_today - 1;
  v_sausage uuid;
  v_custard uuid;
begin
  select id into v_sausage
  from public.products
  where name = 'ไส้กรอก'
  order by is_available desc, id
  limit 1;

  select id into v_custard
  from public.products
  where name = 'สังขยา'
  order by is_available desc, id
  limit 1;

  if v_sausage is null or v_custard is null then
    raise exception 'products ไส้กรอก/สังขยา missing; run supabase/seed.sql first';
  end if;

  insert into public.customers (id, name) values
    ('eeeeeeee-0000-0000-0000-00000000c001', 'ลูกค้าทดสอบ E2E ปิดวันนี้'),
    ('eeeeeeee-0000-0000-0000-00000000c002', 'ลูกค้าทดสอบ E2E ข้ามวัน');

  insert into public.bills (id, customer_id, status, opened_at, closed_at, total) values
    ('eeeeeeee-0000-0000-0000-00000000b001',
     'eeeeeeee-0000-0000-0000-00000000c001', 'closed',
     (v_today + time '08:00') at time zone 'Asia/Bangkok',
     (v_today + time '09:00') at time zone 'Asia/Bangkok',
     7.00),
    ('eeeeeeee-0000-0000-0000-00000000b002',
     'eeeeeeee-0000-0000-0000-00000000c002', 'open',
     (v_yesterday + time '09:00') at time zone 'Asia/Bangkok',
     null,
     17.50);

  insert into public.orders (id, customer_id, total, bill_id, business_date, created_at) values
    ('eeeeeeee-0000-0000-0000-00000000e001',
     'eeeeeeee-0000-0000-0000-00000000c001', 7.00,
     'eeeeeeee-0000-0000-0000-00000000b001', v_today,
     (v_today + time '08:30') at time zone 'Asia/Bangkok'),
    ('eeeeeeee-0000-0000-0000-00000000e002',
     'eeeeeeee-0000-0000-0000-00000000c002', 7.00,
     'eeeeeeee-0000-0000-0000-00000000b002', v_yesterday,
     (v_yesterday + time '09:30') at time zone 'Asia/Bangkok'),
    ('eeeeeeee-0000-0000-0000-00000000e003',
     'eeeeeeee-0000-0000-0000-00000000c002', 10.50,
     'eeeeeeee-0000-0000-0000-00000000b002', v_today,
     (v_today + time '10:00') at time zone 'Asia/Bangkok');

  insert into public.order_items (order_id, product_id, product_name, unit_price, qty) values
    ('eeeeeeee-0000-0000-0000-00000000e001', v_custard, 'สังขยา', 3.50, 2),
    ('eeeeeeee-0000-0000-0000-00000000e002', v_custard, 'สังขยา', 3.50, 2),
    ('eeeeeeee-0000-0000-0000-00000000e003', v_sausage, 'ไส้กรอก', 3.50, 3);

  insert into public.bill_items (bill_id, product_id, product_name, unit_price, qty) values
    ('eeeeeeee-0000-0000-0000-00000000b001', v_custard, 'สังขยา', 3.50, 2),
    ('eeeeeeee-0000-0000-0000-00000000b002', v_custard, 'สังขยา', 3.50, 2),
    ('eeeeeeee-0000-0000-0000-00000000b002', v_sausage, 'ไส้กรอก', 3.50, 3)
  on conflict (bill_id, product_id) do update
    set qty = excluded.qty;
end $$;

commit;

-- ---------------------------------------------------------------------------
-- VERIFY (raw tables; the app should show 7.00 closed / 17.50 day total)
-- ---------------------------------------------------------------------------
select
  b.id as bill_id,
  b.status,
  b.total as bill_total,
  coalesce(sum(o.total) filter (
    where o.business_date = (now() at time zone 'Asia/Bangkok')::date
  ), 0) as day_total,
  count(o.id) filter (
    where o.business_date = (now() at time zone 'Asia/Bangkok')::date
  ) as day_orders
from public.bills b
left join public.orders o on o.bill_id = b.id
where b.id in (
  'eeeeeeee-0000-0000-0000-00000000b001',
  'eeeeeeee-0000-0000-0000-00000000b002'
)
group by b.id, b.status, b.total
order by b.status desc;
