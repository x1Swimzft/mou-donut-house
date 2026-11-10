-- Phase 2.5 bills acceptance tests.
-- Runs in one transaction and rolls back; the database is left unchanged.
-- Admin RPCs use a temporary auth user inserted into auth.users and admins.

begin;

create temp table _bills_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name) values
  ('aaaaaaaa-0000-0000-0000-00000000a001', 'ลูกค้าทดสอบบิล A'),
  ('aaaaaaaa-0000-0000-0000-00000000b001', 'ลูกค้าทดสอบบิล B'),
  ('aaaaaaaa-0000-0000-0000-00000000c001', 'ลูกค้าทดสอบบิล C');

insert into public.access_codes (code, customer_id, is_active) values
  ('TEST-BILL-A1', 'aaaaaaaa-0000-0000-0000-00000000a001', true),
  ('TEST-BILL-A2', 'aaaaaaaa-0000-0000-0000-00000000a001', true),
  ('TEST-BILL-B1', 'aaaaaaaa-0000-0000-0000-00000000b001', true),
  ('TEST-BILL-C1', 'aaaaaaaa-0000-0000-0000-00000000c001', true);

-- Keep the three products used by the assertions available.
update public.products
set is_available = true
where name in ('ช็อคโกแลต', 'สังขยา', 'ไส้กรอก');

-- Temporary admin (rolled back at the end).
insert into auth.users (id) values ('aaaaaaaa-0000-0000-0000-00000000ad01');
insert into public.admins (user_id) values ('aaaaaaaa-0000-0000-0000-00000000ad01');

select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-00000000ad01', true);
select set_config('request.jwt.claims',
                  jsonb_build_object('sub', 'aaaaaaaa-0000-0000-0000-00000000ad01')::text,
                  true);

-- Case 1: one customer, two codes, two orders -> one open bill.
do $$
declare
  v_p1 uuid;
  v_p2 uuid;
  v_res1 jsonb;
  v_res2 jsonb;
  v_bill1 uuid;
  v_bill2 uuid;
  v_bill public.bills;
  v_orders int;
  v_items int;
begin
  select id into v_p1 from public.products where name = 'ช็อคโกแลต' limit 1;
  select id into v_p2 from public.products where name = 'สังขยา' limit 1;

  v_res1 := public.create_order(
    'TEST-BILL-A1',
    jsonb_build_array(jsonb_build_object('product_id', v_p1, 'qty', 1)));
  v_res2 := public.create_order(
    'TEST-BILL-A2',
    jsonb_build_array(jsonb_build_object('product_id', v_p2, 'qty', 1)));

  select bill_id into v_bill1
  from public.orders
  where id = (v_res1 ->> 'order_id')::uuid;

  select bill_id into v_bill2
  from public.orders
  where id = (v_res2 ->> 'order_id')::uuid;

  select * into v_bill from public.bills where id = v_bill1;

  select count(*) into v_orders from public.orders where bill_id = v_bill1;
  select count(*) into v_items from public.bill_items where bill_id = v_bill1;

  insert into _bills_results
  values (
    1,
    'two_codes_same_customer_same_bill',
    v_bill1 = v_bill2
      and v_bill.status = 'open'
      and v_bill.total = 7.50
      and v_orders = 2
      and v_items = 2,
    format('bill1=%s bill2=%s status=%s total=%s orders=%s items=%s',
           v_bill1, v_bill2, v_bill.status, v_bill.total, v_orders, v_items)
  );
end $$;

-- Case 2: after closing the first bill, a new order opens a new bill.
do $$
declare
  v_p3 uuid;
  v_old_bill uuid;
  v_old_total numeric;
  v_res jsonb;
  v_new_res jsonb;
  v_new_bill uuid;
  v_new public.bills;
  v_old_status text;
  v_old_total_after numeric;
begin
  select id into v_p3 from public.products where name = 'ไส้กรอก' limit 1;

  select id, total
  into v_old_bill, v_old_total
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000a001'
    and status = 'open';

  v_res := public.close_bill(v_old_bill);

  v_new_res := public.create_order(
    'TEST-BILL-A1',
    jsonb_build_array(jsonb_build_object('product_id', v_p3, 'qty', 1)));

  select bill_id into v_new_bill
  from public.orders
  where id = (v_new_res ->> 'order_id')::uuid;

  select * into v_new from public.bills where id = v_new_bill;

  select status, total
  into v_old_status, v_old_total_after
  from public.bills
  where id = v_old_bill;

  insert into _bills_results
  values (
    2,
    'new_bill_after_close',
    (v_res ->> 'status') = 'closed'
      and v_new_bill <> v_old_bill
      and v_new.status = 'open'
      and v_new.total = 3.50
      and v_old_status = 'closed'
      and v_old_total_after = v_old_total,
    format('old=%s old_total=%s old_total_after=%s new=%s new_total=%s',
           v_old_bill, v_old_total, v_old_total_after, v_new_bill, v_new.total)
  );
