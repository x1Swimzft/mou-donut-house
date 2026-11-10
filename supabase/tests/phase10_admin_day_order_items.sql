-- Phase 10: admin_day_order_items acceptance tests
-- Runs in one transaction and rolls back; database left unchanged.

begin;

create temp table _phase10_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

-- Set up test data
insert into public.customers (id, name) values
  ('eeeeeeee-0000-0000-0000-00000000c001', 'ลูกค้า ต้า'),
  ('eeeeeeee-0000-0000-0000-00000000c002', 'ลูกค้า นัท');

-- Product test
insert into public.products (id, name, price, category, is_available) values
  ('eeeeeeee-0000-0000-0000-00000000a001', 'ไส้กรอก', 3.50, 'donut', true),
  ('eeeeeeee-0000-0000-0000-00000000a002', 'หมูหยอง', 3.50, 'donut', true)
on conflict (id) do update set name = excluded.name;

-- Customer A (ต้า): 1 bill, 1 order (ไส้กรอก x5)
insert into public.bills (id, customer_id, status, opened_at, total) values
  ('eeeeeeee-0000-0000-0000-00000000b001', 'eeeeeeee-0000-0000-0000-00000000c001', 'open', now(), 17.50);

insert into public.orders (id, customer_id, total, bill_id, business_date) values
  ('eeeeeeee-0000-0000-0000-00000000e001', 'eeeeeeee-0000-0000-0000-00000000c001', 17.50, 'eeeeeeee-0000-0000-0000-00000000b001', '2099-11-10');

insert into public.order_items (order_id, product_id, product_name, unit_price, qty) values
  ('eeeeeeee-0000-0000-0000-00000000e001', 'eeeeeeee-0000-0000-0000-00000000a001', 'ไส้กรอก', 3.50, 5);

-- Customer B (นัท): 1 bill, 1 order (ไส้กรอก x10, หมูหยอง x20)
insert into public.bills (id, customer_id, status, opened_at, total) values
  ('eeeeeeee-0000-0000-0000-00000000b002', 'eeeeeeee-0000-0000-0000-00000000c002', 'open', now(), 105.00);

insert into public.orders (id, customer_id, total, bill_id, business_date) values
  ('eeeeeeee-0000-0000-0000-00000000e002', 'eeeeeeee-0000-0000-0000-00000000c002', 105.00, 'eeeeeeee-0000-0000-0000-00000000b002', '2099-11-10');

insert into public.order_items (order_id, product_id, product_name, unit_price, qty) values
  ('eeeeeeee-0000-0000-0000-00000000e002', 'eeeeeeee-0000-0000-0000-00000000a001', 'ไส้กรอก', 3.50, 10),
  ('eeeeeeee-0000-0000-0000-00000000e002', 'eeeeeeee-0000-0000-0000-00000000a002', 'หมูหยอง', 3.50, 20);

-- Customer A (ต้า): 2nd order in same day (1-2 hours later): ไส้กรอก x5 additional
insert into public.orders (id, customer_id, total, bill_id, business_date) values
  ('eeeeeeee-0000-0000-0000-00000000e003', 'eeeeeeee-0000-0000-0000-00000000c001', 17.50, 'eeeeeeee-0000-0000-0000-00000000b001', '2099-11-10');

insert into public.order_items (order_id, product_id, product_name, unit_price, qty) values
  ('eeeeeeee-0000-0000-0000-00000000e003', 'eeeeeeee-0000-0000-0000-00000000a001', 'ไส้กรอก', 3.50, 5);

-- Set admin context
insert into auth.users (id) values ('eeeeeeee-0000-0000-0000-00000000ad01');
insert into public.admins (user_id) values ('eeeeeeee-0000-0000-0000-00000000ad01');
select set_config('request.jwt.claim.sub', 'eeeeeeee-0000-0000-0000-00000000ad01', true);
select set_config('request.jwt.claims', jsonb_build_object('sub', 'eeeeeeee-0000-0000-0000-00000000ad01')::text, true);

-- Case 1: admin_day_order_items consolidates ไส้กรอก (ต้า 5+5 + นัท 10 = 20 pieces) and หมูหยอง (20 pieces)
do $$
declare
  v_res jsonb;
  v_sausage jsonb;
  v_pork jsonb;
begin
  v_res := public.admin_day_order_items('2099-11-10');
  
  select value into v_sausage
  from jsonb_array_elements(v_res)
  where value ->> 'product_name' = 'ไส้กรอก';

  select value into v_pork
  from jsonb_array_elements(v_res)
  where value ->> 'product_name' = 'หมูหยอง';

  insert into _phase10_results values (
    1,
    'consolidate_sausage_qty',
    (v_sausage ->> 'total_qty')::int = 20 and (v_sausage ->> 'total_amount')::numeric = 70.00,
    format('sausage qty=%s amount=%s', v_sausage ->> 'total_qty', v_sausage ->> 'total_amount')
  );

  insert into _phase10_results values (
    2,
    'consolidate_pork_qty',
    (v_pork ->> 'total_qty')::int = 20 and (v_pork ->> 'total_amount')::numeric = 70.00,
    format('pork qty=%s amount=%s', v_pork ->> 'total_qty', v_pork ->> 'total_amount')
  );

  insert into _phase10_results values (
    3,
    'customer_breakdown_exists',
    jsonb_array_length(v_sausage -> 'customer_breakdown') = 2,
    format('sausage customer breakdown count=%s', jsonb_array_length(v_sausage -> 'customer_breakdown'))
  );
end $$;

-- Case 2: non-admin gets ZZ008
do $$
declare
  v_caught boolean := false;
  v_code text := '';
begin
  perform set_config('request.jwt.claim.sub', 'eeeeeeee-0000-0000-0000-000000000999', true);
  perform set_config('request.jwt.claims', jsonb_build_object('sub', 'eeeeeeee-0000-0000-0000-000000000999')::text, true);

  begin
    perform public.admin_day_order_items('2026-11-10');
  exception when others then
    get stacked diagnostics v_code = returned_sqlstate;
    v_caught := true;
  end;

  insert into _phase10_results values (
    4,
    'reject_non_admin',
    v_caught and v_code = 'ZZ008',
    format('caught=%s code=%s', v_caught, v_code)
  );
end $$;

select case_no, case_name, ok, detail from _phase10_results order by case_no;

rollback;
