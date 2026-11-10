-- Phase 9: Admin Product Management (Price Edit & Add New Product)
-- Security: Gated by public.is_admin() -> ZZ008
-- Error codes:
--   ZZ008: forbidden
--   ZZ011: name_invalid
--   ZZ012: product_not_found
--   ZZ013: invalid_parameter (negative price, null id, invalid category)

-- 1. Update product price
create or replace function public.admin_update_product_price(
  p_product_id uuid,
  p_price numeric
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_updated record;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_product_id is null or p_price is null or p_price < 0 then
    raise exception using errcode = 'ZZ013', message = 'invalid_parameter';
  end if;

  update public.products
  set price = round(p_price, 2)
  where id = p_product_id
  returning id, name, price, category, is_available into v_updated;

  if not found then
    raise exception using errcode = 'ZZ012', message = 'product_not_found';
  end if;

  return jsonb_build_object(
    'id', v_updated.id,
    'name', v_updated.name,
    'price', v_updated.price,
    'category', v_updated.category,
    'is_available', v_updated.is_available
  );
end;
$$;

revoke all on function public.admin_update_product_price(uuid, numeric) from public;
grant execute on function public.admin_update_product_price(uuid, numeric) to anon, authenticated;

-- 2. Create new product
create or replace function public.admin_create_product(
  p_name text,
  p_price numeric,
  p_category text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_name text;
  v_category text;
  v_created record;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  v_name := btrim(coalesce(p_name, ''));
  v_category := lower(btrim(coalesce(p_category, '')));

  if v_name = '' then
    raise exception using errcode = 'ZZ011', message = 'name_invalid';
  end if;

  if p_price is null or p_price < 0 then
    raise exception using errcode = 'ZZ013', message = 'invalid_parameter';
  end if;

  if v_category not in ('donut', 'drink') then
    raise exception using errcode = 'ZZ013', message = 'invalid_parameter';
  end if;

  insert into public.products (name, price, category, is_available)
  values (v_name, round(p_price, 2), v_category, true)
  returning id, name, price, category, is_available into v_created;

  return jsonb_build_object(
    'id', v_created.id,
    'name', v_created.name,
    'price', v_created.price,
    'category', v_created.category,
    'is_available', v_created.is_available
  );
end;
$$;

revoke all on function public.admin_create_product(text, numeric, text) from public;
grant execute on function public.admin_create_product(text, numeric, text) to anon, authenticated;
