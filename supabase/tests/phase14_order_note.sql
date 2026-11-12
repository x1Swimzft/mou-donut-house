-- Phase 14: rolled-back tests for orders.note and create_order(p_note).
-- Run in the Supabase SQL editor as postgres. The whole file rolls back.

begin;

insert into public.customers (id, name) values
  ('14141414-1414-1414-1414-141414141401', 'ลูกค้าทดสอบโน้ต');

insert into public.access_codes (code, customer_id, is_active) values
  ('TEST-NOTE-001', '14141414-1414-1414-1414-141414141401', true);

create temp table _note_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

do $$
declare
  v_product uuid;
  v_res jsonb;
  v_note text;
  v_len int;
  v_err text;
begin
  select id into v_product
  from public.products
  where is_available
  order by sort_order, name
  limit 1;

  if v_product is null then
    raise exception 'no available product; run supabase/seed.sql first';
  end if;

  -- 1. note is trimmed and stored with the order
  v_res := public.create_order(
    'TEST-NOTE-001',
    jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 2)),
    '  ไม่ใส่ถั่ว  ');
  select note into v_note from public.orders where id = (v_res ->> 'order_id')::uuid;
  insert into _note_results values
    (1, 'note trimmed', v_note = 'ไม่ใส่ถั่ว', coalesce(v_note, '<null>'));

  -- 2. null note stays null
  v_res := public.create_order(
    'TEST-NOTE-001',
    jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 1)),
    null);
  select note into v_note from public.orders where id = (v_res ->> 'order_id')::uuid;
  insert into _note_results values
    (2, 'null note', v_note is null, coalesce(v_note, '<null>'));

  -- 3. blank note is stored as null
  v_res := public.create_order(
    'TEST-NOTE-001',
    jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 1)),
    '   ');
  select note into v_note from public.orders where id = (v_res ->> 'order_id')::uuid;
  insert into _note_results values
    (3, 'blank note -> null', v_note is null, coalesce(v_note, '<null>'));

  -- 4. exactly 200 characters is accepted
  v_res := public.create_order(
    'TEST-NOTE-001',
    jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 1)),
    repeat('ก', 200));
  select char_length(note) into v_len from public.orders where id = (v_res ->> 'order_id')::uuid;
  insert into _note_results values
    (4, '200 chars accepted', v_len = 200, coalesce(v_len::text, '<null>'));

  -- 5. 201 characters is rejected with ZZ014
  v_err := 'no error';
  begin
    perform public.create_order(
      'TEST-NOTE-001',
      jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 1)),
      repeat('ก', 201));
  exception when others then
    v_err := sqlstate;
  end;
  insert into _note_results values
    (5, '201 chars rejected ZZ014', v_err = 'ZZ014', v_err);

  -- 6. two-argument calls still work and default the note to null
  v_res := public.create_order(
    'TEST-NOTE-001',
    jsonb_build_array(jsonb_build_object('product_id', v_product, 'qty', 1)));
  select note into v_note from public.orders where id = (v_res ->> 'order_id')::uuid;
  insert into _note_results values
    (6, '2-arg call keeps working', v_note is null, coalesce(v_note, '<null>'));

  -- 7. two-argument wrapper still validates product ids
  v_err := 'no error';
  begin
    perform public.create_order(
      'TEST-NOTE-001',
      jsonb_build_array(jsonb_build_object('product_id', 'not-a-uuid', 'qty', 1)));
  exception when others then
    v_err := sqlstate;
  end;
  insert into _note_results values
    (7, '2-arg malformed product rejected', v_err = 'ZZ003', v_err);
end $$;

select case_no, case_name, ok, detail
from _note_results
order by case_no;

rollback;
