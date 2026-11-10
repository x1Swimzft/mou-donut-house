-- Phase 4 customer admin acceptance tests.
-- Runs in one transaction and rolls back; the database is left unchanged.
-- Admin RPCs use a temporary auth user inserted into auth.users and admins.

begin;

create temp table _cust_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name) values
  ('cccccccc-0000-0000-0000-00000000c001', 'ลูกค้าทดสอบ หนึ่ง');

insert into public.access_codes (code, customer_id, is_active, label) values
  ('CUST-ONE-1', 'cccccccc-0000-0000-0000-00000000c001', true, 'รหัสแรก'),
  ('CUST-ONE-2', 'cccccccc-0000-0000-0000-00000000c001', true, 'รหัสสอง');

-- Temporary admin and a plain authenticated user (both rolled back).
insert into auth.users (id) values ('cccccccc-0000-0000-0000-00000000ad01');
insert into auth.users (id) values ('cccccccc-0000-0000-0000-00000000ad02');
insert into public.admins (user_id) values ('cccccccc-0000-0000-0000-00000000ad01');

select set_config('request.jwt.claim.sub', 'cccccccc-0000-0000-0000-00000000ad01', true);
select set_config('request.jwt.claims',
                  jsonb_build_object('sub', 'cccccccc-0000-0000-0000-00000000ad01')::text,
                  true);

-- Case 1: create customer + first code, code normalised to uppercase.
do $$
declare
  v_res jsonb;
  v_id uuid;
  v_name text;
  v_code text;
  v_label text;
  v_active boolean;
begin
  v_res := public.admin_create_customer('  ลูกค้าใหม่  ', ' cust-new-9 ', '  รหัสแรก  ');
  v_id := (v_res->>'customer_id')::uuid;

  select c.name into v_name from public.customers c where c.id = v_id;
  select a.code, a.label, a.is_active into v_code, v_label, v_active
  from public.access_codes a where a.customer_id = v_id;

  insert into _cust_results
  values (
    1,
    'create_customer_normalises',
    v_name = 'ลูกค้าใหม่' and v_code = 'CUST-NEW-9' and v_label = 'รหัสแรก' and v_active,
    format('name=%s code=%s label=%s active=%s', v_name, v_code, v_label, v_active)
  );
end $$;

-- Case 2: list returns customers with codes, and no extra fields.
do $$
declare
  v_list jsonb;
  v_new jsonb;
  v_one jsonb;
  v_keys int;
  v_code_keys int;
begin
  v_list := public.admin_list_customers();
  select value into v_new from jsonb_array_elements(v_list) where value->>'name' = 'ลูกค้าใหม่';
  select value into v_one from jsonb_array_elements(v_list) where value->>'name' = 'ลูกค้าทดสอบ หนึ่ง';

  select count(*) into v_keys from jsonb_object_keys(v_new);
  select count(*) into v_code_keys from jsonb_object_keys(v_new->'codes'->0);

  insert into _cust_results
  values (
    2,
    'list_shape_and_codes',
    v_keys = 4 and v_code_keys = 4
      and jsonb_array_length(v_new->'codes') = 1
      and jsonb_array_length(v_one->'codes') = 2,
    format('customer_keys=%s code_keys=%s new_codes=%s one_codes=%s',
      v_keys, v_code_keys,
      jsonb_array_length(v_new->'codes'), jsonb_array_length(v_one->'codes'))
  );
end $$;

-- Case 3: add a second code to an existing customer.
do $$
declare
  v_res jsonb;
  v_count int;
begin
  v_res := public.admin_add_code('cccccccc-0000-0000-0000-00000000c001', ' cust-one-3 ', 'รหัสสาม');
  select count(*) into v_count
  from public.access_codes
  where customer_id = 'cccccccc-0000-0000-0000-00000000c001';

  insert into _cust_results
  values (
    3,
    'add_code',
    (v_res->>'code') = 'CUST-ONE-3' and v_count = 3,
    format('code=%s codes=%s', v_res->>'code', v_count)
  );
end $$;

-- Case 4: label longer than 40 -> ZZ007.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_create_customer('ลูกค้า label ยาว', 'CUST-LBL-1', repeat('ก', 41));
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (4, 'label_too_long', v_state = 'ZZ007', 'sqlstate=' || v_state);
end $$;

-- Case 5: blank label -> ZZ007.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_add_code('cccccccc-0000-0000-0000-00000000c001', 'CUST-LBL-2', '   ');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (5, 'blank_label', v_state = 'ZZ007', 'sqlstate=' || v_state);
end $$;

-- Case 6: duplicate code -> ZZ009 and no orphan customer row.
do $$
declare
  v_state text := 'no_error';
  v_before int;
  v_after int;
begin
  select count(*) into v_before from public.customers;
  begin
    perform public.admin_create_customer('ลูกค้าซ้ำรหัส', 'CUST-ONE-1', 'ซ้ำ');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  select count(*) into v_after from public.customers;

  insert into _cust_results
  values (
    6,
    'duplicate_code_no_orphan',
    v_state = 'ZZ009' and v_before = v_after,
    format('sqlstate=%s before=%s after=%s', v_state, v_before, v_after)
  );
