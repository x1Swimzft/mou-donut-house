-- Phase 11: Admin Order & Customer Management
-- 1. admin_delete_bill: deletes a specific bill and all associated orders
-- 2. admin_delete_customer_day_orders: deletes a customer's orders for a specific business date & reconciles bills
-- 3. admin_clear_day_orders: clears all orders for a specific business date & cleans up empty bills
-- 4. admin_update_customer: renames a customer
-- 5. admin_delete_customer: deletes a customer and their data
-- 6. admin_delete_code: deletes an access code
-- 7. admin_update_code: updates code label

-- 1. admin_delete_bill
create or replace function public.admin_delete_bill(p_bill_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_bill public.bills;
  v_orders_deleted int := 0;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_bill_id is null then
    raise exception using errcode = 'ZZ006', message = 'invalid_bill';
  end if;

  select * into v_bill
  from public.bills
  where id = p_bill_id;

  if v_bill.id is null then
    raise exception using errcode = 'ZZ006', message = 'bill_not_found';
  end if;

  -- Delete orders belonging to this bill (order_items cascade delete)
  with deleted as (
    delete from public.orders
    where bill_id = p_bill_id
    returning id
  )
  select count(*) into v_orders_deleted from deleted;

  -- Delete bill_items and bill
  delete from public.bill_items where bill_id = p_bill_id;
  delete from public.bills where id = p_bill_id;

  return jsonb_build_object(
    'bill_id', p_bill_id,
    'orders_deleted', v_orders_deleted,
    'success', true
  );
end;
$$;

revoke all on function public.admin_delete_bill(uuid) from public;
grant execute on function public.admin_delete_bill(uuid) to anon, authenticated;


-- 2. admin_delete_customer_day_orders
create or replace function public.admin_delete_customer_day_orders(p_customer_id uuid, p_business_date date)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_bill_ids uuid[];
  v_b_id uuid;
  v_remaining_orders int;
  v_orders_deleted int := 0;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_customer_id is null or p_business_date is null then
    return jsonb_build_object('orders_deleted', 0, 'success', false);
  end if;

  -- Collect bill_ids of orders to be deleted
  select array_agg(distinct bill_id) filter (where bill_id is not null)
  into v_bill_ids
  from public.orders
  where customer_id = p_customer_id
    and business_date = p_business_date;

  -- Delete the orders
  with deleted as (
    delete from public.orders
    where customer_id = p_customer_id
      and business_date = p_business_date
    returning id
  )
  select count(*) into v_orders_deleted from deleted;

  -- Reconcile or delete affected bills
  if v_bill_ids is not null then
    foreach v_b_id in array v_bill_ids loop
      select count(*) into v_remaining_orders
      from public.orders
      where bill_id = v_b_id;

      if v_remaining_orders = 0 then
        delete from public.bill_items where bill_id = v_b_id;
        delete from public.bills where id = v_b_id;
      else
        update public.bills
        set total = coalesce((select sum(total) from public.orders where bill_id = v_b_id), 0)
        where id = v_b_id;

        delete from public.bill_items where bill_id = v_b_id;
        insert into public.bill_items (bill_id, product_id, product_name, unit_price, qty)
        select v_b_id, oi.product_id, min(oi.product_name), min(oi.unit_price), sum(oi.qty)::int
        from public.orders o
        join public.order_items oi on oi.order_id = o.id
        where o.bill_id = v_b_id
        group by oi.product_id;
      end if;
    end loop;
  end if;

  return jsonb_build_object(
    'customer_id', p_customer_id,
    'business_date', p_business_date,
    'orders_deleted', v_orders_deleted,
    'success', true
  );
end;
$$;

revoke all on function public.admin_delete_customer_day_orders(uuid, date) from public;
grant execute on function public.admin_delete_customer_day_orders(uuid, date) to anon, authenticated;


-- 3. admin_clear_day_orders
create or replace function public.admin_clear_day_orders(p_business_date date)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_bill_ids uuid[];
  v_b_id uuid;
  v_remaining_orders int;
  v_orders_deleted int := 0;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_business_date is null then
    return jsonb_build_object('orders_deleted', 0, 'success', false);
  end if;

  -- Collect bill_ids of orders to be deleted
  select array_agg(distinct bill_id) filter (where bill_id is not null)
  into v_bill_ids
  from public.orders
  where business_date = p_business_date;

  -- Delete orders of this business_date
  with deleted as (
    delete from public.orders
    where business_date = p_business_date
    returning id
  )
  select count(*) into v_orders_deleted from deleted;

  -- Reconcile or delete affected bills
  if v_bill_ids is not null then
    foreach v_b_id in array v_bill_ids loop
      select count(*) into v_remaining_orders
      from public.orders
      where bill_id = v_b_id;

      if v_remaining_orders = 0 then
        delete from public.bill_items where bill_id = v_b_id;
        delete from public.bills where id = v_b_id;
      else
        update public.bills
        set total = coalesce((select sum(total) from public.orders where bill_id = v_b_id), 0)
        where id = v_b_id;

        delete from public.bill_items where bill_id = v_b_id;
        insert into public.bill_items (bill_id, product_id, product_name, unit_price, qty)
        select v_b_id, oi.product_id, min(oi.product_name), min(oi.unit_price), sum(oi.qty)::int
        from public.orders o
        join public.order_items oi on oi.order_id = o.id
        where o.bill_id = v_b_id
        group by oi.product_id;
      end if;
    end loop;
  end if;

  return jsonb_build_object(
    'business_date', p_business_date,
    'orders_deleted', v_orders_deleted,
    'success', true
  );
end;
$$;

revoke all on function public.admin_clear_day_orders(date) from public;
grant execute on function public.admin_clear_day_orders(date) to anon, authenticated;


-- 4. admin_update_customer
create or replace function public.admin_update_customer(p_customer_id uuid, p_name text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_name text;
  v_customer public.customers;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_customer_id is null then
    raise exception using errcode = 'ZZ010', message = 'customer_not_found';
  end if;

  v_name := btrim(p_name);
  if v_name is null or char_length(v_name) < 1 or char_length(v_name) > 80 then
    raise exception using errcode = 'ZZ011', message = 'name_invalid';
  end if;

  update public.customers
  set name = v_name
  where id = p_customer_id
  returning * into v_customer;

  if v_customer.id is null then
    raise exception using errcode = 'ZZ010', message = 'customer_not_found';
  end if;

  return jsonb_build_object(
    'customer_id', v_customer.id,
    'name', v_customer.name
  );
end;
$$;

revoke all on function public.admin_update_customer(uuid, text) from public;
grant execute on function public.admin_update_customer(uuid, text) to anon, authenticated;


-- 5. admin_delete_customer
create or replace function public.admin_delete_customer(p_customer_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_exists boolean;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_customer_id is null then
    raise exception using errcode = 'ZZ010', message = 'customer_not_found';
  end if;

  select exists(select 1 from public.customers where id = p_customer_id) into v_exists;
  if not v_exists then
    raise exception using errcode = 'ZZ010', message = 'customer_not_found';
  end if;

  -- Delete customer's orders (cascades to order_items)
  delete from public.orders where customer_id = p_customer_id;

  -- Delete customer's bills (cascades to bill_items)
  delete from public.bills where customer_id = p_customer_id;

  -- Delete customer's access codes
  delete from public.access_codes where customer_id = p_customer_id;

  -- Delete customer
  delete from public.customers where id = p_customer_id;

  return jsonb_build_object(
    'customer_id', p_customer_id,
    'deleted', true
  );
end;
$$;

revoke all on function public.admin_delete_customer(uuid) from public;
grant execute on function public.admin_delete_customer(uuid) to anon, authenticated;


-- 6. admin_delete_code
create or replace function public.admin_delete_code(p_code text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_code text;
  v_deleted boolean;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  v_code := upper(btrim(p_code));
  if v_code is null or char_length(v_code) < 1 then
    raise exception using errcode = 'ZZ009', message = 'code_invalid';
  end if;

  delete from public.access_codes
  where code = v_code;

  get diagnostics v_deleted = row_count;

  if not v_deleted then
    raise exception using errcode = 'ZZ010', message = 'code_not_found';
  end if;

  return jsonb_build_object(
    'code', v_code,
    'deleted', true
  );
end;
$$;

revoke all on function public.admin_delete_code(text) from public;
grant execute on function public.admin_delete_code(text) to anon, authenticated;


-- 7. admin_update_code
create or replace function public.admin_update_code(p_code text, p_label text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_code text;
  v_label text;
  v_row public.access_codes;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  v_code := upper(btrim(p_code));
  if v_code is null or char_length(v_code) < 1 then
    raise exception using errcode = 'ZZ009', message = 'code_invalid';
  end if;

  v_label := btrim(p_label);
  if v_label is null or char_length(v_label) < 1 or char_length(v_label) > 40 then
    raise exception using errcode = 'ZZ007', message = 'label_invalid';
  end if;

  update public.access_codes
  set label = v_label
  where code = v_code
  returning * into v_row;

  if v_row.code is null then
    raise exception using errcode = 'ZZ010', message = 'code_not_found';
  end if;

  return jsonb_build_object(
    'code', v_row.code,
    'label', v_row.label
  );
end;
$$;

revoke all on function public.admin_update_code(text, text) from public;
grant execute on function public.admin_update_code(text, text) to anon, authenticated;
