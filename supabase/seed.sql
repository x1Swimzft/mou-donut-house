insert into public.products (name, price, category, sort_order)
select v.name, v.price, v.category, v.sort_order
from (values
  ('ไส้กรอก',   3.50::numeric, 'donut', 1),
  ('หมูหยอง',   3.50,          'donut', 2),
  ('ลูกเกด',    3.50,          'donut', 3),
  ('สับปะรด',   3.50,          'donut', 4),
  ('ถั่วแดง',   3.50,          'donut', 5),
  ('เผือก',     3.50,          'donut', 6),
  ('สังขยา',    3.50,          'donut', 7),
  ('ปูอัด',     3.50,          'donut', 8),
  ('ไก่',       3.50,          'donut', 9),
  ('แฮม',       3.50,          'donut', 10),
  ('มะพร้าว',   4.00,          'donut', 11),
  ('ช็อคโกแลต', 4.00,          'donut', 12),
  ('มันม่วง',   3.50,          'donut', 13),
  ('ข้าวโพด',   3.50,          'donut', 14),
  ('พิซซ่า',    4.00,          'donut', 15),
  ('โดนัท',     3.50,          'donut', 16),
  ('ตุ๊กตา',    3.50,          'donut', 17)
) as v(name, price, category, sort_order)
where not exists (select 1 from public.products);
