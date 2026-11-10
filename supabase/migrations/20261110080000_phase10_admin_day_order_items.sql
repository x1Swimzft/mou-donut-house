-- Phase 10: admin_day_order_items — Aggregate all orders by filling/product for kitchen production
-- Groups customer orders by product name and unit price, providing combined piece count and customer breakdown.

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
    ) order by p_items.total_qty desc, p_items.product_name)
    from (
      select
        t.product_name,
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
      group by t.product_name, t.unit_price
    ) p_items
  ), '[]'::jsonb);
end;
$$;

revoke all on function public.admin_day_order_items(date) from public;
grant execute on function public.admin_day_order_items(date) to anon, authenticated;
