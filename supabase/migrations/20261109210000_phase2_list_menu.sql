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
    order by p.category, p.name;
end;
$$;

revoke all on function public.list_menu(text) from public;
grant execute on function public.list_menu(text) to anon, authenticated;