end $$;

-- Case 3: closing an already closed bill is a no-op.
do $$
declare
  v_old_bill uuid;
  v_closed_before timestamptz;
  v_total_before numeric;
  v_res jsonb;
  v_closed_after timestamptz;
  v_total_after numeric;
  v_status_after text;
begin
  select id
  into v_old_bill
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000a001'
    and status = 'closed'
  order by closed_at
  limit 1;

  select closed_at, total
  into v_closed_before, v_total_before
  from public.bills
  where id = v_old_bill;

  v_res := public.close_bill(v_old_bill);

  select closed_at, total, status
  into v_closed_after, v_total_after, v_status_after
  from public.bills
  where id = v_old_bill;

  insert into _bills_results
  values (
    3,
    'close_bill_noop',
    v_status_after = 'closed'
      and v_closed_after = v_closed_before
      and v_total_after = v_total_before
      and (v_res ->> 'status') = 'closed',
    format('closed_before=%s closed_after=%s total_before=%s total_after=%s',
           v_closed_before, v_closed_after, v_total_before, v_total_after)
  );
end $$;

-- Case 4: close_all_bills only closes supplied ids that are open at call time.
do $$
declare
  v_a_open uuid;
  v_a_closed uuid;
  v_b_bill uuid;
  v_p3 uuid;
  v_ids uuid[];
  v_closed_count int;
  v_a_open_status text;
  v_b_status text;
begin
  select id into v_a_open
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000a001'
    and status = 'open'
  limit 1;

  select id into v_a_closed
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000a001'
    and status = 'closed'
  order by closed_at
  limit 1;

  -- The admin's list is captured before B opens a bill.
  v_ids := array[v_a_open, v_a_closed];

  select id into v_p3 from public.products where name = 'ไส้กรอก' limit 1;
  perform public.create_order(
    'TEST-BILL-B1',
    jsonb_build_array(jsonb_build_object('product_id', v_p3, 'qty', 1)));

  select id into v_b_bill
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000b001'
    and status = 'open';

  v_closed_count := public.close_all_bills(v_ids);

  select status into v_a_open_status from public.bills where id = v_a_open;
  select status into v_b_status from public.bills where id = v_b_bill;

  insert into _bills_results
  values (
    4,
    'close_all_selective',
    v_closed_count = 1
      and v_a_open_status = 'closed'
      and v_b_status = 'open',
    format('closed_count=%s a_open=%s b_status=%s', v_closed_count, v_a_open_status, v_b_status)
  );
end $$;

-- Case 6: my_bills returns only the bills of the customer behind the code.
do $$
declare
  v_a_count int;
  v_a_total numeric;
  v_a_items int;
  v_a2_count int;
  v_b_count int;
  v_c_count int;
  v_leak int;
  v_b_bill uuid;
begin
  select count(*),
         coalesce(sum(total), 0),
         coalesce(sum(jsonb_array_length(items)), 0)::int
  into v_a_count, v_a_total, v_a_items
  from public.my_bills('TEST-BILL-A1');

  select count(*) into v_a2_count from public.my_bills('TEST-BILL-A2');
  select count(*) into v_b_count from public.my_bills('TEST-BILL-B1');
  select count(*) into v_c_count from public.my_bills('TEST-BILL-C1');

  select id into v_b_bill
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000b001';

  select count(*) into v_leak
  from public.my_bills('TEST-BILL-A1') mb
  join public.bills b on b.id = mb.bill_id
  where b.customer_id <> 'aaaaaaaa-0000-0000-0000-00000000a001';

  insert into _bills_results
  values (
    6,
    'my_bills_isolation',
    v_a_count = 2
      and v_a2_count = 2
      and v_a_total = 11.00
      and v_a_items = 3
      and v_b_count = 1
      and v_c_count = 0
      and v_leak = 0
      and not exists (
        select 1 from public.my_bills('TEST-BILL-A1') mb
        where mb.bill_id = v_b_bill
      ),
    format('a_count=%s a2_count=%s a_total=%s a_items=%s b_count=%s c_count=%s leak=%s',
           v_a_count, v_a2_count, v_a_total, v_a_items, v_b_count, v_c_count, v_leak)
  );
end $$;

-- Case 7: store_summary totals closed bills and lists customers without orders.
do $$
declare
  v_summary jsonb;
  v_total numeric;
  v_bills int;
  v_without jsonb;
  v_c_has boolean;
  v_a_has boolean;
  v_b_has boolean;
