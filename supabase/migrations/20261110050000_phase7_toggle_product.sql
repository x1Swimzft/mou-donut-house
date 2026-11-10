-- Phase 7A: Admin Product Availability Toggle
-- RPC to toggle product availability (is_available) with is_admin() gate.
-- Error codes: ZZ012 (product_not_found), ZZ013 (invalid_parameter), ZZ008 (forbidden)

create or replace function public.admin_toggle_product(
  p_product_id uuid,
  p_is_available boolean
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_prod public.products;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_is_available is null then
    raise exception using errcode = 'ZZ013', message = 'invalid_parameter';
  end if;

  update public.products
  set is_available = p_is_available
  where id = p_product_id
  returning * into v_prod;

  if v_prod.id is null then
    raise exception using errcode = 'ZZ012', message = 'product_not_found';
  end if;

  return jsonb_build_object(
    'product_id', v_prod.id,
    'name', v_prod.name,
    'is_available', v_prod.is_available
  );
end;
$$;

revoke all on function public.admin_toggle_product(uuid, boolean) from public;
grant execute on function public.admin_toggle_product(uuid, boolean) to anon, authenticated;
