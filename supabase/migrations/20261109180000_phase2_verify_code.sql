create or replace function public.verify_access_code(p_code text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_customer_name text;
  v_is_active boolean;
begin
  select c.name, a.is_active
  into v_customer_name, v_is_active
  from public.access_codes a
  join public.customers c on c.id = a.customer_id
  where a.code = p_code;

  if not found then
    raise exception using errcode = 'ZZ001', message = 'invalid_code';
  end if;

  if not v_is_active then
    raise exception using errcode = 'ZZ002', message = 'code_inactive';
  end if;

  return jsonb_build_object('customer_name', v_customer_name);
end;
$$;

revoke all on function public.verify_access_code(text) from public;
grant execute on function public.verify_access_code(text) to anon, authenticated;