begin
  v_summary := public.store_summary((now() at time zone 'Asia/Bangkok')::date);
  v_total := (v_summary ->> 'total')::numeric;
  v_bills := jsonb_array_length(v_summary -> 'closed_bills');
  v_without := v_summary -> 'customers_without_orders';

  select exists (
    select 1 from jsonb_array_elements(v_without) e
    where e ->> 'customer_id' = 'aaaaaaaa-0000-0000-0000-00000000c001'
  ) into v_c_has;

  select exists (
    select 1 from jsonb_array_elements(v_without) e
    where e ->> 'customer_id' = 'aaaaaaaa-0000-0000-0000-00000000a001'
  ) into v_a_has;

  select exists (
    select 1 from jsonb_array_elements(v_without) e
    where e ->> 'customer_id' = 'aaaaaaaa-0000-0000-0000-00000000b001'
  ) into v_b_has;

  insert into _bills_results
  values (
    7,
    'store_summary',
    v_total = 11.00
      and v_bills = 2
      and v_c_has
      and not v_a_has
      and not v_b_has,
    format('total=%s closed_bills=%s c_without=%s a_without=%s b_without=%s',
           v_total, v_bills, v_c_has, v_a_has, v_b_has)
  );
end $$;

-- Case 8: register_code validates labels and overwrites.
do $$
declare
  v_res jsonb;
  v_label text;
  v_state_long text := 'no_error';
  v_state_empty text := 'no_error';
  v_state_invalid text := 'no_error';
begin
  v_res := public.register_code('TEST-BILL-A1', 'โต๊ะ 1');

  v_res := public.register_code('TEST-BILL-A1', 'โต๊ะ 2');

  select label into v_label from public.access_codes where code = 'TEST-BILL-A1';

  begin
    perform public.register_code('TEST-BILL-A1', repeat('ก', 41));
  exception when others then
    get stacked diagnostics v_state_long = returned_sqlstate;
  end;

  begin
    perform public.register_code('TEST-BILL-A1', '');
  exception when others then
    get stacked diagnostics v_state_empty = returned_sqlstate;
  end;

  begin
    perform public.register_code('TEST-BILL-NOPE', 'x');
  exception when others then
    get stacked diagnostics v_state_invalid = returned_sqlstate;
  end;

  insert into _bills_results
  values (
    8,
    'register_code_label',
    v_label = 'โต๊ะ 2'
      and (v_res ->> 'label') = 'โต๊ะ 2'
      and v_state_long = 'ZZ007'
      and v_state_empty = 'ZZ007'
      and v_state_invalid = 'ZZ001',
    format('label=%s too_long=%s empty=%s invalid_code=%s',
           v_label, v_state_long, v_state_empty, v_state_invalid)
  );
end $$;

-- Case 5: anon cannot call admin RPCs or read bills directly.
do $$
declare
  v_b_bill uuid;
  v_state_close text := 'no_error';
  v_state_close_all text := 'no_error';
  v_state_summary text := 'no_error';
  v_anon_state text := 'ok';
  v_anon_rows int;
begin
  perform set_config('request.jwt.claim.sub', '', true);
  perform set_config('request.jwt.claims', '', true);

  select id into v_b_bill
  from public.bills
  where customer_id = 'aaaaaaaa-0000-0000-0000-00000000b001'
    and status = 'open';

  begin
    perform public.close_bill(v_b_bill);
  exception when others then
    get stacked diagnostics v_state_close = returned_sqlstate;
  end;

  begin
    perform public.close_all_bills(array[v_b_bill]);
  exception when others then
    get stacked diagnostics v_state_close_all = returned_sqlstate;
  end;

  begin
    perform public.store_summary((now() at time zone 'Asia/Bangkok')::date);
  exception when others then
    get stacked diagnostics v_state_summary = returned_sqlstate;
  end;

  begin
    set local role anon;
    select count(*) into v_anon_rows from public.bills;
  exception when insufficient_privilege then
    v_anon_state := 'insufficient_privilege';
  end;
  reset role;

  insert into _bills_results
  values (
    5,
    'anon_forbidden',
    v_state_close = 'ZZ008'
      and v_state_close_all = 'ZZ008'
      and v_state_summary = 'ZZ008'
      and (v_anon_state = 'insufficient_privilege' or v_anon_rows = 0),
    format('close_bill=%s close_all=%s summary=%s anon_read=%s rows=%s',
           v_state_close, v_state_close_all, v_state_summary, v_anon_state, v_anon_rows)
  );
end $$;

select case_no, case_name, ok, detail
from _bills_results
order by case_no;

-- Rollback leaves no test customers, codes, orders, bills, or auth user.
rollback;
