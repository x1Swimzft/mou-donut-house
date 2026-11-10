-- Phase 4: admin customer + access code management.
-- New codes: ZZ009 code_invalid/code_taken, ZZ010 code_not_found/customer_not_found,
-- ZZ011 name_invalid. Existing ZZ007 label_invalid and ZZ008 forbidden are reused.

create index if not exists access_codes_customer_id_idx
  on public.access_codes (customer_id);

-- List all customers with their codes (admin only).
create or replace function public.admin_list_customers()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  return coalesce((
    select jsonb_agg(jsonb_build_object(
      'customer_id', c.id,
      'name', c.name,
      'created_at', c.created_at,
      'codes', coalesce((
        select jsonb_agg(jsonb_build_object(
          'code', a.code,
          'label', a.label,
          'is_active', a.is_active,
          'first_used_at', a.first_used_at
        ) order by a.code)
        from public.access_codes a
        where a.customer_id = c.id
      ), '[]'::jsonb)
    ) order by c.name)
    from public.customers c
  ), '[]'::jsonb);
end;
$$;

revoke all on function public.admin_list_customers() from public;
grant execute on function public.admin_list_customers() to anon, authenticated;

-- Create a customer and their first access code.
create or replace function public.admin_create_customer(
  p_name text,
  p_code text,
  p_label text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_name text;
  v_code text;
  v_label text;
  v_customer_id uuid;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  v_name := btrim(p_name);
  if v_name is null or char_length(v_name) < 1 or char_length(v_name) > 80 then
    raise exception using errcode = 'ZZ011', message = 'name_invalid';
  end if;

  v_code := upper(btrim(p_code));
  if v_code is null or char_length(v_code) < 1 or char_length(v_code) > 32 then
    raise exception using errcode = 'ZZ009', message = 'code_invalid';
  end if;

  v_label := btrim(p_label);
  if p_label is null or char_length(v_label) < 1 or char_length(v_label) > 40 then
    raise exception using errcode = 'ZZ007', message = 'label_invalid';
  end if;

  if exists (select 1 from public.access_codes a where a.code = v_code) then
    raise exception using errcode = 'ZZ009', message = 'code_taken';
  end if;

  insert into public.customers (name)
  values (v_name)
  returning id into v_customer_id;

  insert into public.access_codes (code, customer_id, is_active, label)
  values (v_code, v_customer_id, true, v_label);

  return jsonb_build_object(
    'customer_id', v_customer_id,
    'code', v_code,
    'label', v_label
  );
exception
  when unique_violation then
    raise exception using errcode = 'ZZ009', message = 'code_taken';
end;
$$;

revoke all on function public.admin_create_customer(text, text, text) from public;
grant execute on function public.admin_create_customer(text, text, text) to anon, authenticated;

-- Add another code to an existing customer.
create or replace function public.admin_add_code(
  p_customer_id uuid,
  p_code text,
  p_label text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_customer public.customers;
  v_code text;
  v_label text;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  select * into v_customer
  from public.customers
  where id = p_customer_id;

  if v_customer.id is null then
    raise exception using errcode = 'ZZ010', message = 'customer_not_found';
  end if;

  v_code := upper(btrim(p_code));
  if v_code is null or char_length(v_code) < 1 or char_length(v_code) > 32 then
    raise exception using errcode = 'ZZ009', message = 'code_invalid';
  end if;

  v_label := btrim(p_label);
  if p_label is null or char_length(v_label) < 1 or char_length(v_label) > 40 then
    raise exception using errcode = 'ZZ007', message = 'label_invalid';
  end if;

  if exists (select 1 from public.access_codes a where a.code = v_code) then
    raise exception using errcode = 'ZZ009', message = 'code_taken';
  end if;

  insert into public.access_codes (code, customer_id, is_active, label)
  values (v_code, p_customer_id, true, v_label);

  return jsonb_build_object(
    'customer_id', p_customer_id,
    'code', v_code,
    'label', v_label,
    'is_active', true
  );
exception
  when unique_violation then
    raise exception using errcode = 'ZZ009', message = 'code_taken';
end;
$$;

revoke all on function public.admin_add_code(uuid, text, text) from public;
grant execute on function public.admin_add_code(uuid, text, text) to anon, authenticated;

-- Activate or deactivate a code.
create or replace function public.admin_toggle_code(
  p_code text,
  p_active boolean
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_code public.access_codes;
begin
  if not public.is_admin() then
    raise exception using errcode = 'ZZ008', message = 'forbidden';
  end if;

  if p_active is null then
    raise exception using errcode = 'ZZ009', message = 'code_invalid';
  end if;

  update public.access_codes a
  set is_active = p_active
  where a.code = upper(btrim(p_code))
  returning * into v_code;

  if v_code.code is null then
    raise exception using errcode = 'ZZ010', message = 'code_not_found';
  end if;

  return jsonb_build_object(
    'code', v_code.code,
    'is_active', v_code.is_active
  );
end;
$$;

revoke all on function public.admin_toggle_code(text, boolean) from public;
grant execute on function public.admin_toggle_code(text, boolean) to anon, authenticated;
