begin;

create temp table _login_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name) values
  ('77777777-7777-7777-7777-777777777777', 'สมชาย ใจดี'),
  ('88888888-8888-8888-8888-888888888888', 'Somchai Test'),
  ('99999999-9999-9999-9999-999999999999', 'ลูกค้าอีกคน');

insert into public.access_codes (code, customer_id, is_active, label) values
  ('LOGIN-OK-01', '77777777-7777-7777-7777-777777777777', true, 'รหัสหลัก'),
  ('LOGIN-OK-02', '77777777-7777-7777-7777-777777777777', true, 'รหัสลูกน้อง'),
  ('LOGIN-OFF-01', '77777777-7777-7777-7777-777777777777', false, 'รหัสปิด'),
  ('LOGIN-OTHER', '99999999-9999-9999-9999-999999999999', true, 'รหัสคนอื่น'),
  ('LOGIN-CASE-01', '88888888-8888-8888-8888-888888888888', true, 'ตัวเล็ก');

do $$
declare
  v_res jsonb;
  v_keys int;
begin
  v_res := public.verify_login('สมชาย ใจดี', 'LOGIN-OK-01');
  select count(*) into v_keys from jsonb_object_keys(v_res);
  insert into _login_results
  values (
    1,
    'valid_pair',
    (v_res->>'customer_name') = 'สมชาย ใจดี' and v_keys = 1,
    format('customer_name=%s keys=%s', v_res->>'customer_name', v_keys)
  );
end $$;

do $$
declare
  v_res jsonb;
begin
  v_res := public.verify_login('สมชาย ใจดี', 'LOGIN-OK-02');
  insert into _login_results
  values (
    2,
    'second_code_same_customer',
    (v_res->>'customer_name') = 'สมชาย ใจดี',
    'customer_name=' || (v_res->>'customer_name')
  );
end $$;

do $$
declare
  v_res jsonb;
begin
  v_res := public.verify_login('  SOMCHAI TEST  ', ' login-case-01 ');
  insert into _login_results
  values (
    3,
    'trim_and_case_insensitive',
    (v_res->>'customer_name') = 'Somchai Test',
    'customer_name=' || (v_res->>'customer_name')
  );
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('สมชาย ใจดี', 'LOGIN-OTHER');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (4, 'wrong_name_valid_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('สมชาย ใจดี', 'LOGIN-NOPE-9999');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (5, 'unknown_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('ลูกค้าอีกคน', 'LOGIN-OK-01');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (6, 'other_customer_name', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('สมชาย ใจดี', 'LOGIN-OFF-01');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (7, 'inactive_code', v_state = 'ZZ002', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('ลูกค้าอีกคน', 'LOGIN-OFF-01');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (8, 'inactive_code_wrong_name_hides_state', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login('   ', 'LOGIN-OK-01');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (9, 'empty_name', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_login(null, null);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _login_results
  values (10, 'null_inputs', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

select case_no, case_name, ok, detail
from _login_results
order by case_no;

rollback;
