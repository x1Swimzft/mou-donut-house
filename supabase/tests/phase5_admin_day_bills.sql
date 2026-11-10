-- Phase 5 admin_day_bills acceptance tests.
-- Runs in one transaction and rolls back; the database is left unchanged.
-- Dates are fixed so the result does not depend on when the test runs.
-- Admin RPCs use a temporary auth user inserted into auth.users and admins.

begin;

create temp table _day_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

create temp table _day_params as
select date '2026-11-10' as day, date '2026-11-09' as prev_day;

insert into public.customers (id, name) values
  ('dddddddd-0000-0000-0000-00000000c001', 'ลูกค้าทดสอบ หนึ่ง'),
  ('dddddddd-0000-0000-0000-00000000c002', 'ลูกค้าทดสอบ สอง'),
  ('dddddddd-0000-0000-0000-00000000c003', 'ลูกค้าทดสอบ สาม');

-- B1: open, spans prev_day + day. B2: closed on day. B3: open, prev_day only.
insert into public.bills (id, customer_id, status, opened_at, closed_at, total) values
  ('dddddddd-0000-0000-0000-00000000b001', 'dddddddd-0000-0000-0000-00000000c001', 'open',
   timestamptz '2026-11-09 09:00:00+07', null, 17.50),
  ('dddddddd-0000-0000-0000-00000000b002', 'dddddddd-0000-0000-0000-00000000c002', 'closed',
   timestamptz '2026-11-10 08:00:00+07', timestamptz '2026-11-10 09:00:00+07', 3.50),
  ('dddddddd-0000-0000-0000-00000000b003', 'dddddddd-0000-0000-0000-00000000c003', 'open',
   timestamptz '2026-11-09 10:00:00+07', null, 5.00);

insert into public.orders (id, customer_id, total, bill_id, business_date) values
  ('dddddddd-0000-0000-0000-00000000e001', 'dddddddd-0000-0000-0000-00000000c001', 7.00,
   'dddddddd-0000-0000-0000-00000000b001', (select prev_day from _day_params)),
  ('dddddddd-0000-0000-0000-00000000e002', 'dddddddd-0000-0000-0000-00000000c001', 10.50,
   'dddddddd-0000-0000-0000-00000000b001', (select day from _day_params)),
  ('dddddddd-0000-0000-0000-00000000e003', 'dddddddd-0000-0000-0000-00000000c002', 3.50,
   'dddddddd-0000-0000-0000-00000000b002', (select day from _day_params)),
  ('dddddddd-0000-0000-0000-00000000e004', 'dddddddd-0000-0000-0000-00000000c003', 5.00,
   'dddddddd-0000-0000-0000-00000000b003', (select prev_day from _day_params));

insert into auth.users (id) values ('dddddddd-0000-0000-0000-00000000ad01');
insert into auth.users (id) values ('dddddddd-0000-0000-0000-00000000ad02');
insert into public.admins (user_id) values ('dddddddd-0000-0000-0000-00000000ad01');

select set_config('request.jwt.claim.sub', 'dddddddd-0000-0000-0000-00000000ad01', true);
select set_config('request.jwt.claims',
                  jsonb_build_object('sub', 'dddddddd-0000-0000-0000-00000000ad01')::text,
                  true);

-- Case 1: admin identity is really what is_admin() reads.
do $$
declare
  v_uid text;
  v_admin boolean;
begin
  select (select auth.uid())::text into v_uid;
  v_admin := public.is_admin();

  insert into _day_results
  values (
    1,
    'admin_identity',
    v_uid = 'dddddddd-0000-0000-0000-00000000ad01' and v_admin,
    format('uid=%s is_admin=%s', coalesce(v_uid, 'null'), v_admin)
  );
end $$;

-- Case 2: day returns exactly the two bills that have orders on that day.
do $$
declare v_list jsonb;
begin
  v_list := public.admin_day_bills((select day from _day_params));
  insert into _day_results
  values (2, 'day_row_count', jsonb_array_length(v_list) = 2,
          'rows=' || jsonb_array_length(v_list));
end $$;

-- Case 3: cross-day bill keeps bill_total but day_total is that day only.
do $$
declare v_row jsonb;
begin
  select value into v_row
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  where value->>'bill_id' = 'dddddddd-0000-0000-0000-00000000b001';

  insert into _day_results
  values (
    3,
    'cross_day_bill_totals',
    (v_row->>'bill_total')::numeric = 17.50
      and (v_row->>'day_total')::numeric = 10.50
      and (v_row->>'order_count')::int = 1,
    format('bill_total=%s day_total=%s order_count=%s',
           v_row->>'bill_total', v_row->>'day_total', v_row->>'order_count')
  );
end $$;

-- Case 4: open bill status and customer name are returned.
do $$
declare v_row jsonb;
begin
  select value into v_row
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  where value->>'bill_id' = 'dddddddd-0000-0000-0000-00000000b001';

  insert into _day_results
  values (
    4,
    'open_bill_row',
    v_row->>'status' = 'open' and v_row->>'customer_name' = 'ลูกค้าทดสอบ หนึ่ง',
    format('status=%s name=%s', v_row->>'status', v_row->>'customer_name')
  );
