begin;

create temp table _menu_results (
  case_no int,
  case_name text,
  ok boolean,
  detail text
) on commit drop;

insert into public.customers (id, name, phone) values
  ('99999999-9999-9999-9999-999999999991', 'ลูกค้าทดสอบเมนู', '0800000005'),
  ('99999999-9999-9999-9999-999999999992', 'ลูกค้าทดสอบเมนูปิด', '0800000006');

insert into public.access_codes (code, customer_id, is_active) values
  ('TEST-MENU-OK', '99999999-9999-9999-9999-999999999991', true),
  ('TEST-MENU-OFF', '99999999-9999-9999-9999-999999999992', false);

update public.products
set is_available = false
where id = (select id from public.products where is_available order by id limit 1);

do $$
declare
  v_expected int;
  v_expected_off int;
  v_count int;
  v_off int;
  v_fields_ok boolean;
begin
  select count(*) into v_expected from public.products;
  select count(*) into v_expected_off from public.products where not is_available;

  select count(*), count(*) filter (where not is_available)
  into v_count, v_off
  from public.list_menu('TEST-MENU-OK');

  select coalesce(bool_and(field_names = array['category','id','is_available','name','price']), false)
  into v_fields_ok
  from (
    select (
      select array_agg(k order by k)
      from jsonb_object_keys(to_jsonb(r)) k
    ) as field_names
    from public.list_menu('TEST-MENU-OK') r
  ) x;

  insert into _menu_results
  values (
    1,
    'valid_returns_all_products',
    v_count = v_expected and v_off = v_expected_off and v_off >= 1 and v_fields_ok,
    format('rows=%s expected=%s sold_out=%s expected_sold_out=%s fields_ok=%s',
           v_count, v_expected, v_off, v_expected_off, v_fields_ok)
  );
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.list_menu('TEST-MENU-NOPE');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _menu_results values (2, 'invalid_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.list_menu('TEST-MENU-OFF');
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _menu_results values (3, 'inactive_code', v_state = 'ZZ002', 'sqlstate=' || v_state);
end $$;

do $$
declare v_state text := 'no_error';
begin
  begin
    perform public.list_menu(null);
  exception when others then
    get stacked diagnostics v_state = returned_sqlstate;
  end;
  insert into _menu_results values (4, 'null_code', v_state = 'ZZ001', 'sqlstate=' || v_state);
end $$;

select case_no, case_name, ok, detail
from _menu_results
order by case_no;

rollback;