end $$;

-- Case 7: blank code -> ZZ009.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_create_customer('ลูกค้าไม่มีรหัส', '   ', 'label');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (7, 'blank_code', v_state = 'ZZ009', 'sqlstate=' || v_state);
end $$;

-- Case 8: code longer than 32 -> ZZ009.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_add_code('cccccccc-0000-0000-0000-00000000c001', repeat('C', 33), 'ยาว');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (8, 'code_too_long', v_state = 'ZZ009', 'sqlstate=' || v_state);
end $$;

-- Case 9: blank name -> ZZ011.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_create_customer('   ', 'CUST-NAME-1', 'label');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (9, 'blank_name', v_state = 'ZZ011', 'sqlstate=' || v_state);
end $$;

-- Case 10: name longer than 80 -> ZZ011.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_create_customer(repeat('ก', 81), 'CUST-NAME-2', 'label');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (10, 'name_too_long', v_state = 'ZZ011', 'sqlstate=' || v_state);
end $$;

-- Case 11: deactivate a code, list reflects it.
do $$
declare
  v_res jsonb;
  v_list jsonb;
  v_active boolean;
begin
  v_res := public.admin_toggle_code('cust-one-2', false);
  select (code->>'is_active')::boolean into v_active
  from jsonb_array_elements(public.admin_list_customers()) value,
       jsonb_array_elements(value->'codes') code
  where code->>'code' = 'CUST-ONE-2';

  insert into _cust_results
  values (
    11,
    'toggle_off',
    (v_res->>'is_active')::boolean = false and v_active = false,
    format('rpc=%s list=%s', v_res->>'is_active', v_active)
  );
end $$;

-- Case 12: reactivate.
do $$
declare v_res jsonb;
begin
  v_res := public.admin_toggle_code('CUST-ONE-2', true);
  insert into _cust_results
  values (12, 'toggle_on', (v_res->>'is_active')::boolean, 'rpc=' || (v_res->>'is_active'));
end $$;

-- Case 13: unknown code -> ZZ010.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_toggle_code('CUST-NOPE-999', true);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (13, 'toggle_unknown_code', v_state = 'ZZ010', 'sqlstate=' || v_state);
end $$;

-- Case 14: add code to unknown customer -> ZZ010.
do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.admin_add_code('dddddddd-0000-0000-0000-000000000000', 'CUST-X-1', 'label');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _cust_results
  values (14, 'unknown_customer', v_state = 'ZZ010', 'sqlstate=' || v_state);
end $$;

-- Case 15: authenticated but not admin -> ZZ008 on all four RPCs.
do $$
declare
  v_list text := 'no_error';
  v_create text := 'no_error';
  v_add text := 'no_error';
  v_toggle text := 'no_error';
begin
  perform set_config('request.jwt.claim.sub', 'cccccccc-0000-0000-0000-00000000ad02', true);
  perform set_config('request.jwt.claims',
                     jsonb_build_object('sub', 'cccccccc-0000-0000-0000-00000000ad02')::text,
                     true);

  begin
    perform public.admin_list_customers();
  exception when others then
    get stacked diagnostics v_list = returned_sqlstate;
  end;

  begin
    perform public.admin_create_customer('ไม่ใช่แอดมิน', 'CUST-NOADMIN-1', 'label');
  exception when others then
    get stacked diagnostics v_create = returned_sqlstate;
  end;

  begin
    perform public.admin_add_code('cccccccc-0000-0000-0000-00000000c001', 'CUST-NOADMIN-2', 'label');
  exception when others then
    get stacked diagnostics v_add = returned_sqlstate;
  end;

  begin
    perform public.admin_toggle_code('CUST-ONE-1', false);
  exception when others then
    get stacked diagnostics v_toggle = returned_sqlstate;
  end;

  insert into _cust_results
  values (
    15,
    'non_admin_forbidden',
    v_list = 'ZZ008' and v_create = 'ZZ008' and v_add = 'ZZ008' and v_toggle = 'ZZ008',
    format('list=%s create=%s add=%s toggle=%s', v_list, v_create, v_add, v_toggle)
  );
end $$;

-- Case 16: anon (no jwt sub) -> ZZ008 and no rows leak.
do $$
declare v_state text := 'no_error';
begin
  perform set_config('request.jwt.claim.sub', null, true);
  perform set_config('request.jwt.claims', '{}', true);

  begin
    perform public.admin_list_customers();
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;

  insert into _cust_results
  values (16, 'anon_forbidden', v_state = 'ZZ008', 'sqlstate=' || v_state);
end $$;

select jsonb_agg(
         jsonb_build_object(
           'n', case_no,
           'case', case_name,
           'ok', coalesce(ok, false),
           'detail', detail
         ) order by case_no
       ) as results
from _cust_results;

rollback;