end $$;

-- Case 5: closed bill stays closed with day_total equal to bill_total.
do $$
declare v_row jsonb;
begin
  select value into v_row
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  where value->>'bill_id' = 'dddddddd-0000-0000-0000-00000000b002';

  insert into _day_results
  values (
    5,
    'closed_bill_row',
    v_row->>'status' = 'closed'
      and (v_row->>'day_total')::numeric = 3.50
      and (v_row->>'bill_total')::numeric = 3.50
      and v_row->>'closed_at' is not null,
    format('status=%s day_total=%s closed_at=%s',
           v_row->>'status', v_row->>'day_total', v_row->>'closed_at')
  );
end $$;

-- Case 6: a bill with orders only on prev_day is absent from day.
do $$
declare v_count int;
begin
  select count(*) into v_count
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  where value->>'bill_id' = 'dddddddd-0000-0000-0000-00000000b003';

  insert into _day_results
  values (6, 'no_orders_on_day_absent', v_count = 0, 'matches=' || v_count);
end $$;

-- Case 7: null date returns an empty array.
do $$
declare v_res jsonb;
begin
  v_res := public.admin_day_bills(null);
  insert into _day_results
  values (7, 'null_date_empty', v_res = '[]'::jsonb, 'value=' || v_res::text);
end $$;

-- Case 8: prev_day returns B1 and B3, not B2.
do $$
declare
  v_list jsonb;
  v_has_b2 int;
begin
  v_list := public.admin_day_bills((select prev_day from _day_params));
  select count(*) into v_has_b2
  from jsonb_array_elements(v_list)
  where value->>'bill_id' = 'dddddddd-0000-0000-0000-00000000b002';

  insert into _day_results
  values (
    8,
    'prev_day_rows',
    jsonb_array_length(v_list) = 2 and v_has_b2 = 0,
    format('rows=%s b2=%s', jsonb_array_length(v_list), v_has_b2)
  );
end $$;

-- Case 9: row shape is exactly nine keys and totals are JSON numbers.
do $$
declare
  v_row jsonb;
  v_keys int;
  v_types text;
begin
  select value into v_row
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  limit 1;

  select count(*) into v_keys from jsonb_object_keys(v_row);
  v_types := concat_ws(',',
    jsonb_typeof(v_row->'bill_total'),
    jsonb_typeof(v_row->'day_total'),
    jsonb_typeof(v_row->'order_count'));

  insert into _day_results
  values (
    9,
    'row_shape',
    v_keys = 9 and v_types = 'number,number,number',
    format('keys=%s types=%s', v_keys, v_types)
  );
end $$;

-- Case 10: ordering follows opened_at then id.
do $$
declare v_first text;
begin
  select value->>'bill_id' into v_first
  from jsonb_array_elements(public.admin_day_bills((select day from _day_params)))
  limit 1;

  insert into _day_results
  values (10, 'ordering', v_first = 'dddddddd-0000-0000-0000-00000000b001', 'first=' || v_first);
end $$;

-- Case 11: authenticated non-admin gets ZZ008 and is_admin() is false.
do $$
declare
  v_state text := 'no_error';
  v_admin boolean;
  v_uid text;
begin
  perform set_config('request.jwt.claim.sub', 'dddddddd-0000-0000-0000-00000000ad02', true);
  perform set_config('request.jwt.claims',
                     jsonb_build_object('sub', 'dddddddd-0000-0000-0000-00000000ad02')::text,
                     true);
  v_admin := public.is_admin();
  select (select auth.uid())::text into v_uid;

  begin
    perform public.admin_day_bills((select day from _day_params));
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;

  insert into _day_results
  values (
    11,
    'non_admin_forbidden',
    v_state = 'ZZ008' and not v_admin,
    format('sqlstate=%s is_admin=%s uid=%s', v_state, v_admin, coalesce(v_uid, 'null'))
  );
end $$;

-- Case 12: anon (no jwt sub) gets ZZ008, auth.uid() is null and is_admin() is false.
do $$
declare
  v_state text := 'no_error';
  v_admin boolean;
  v_uid text;
begin
  perform set_config('request.jwt.claim.sub', null, true);
  perform set_config('request.jwt.claims', '{}', true);
  v_admin := public.is_admin();
  select (select auth.uid())::text into v_uid;

  begin
    perform public.admin_day_bills((select day from _day_params));
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;

  insert into _day_results
  values (
    12,
    'anon_forbidden',
    v_state = 'ZZ008' and not v_admin and v_uid is null,
    format('sqlstate=%s is_admin=%s uid=%s', v_state, v_admin, coalesce(v_uid, 'null'))
  );
end $$;

select jsonb_agg(
         jsonb_build_object(
           'n', case_no,
           'case', case_name,
           'ok', coalesce(ok, false),
           'detail', detail
         ) order by case_no
       ) as results
from _day_results;

rollback;
