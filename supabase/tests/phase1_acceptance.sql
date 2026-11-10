begin;

create temp table _acceptance_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name) values
  ('11111111-1111-1111-1111-111111111111', 'Test Active Customer'),
  ('22222222-2222-2222-2222-222222222222', 'Test Inactive Customer');

insert into public.access_codes (code, customer_id, is_active) values
  ('TEST-ACTIVE-001', '11111111-1111-1111-1111-111111111111', true),
  ('TEST-INACTIVE-001', '22222222-2222-2222-2222-222222222222', false);

insert into public.products (id, name, price, category, is_available) values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'Test Donut A', 40.00, 'donut', true),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'Test Drink B', 25.50, 'drink', true),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03', 'Test Donut Out', 30.00, 'donut', false);

do $$
declare
  v_res jsonb;
  v_order uuid;
  v_db_total numeric(10,2);
  v_db_status text;
  v_items int;
  v_snap_ok boolean;
  v_first_used timestamptz;
  v_ok boolean;
begin
  v_res := public.create_order(
    'TEST-ACTIVE-001',
    '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":2,"price":0.01,"unit_price":0.01},
      {"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02","qty":1}]'::jsonb);
  v_order := (v_res->>'order_id')::uuid;

  select o.total, o.status into v_db_total, v_db_status
  from public.orders o
  where o.id = v_order;

  select count(*), coalesce(bool_and(oi.unit_price = p.price and oi.product_name = p.name), false)
    into v_items, v_snap_ok
  from public.order_items oi
  join public.products p on p.id = oi.product_id
  where oi.order_id = v_order;

  select a.first_used_at into v_first_used
  from public.access_codes a
  where a.code = 'TEST-ACTIVE-001';

  v_ok := (v_res->>'total')::numeric = 105.50
      and v_db_total = 105.50
      and v_db_status = 'placed'
      and v_items = 2
      and v_snap_ok
      and v_first_used is not null;

  insert into _acceptance_results
  values (1, 'fake_price_ignored', coalesce(v_ok, false),
          format('rpc_total=%s db_total=%s status=%s items=%s snapshots=%s first_used=%s',
                 v_res->>'total', v_db_total, v_db_status, v_items, v_snap_ok, v_first_used is not null));
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('NOPE', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (2, 'invalid_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-INACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (3, 'inactive_code', v_state = 'ZZ002', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03","qty":1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (4, 'unavailable_product', v_state = 'ZZ003', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"99999999-9999-9999-9999-999999999999","qty":1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (5, 'missing_product', v_state = 'ZZ003', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":0}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (6, 'qty_zero', v_state = 'ZZ004', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":-1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (7, 'qty_negative', v_state = 'ZZ004', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":1.5}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (8, 'qty_fractional', v_state = 'ZZ004', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01","qty":"2"}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (9, 'qty_string', v_state = 'ZZ004', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (10, 'empty_cart', v_state = 'ZZ005', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', null::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (11, 'null_items', v_state = 'ZZ005', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '{}'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (12, 'object_items', v_state = 'ZZ005', 'sqlstate=' || v_state);
end $$;

do $$
declare
  v_total bigint;
  v_scoped bigint;
begin
  select count(*) into v_total from public.orders;
  select count(*) into v_scoped
  from public.orders
  where customer_id in ('11111111-1111-1111-1111-111111111111',
                        '22222222-2222-2222-2222-222222222222');
  insert into _acceptance_results
  values (13, 'no_orphan_orders', v_scoped = 1,
          format('test_customer_orders=%s total_orders=%s', v_scoped, v_total));
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.create_order('TEST-ACTIVE-001', '[{"product_id":"not-a-uuid","qty":1}]'::jsonb);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _acceptance_results values (14, 'malformed_product_id', v_state = 'ZZ003', 'sqlstate=' || v_state);
end $$;

select case_no, case_name, ok, detail
from _acceptance_results
order by case_no;

rollback;
