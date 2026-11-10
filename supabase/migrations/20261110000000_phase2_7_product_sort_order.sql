-- Phase 2.7: keep the paper menu order in list_menu.
alter table public.products
  add column if not exists sort_order integer not null default 0;

update public.products p
set sort_order = v.sort_order
from (values
  ('ไส้กรอก',   1),
  ('หมูหยอง',   2),
  ('ลูกเกด',    3),
  ('สับปะรด',   4),
  ('ถั่วแดง',   5),
  ('เผือก',     6),
  ('สังขยา',    7),
  ('ปูอัด',     8),
  ('ไก่',       9),
  ('แฮม',       10),
  ('มะพร้าว',   11),
  ('ช็อคโกแลต', 12),
  ('มันม่วง',   13),
  ('ข้าวโพด',   14),
  ('พิซซ่า',    15),
  ('โดนัท',     16),
  ('ตุ๊กตา',    17)
) as v(name, sort_order)
where p.name = v.name;

create or replace function public.list_menu(p_code text)
returns table (
  id uuid,
  name text,
  price numeric(10,2),
  category text,
  is_available boolean
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform public.verify_access_code(p_code);

  return query
    select p.id, p.name, p.price, p.category, p.is_available
    from public.products p
    order by p.sort_order, p.name;
end;
$$;

revoke all on function public.list_menu(text) from public;
grant execute on function public.list_menu(text) to anon, authenticated;
