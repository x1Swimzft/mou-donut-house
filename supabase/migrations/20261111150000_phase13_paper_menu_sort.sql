-- Phase 13: Paper Menu Order Sorting for Kitchen & Bills
-- Ensures admin_day_order_items, my_bills, and new products follow paper bill sequence (1 to 17).

-- 1. admin_day_order_items sorted by sort_order
create or replace function public.admin_day_order_items(p_business_date date)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_business_date is null then
    return '[]'::jsonb;
  end if;

  return coalesce((
    select jsonb_agg(jsonb_build_object(
      'product_name', p_items.product_name,
      'unit_price', p_items.unit_price,
      'total_qty', p_items.total_qty,
      'total_amount', p_items.total_amount,
      'customer_breakdown', p_items.customers
    ) order by p_items.sort_order asc, p_items.product_name asc)
    from (
      select
        t.product_name,
        coalesce(prod.sort_order, 999) as sort_order,
        t.unit_price,
        sum(t.cust_qty)::int as total_qty,
        sum(t.cust_qty * t.unit_price)::numeric(10,2) as total_amount,
        jsonb_agg(jsonb_build_object(
          'customer_name', t.customer_name,
          'qty', t.cust_qty
        ) order by t.cust_qty desc, t.customer_name) as customers
      from (
        select
          oi.product_name,
          oi.unit_price,
          coalesce(c.name, 'ลูกค้า') as customer_name,
          sum(oi.qty)::int as cust_qty
        from public.order_items oi
        join public.orders o on o.id = oi.order_id
        join public.customers c on c.id = o.customer_id
        where o.business_date = p_business_date
        group by oi.product_name, oi.unit_price, c.name, c.id
      ) t
      left join public.products prod on prod.name = t.product_name
      group by t.product_name, prod.sort_order, t.unit_price
    ) p_items
  ), '[]'::jsonb);
end;
$$;

revoke all on function public.admin_day_order_items(date) from public;
grant execute on function public.admin_day_order_items(date) to anon, authenticated;

-- 2. my_bills with items sorted by paper sort_order
create or replace function public.my_bills(p_code text)
returns table (
  bill_id uuid,
  status text,
  business_date date,
  opened_at timestamptz,
  closed_at timestamptz,
  total numeric,
  items jsonb
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_customer_id uuid;
begin
  perform public.verify_access_code(p_code);

  select a.customer_id
  into v_customer_id
  from public.access_codes a
  where a.code = p_code;

  return query
  select
    b.id as bill_id,
    b.status,
    coalesce(
      (select min(o.business_date) from public.orders o where o.bill_id = b.id),
      b.opened_at::date
    ) as business_date,
    b.opened_at,
    b.closed_at,
    b.total,
    coalesce((
      select jsonb_agg(
        jsonb_build_object(
          'product_name', bi.product_name,
          'qty', bi.qty,
          'line_total', bi.unit_price * bi.qty
        )
        order by coalesce(p.sort_order, 999) asc, bi.product_name asc
      )
      from public.bill_items bi
      left join public.products p on p.id = bi.product_id
      where bi.bill_id = b.id
    ), '[]'::jsonb) as items
  from public.bills b
  where b.customer_id = v_customer_id
  order by b.opened_at desc;
end;
$$;

revoke all on function public.my_bills(text) from public;
grant execute on function public.my_bills(text) to anon, authenticated;

-- 3. admin_create_product: default sort_order to max + 1
create or replace function public.admin_create_product(
  p_name text,
  p_price numeric,
  p_category text
)
returns public.products
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_trimmed text;
  v_row public.products;
  v_next_sort int;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  v_trimmed := trim(coalesce(p_name, ''));
  if v_trimmed = '' then
    raise exception using errcode = 'ZZ011', message = 'product_name_empty';
  end if;

  if p_price is null or p_price <= 0 then
    raise exception using errcode = 'ZZ012', message = 'product_price_invalid';
  end if;

  if p_category is null or p_category not in ('donut', 'drink') then
    raise exception using errcode = 'ZZ013', message = 'product_category_invalid';
  end if;

  select coalesce(max(sort_order), 0) + 1 into v_next_sort from public.products;

  insert into public.products (name, price, category, is_available, sort_order)
  values (v_trimmed, round(p_price::numeric, 2), p_category, true, v_next_sort)
  returning * into v_row;

  return v_row;
end;
$$;

revoke all on function public.admin_create_product(text, numeric, text) from public;
grant execute on function public.admin_create_product(text, numeric, text) to authenticated;
