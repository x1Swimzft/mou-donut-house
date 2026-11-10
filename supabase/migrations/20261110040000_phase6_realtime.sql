-- Phase 6: Enable Supabase Realtime for orders and bills
alter publication supabase_realtime add table public.orders;
alter publication supabase_realtime add table public.bills;
