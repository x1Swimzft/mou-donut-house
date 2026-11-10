-- Phase 2.6: replace the legacy 10-product menu with the real 17-item menu.
-- Precondition: no order/bill history. The guard prevents deleting products
-- that are already referenced by orders or bills.

do $$
begin
  if exists (select 1 from public.orders)
     or exists (select 1 from public.order_items)
     or exists (select 1 from public.bills)
     or exists (select 1 from public.bill_items) then
    raise exception 'phase2_6_menu_requires_empty_order_history';
  end if;
end $$;

delete from public.products;

insert into public.products (name, price, category, is_available) values
  ('ไส้กรอก',   3.50, 'donut', true),
  ('หมูหยอง',   3.50, 'donut', true),
  ('ลูกเกด',    3.50, 'donut', true),
  ('สับปะรด',   3.50, 'donut', true),
  ('ถั่วแดง',   3.50, 'donut', true),
  ('เผือก',     3.50, 'donut', true),
  ('สังขยา',    3.50, 'donut', true),
  ('ปูอัด',     3.50, 'donut', true),
  ('ไก่',       3.50, 'donut', true),
  ('แฮม',       3.50, 'donut', true),
  ('มะพร้าว',   4.00, 'donut', true),
  ('ช็อคโกแลต', 4.00, 'donut', true),
  ('มันม่วง',   3.50, 'donut', true),
  ('ข้าวโพด',   3.50, 'donut', true),
  ('พิซซ่า',    4.00, 'donut', true),
  ('โดนัท',     3.50, 'donut', true),
  ('ตุ๊กตา',    3.50, 'donut', true);
