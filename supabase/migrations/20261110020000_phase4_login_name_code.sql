-- Phase 4: customer web login with customer name + access code.
-- verify_access_code(p_code) is left untouched: list_menu, create_order and
-- my_bills call it internally with a code only.

create or replace function public.verify_login(p_name text, p_code text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_customer_name text;
  v_is_active boolean;
  v_code text;
  v_name text;
begin
  v_code := upper(btrim(coalesce(p_code, '')));
  v_name := btrim(coalesce(p_name, ''));

  -- Same code as a wrong pair so a caller cannot tell the two apart.
  if v_code = '' or v_name = '' then
    raise exception using errcode = 'ZZ001', message = 'invalid_code';
  end if;

  select c.name, a.is_active
  into v_customer_name, v_is_active
  from public.access_codes a
  join public.customers c on c.id = a.customer_id
  where a.code = v_code;

  if not found then
    raise exception using errcode = 'ZZ001', message = 'invalid_code';
  end if;

  if lower(btrim(v_customer_name)) <> lower(v_name) then
    raise exception using errcode = 'ZZ001', message = 'invalid_code';
  end if;

  if not v_is_active then
    raise exception using errcode = 'ZZ002', message = 'code_inactive';
  end if;

  return jsonb_build_object('customer_name', v_customer_name);
end;
$$;

revoke all on function public.verify_login(text, text) from public;
grant execute on function public.verify_login(text, text) to anon, authenticated;
