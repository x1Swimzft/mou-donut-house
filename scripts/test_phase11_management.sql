-- Test Phase 11: Admin Order & Customer Management
do $$
declare
  v_admin_id uuid;
  v_cust1_id uuid;
  v_cust2_id uuid;
  v_code1 text := 'T11A';
  v_code2 text := 'T11B';
  v_p1_id uuid;
  v_res jsonb;
  v_orders_del int;
begin
  select user_id into v_admin_id from public.admins limit 1;
  if v_admin_id is null then
    raise exception 'no admin found';
  end if;
  perform set_config('request.jwt.claims', jsonb_build_object('sub', v_admin_id::text, 'role', 'authenticated')::text, true);

  -- 1. Create 2 test customers
  v_res := public.admin_create_customer('Test Cust 1', v_code1, 'Code 1');
  v_cust1_id := (v_res ->> 'customer_id')::uuid;

  v_res := public.admin_create_customer('Test Cust 2', v_code2, 'Code 2');
  v_cust2_id := (v_res ->> 'customer_id')::uuid;

  -- 2. Test admin_update_customer (Rename)
  v_res := public.admin_update_customer(v_cust1_id, 'Test Cust 1 Renamed');
  if (v_res ->> 'name') <> 'Test Cust 1 Renamed' then
    raise exception 'admin_update_customer failed';
  end if;

  -- 3. Test admin_update_code (Label)
  v_res := public.admin_update_code(v_code1, 'Updated Label 1');
  if (v_res ->> 'label') <> 'Updated Label 1' then
    raise exception 'admin_update_code failed';
  end if;

  -- 4. Test admin_add_code & admin_delete_code
  v_res := public.admin_add_code(v_cust1_id, 'T11EXTRA', 'Extra Code');
  v_res := public.admin_delete_code('T11EXTRA');
  if not (v_res ->> 'deleted')::boolean then
    raise exception 'admin_delete_code failed';
  end if;

  -- 5. Place test orders
  select id into v_p1_id from public.products where is_available limit 1;

  perform public.create_order(v_code1, jsonb_build_array(jsonb_build_object('product_id', v_p1_id, 'qty', 3)));
  perform public.create_order(v_code2, jsonb_build_array(jsonb_build_object('product_id', v_p1_id, 'qty', 5)));

  -- Verify orders placed on today
  if (select count(*) from public.orders where customer_id in (v_cust1_id, v_cust2_id)) <> 2 then
    raise exception 'orders not created properly';
  end if;

  -- 6. Test admin_delete_customer_day_orders for Cust 1
  v_res := public.admin_delete_customer_day_orders(v_cust1_id, (now() at time zone 'Asia/Bangkok')::date);
  v_orders_del := (v_res ->> 'orders_deleted')::int;
  if v_orders_del <> 1 then
    raise exception 'admin_delete_customer_day_orders failed: %', v_res;
  end if;

  -- Verify Cust 1 has 0 orders now, Cust 2 still has 1
  if (select count(*) from public.orders where customer_id = v_cust1_id) <> 0 then
    raise exception 'cust 1 order not deleted';
  end if;
  if (select count(*) from public.orders where customer_id = v_cust2_id) <> 1 then
    raise exception 'cust 2 order was touched';
  end if;

  -- 7. Test admin_clear_day_orders for today
  -- Note: this clears today's orders (or test orders)
  -- For safety of production data in test, delete cust2 order specifically, or test clear_day_orders on a future date
  insert into public.orders (customer_id, total, business_date) values (v_cust2_id, 100, '2099-01-01');
  v_res := public.admin_clear_day_orders('2099-01-01');
  if (v_res ->> 'orders_deleted')::int <> 1 then
    raise exception 'admin_clear_day_orders failed: %', v_res;
  end if;

  -- 8. Test admin_delete_customer (clean up test customers)
  perform public.admin_delete_customer(v_cust1_id);
  perform public.admin_delete_customer(v_cust2_id);

  if exists (select 1 from public.customers where id in (v_cust1_id, v_cust2_id)) then
    raise exception 'customers not deleted';
  end if;

  raise notice 'PHASE 11 TESTS PASSED SUCCESSFULLY';
end $$;
