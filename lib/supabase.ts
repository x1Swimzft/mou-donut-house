import { createClient, type SupabaseClient } from "@supabase/supabase-js";
import type { CustomerBill, MenuResult, MyBillsResult, OrderResult, Product, VerifyResult } from "./types";

type ProductRow = {
  id: string;
  name: string;
  price: number | string;
  category: string | null;
  is_available: boolean;
};

type VerifyRow = { customer_name?: unknown };
type OrderRow = { order_id?: unknown; total?: unknown };

let client: SupabaseClient | null = null;

function getClient(): SupabaseClient {
  if (client) return client;
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key) throw new Error("missing_supabase_env");
  client = createClient(url, key, {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false }
  });
  return client;
}

function firstRow<T>(data: T | T[] | null): T | null {
  if (!data) return null;
  return Array.isArray(data) ? (data[0] ?? null) : data;
}

function toProduct(row: ProductRow): Product {
  return {
    id: String(row.id),
    name: String(row.name),
    price: Number(row.price),
    category: row.category ? String(row.category) : null,
    is_available: Boolean(row.is_available)
  };
}

export async function verifyLogin(name: string, code: string): Promise<VerifyResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  try {
    const { data, error } = await getClient()
      .rpc("verify_login", { p_name: name, p_code: code })
      .abortSignal(controller.signal);
    if (error) return { ok: false, code: error.code ?? null };
    const row = firstRow(data as VerifyRow | VerifyRow[] | null);
    const customerName = row?.customer_name;
    if (typeof customerName !== "string") return { ok: false, code: null };
    return { ok: true, customerName };
  } catch {
    return { ok: false, code: null };
  } finally {
    clearTimeout(timer);
  }
}

export async function listMenu(code: string): Promise<MenuResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  try {
    const { data, error } = await getClient()
      .rpc("list_menu", { p_code: code })
      .abortSignal(controller.signal);
    if (error) return { ok: false, code: error.code ?? null };
    const rows = (Array.isArray(data) ? data : []) as ProductRow[];
    return { ok: true, products: rows.map(toProduct) };
  } catch {
    return { ok: false, code: null };
  } finally {
    clearTimeout(timer);
  }
}

export async function createOrder(
  code: string,
  items: { product_id: string; qty: number }[]
): Promise<OrderResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  try {
    const { data, error } = await getClient()
      .rpc("create_order", {
        p_code: code,
        p_items: items
      })
      .abortSignal(controller.signal);
    if (error) return { ok: false, code: error.code ?? null };
    const row = firstRow(data as OrderRow | OrderRow[] | null);
    if (!row || typeof row.order_id !== "string") return { ok: false, code: null };
    const total = Number(row.total);
    if (!Number.isFinite(total)) return { ok: false, code: null };
    return { ok: true, orderId: row.order_id, total };
  } catch {
    return { ok: false, code: null };
  } finally {
    clearTimeout(timer);
  }
}

export async function listMyBills(code: string): Promise<MyBillsResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  try {
    const { data, error } = await getClient()
      .rpc("my_bills", { p_code: code })
      .abortSignal(controller.signal);
    if (error) return { ok: false, code: error.code ?? null };
    const rows = (Array.isArray(data) ? data : []) as Array<Record<string, unknown>>;
    const bills: CustomerBill[] = rows.map((r) => ({
      bill_id: String(r.bill_id),
      status: String(r.status),
      business_date: String(r.business_date),
      opened_at: String(r.opened_at),
      closed_at: r.closed_at ? String(r.closed_at) : null,
      total: Number(r.total) || 0,
      items: Array.isArray(r.items)
        ? (r.items as Array<Record<string, unknown>>).map((item) => ({
            product_name: String(item.product_name),
            qty: Number(item.qty) || 0,
            line_total: Number(item.line_total) || 0
          }))
        : []
    }));
    return { ok: true, bills };
  } catch {
    return { ok: false, code: null };
  } finally {
    clearTimeout(timer);
  }
}

export type ProductUpdate = {
  id: string;
  name?: string;
  price?: number;
  category?: string | null;
  is_available: boolean;
};

export function subscribeProducts(
  onUpdate: (product: ProductUpdate) => void
): () => void {
  try {
    const supabase = getClient();
    const channel = supabase
      .channel("menu-products-realtime")
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: "products"
        },
        (payload) => {
          if (payload.eventType === "DELETE") return;
          const row = payload.new as Record<string, unknown> | null;
          if (!row || typeof row.id !== "string") return;
          onUpdate({
            id: row.id,
            name: typeof row.name === "string" ? row.name : undefined,
            price: row.price !== undefined ? Number(row.price) : undefined,
            category: typeof row.category === "string" ? row.category : null,
            is_available: Boolean(row.is_available)
          });
        }
      )
      .subscribe();

    return () => {
      void supabase.removeChannel(channel);
    };
  } catch {
    return () => {};
  }
}
