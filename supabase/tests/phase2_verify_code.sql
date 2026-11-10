begin;

create temp table _verify_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name, phone) values
  ('55555555-5555-5555-5555-555555555555', 'ลูกค้าทดสอบ ถูกต้อง', '0800000001'),
  ('66666666-6666-6666-6666-666666666666', 'ลูกค้าทดสอบ ปิดใช้งาน', '0800000002');

insert into public.access_codes (code, customer_id, is_active) values
  ('TEST-VERIFY-OK', '55555555-5555-5555-5555-555555555555', true),
  ('TEST-VERIFY-OFF', '66666666-6666-6666-6666-666666666666', false);

do $$
declare
  v_res jsonb;
  v_keys int;
  v_has_phone boolean;
begin
  v_res := public.verify_access_code('TEST-VERIFY-OK');
  select count(*) into v_keys from jsonb_object_keys(v_res);
  v_has_phone := v_res ? 'phone';
  insert into _verify_results
  values (
    1,
    'valid_code',
    (v_res->>'customer_name') = 'ลูกค้าทดสอบ ถูกต้อง' and v_keys = 1 and not v_has_phone,
    format('customer_name=%s keys=%s has_phone=%s', v_res->>'customer_name', v_keys, v_has_phone)
  );
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_access_code('DN-NOPE-9999');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _verify_results values (2, 'invalid_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_access_code('TEST-VERIFY-OFF');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _verify_results values (3, 'inactive_code', v_state = 'ZZ002', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.verify_access_code(null);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _verify_results values (4, 'null_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

select case_no, case_name, ok, detail
from _verify_results
order by case_no;

rollback;
