-- Phase 7B: Realtime Menu Sync for Customer Web
-- Allow anon and authenticated to select products unconditionally so that Supabase Realtime CDC
-- delivers UPDATE events for both true -> false (sold out) and false -> true (restocked).
-- The shop menu already returns all 17 items via list_menu(p_code) anyway, so there is no secret data.

drop policy if exists products_select_public on public.products;
create policy products_select_public on public.products
  for select to anon, authenticated
  using (true);

alter table public.products replica identity full;
