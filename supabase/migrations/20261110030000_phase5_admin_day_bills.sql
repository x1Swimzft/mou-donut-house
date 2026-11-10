-- Phase 5: admin day view of bills (open + closed) with per-day and per-bill totals.
-- Does not modify store_summary, create_order, or my_bills.

create or replace function public.admin_day_bills(p_business_date date)
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
      'bill_id', b.id,
      'customer_id', b.customer_id,
      'customer_name', c.name,
      'status', b.status,
      'bill_total', b.total,
      'day_total', (
        select coalesce(sum(o.total), 0)
        from public.orders o
        where o.bill_id = b.id and o.business_date = p_business_date
      ),
      'order_count', (
        select count(*)
        from public.orders o
        where o.bill_id = b.id and o.business_date = p_business_date
      ),
      'opened_at', b.opened_at,
      'closed_at', b.closed_at
    ) order by b.opened_at, b.id)
    from public.bills b
    join public.customers c on c.id = b.customer_id
    where exists (
      select 1
      from public.orders o
      where o.bill_id = b.id and o.business_date = p_business_date
    )
  ), '[]'::jsonb);
end;
$$;

revoke all on function public.admin_day_bills(date) from public;
grant execute on function public.admin_day_bills(date) to anon, authenticated;
