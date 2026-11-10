import type { CartLine, LastOrder } from "./types";

const CODE_KEY = "mdh_code";
const NAME_KEY = "mdh_customer_name";
const CART_KEY = "cart_v1";
const ORDER_KEY = "mdh_last_order_v1";
const NOTICE_KEY = "mdh_notice_v1";

function hasWindow() {
  return typeof window !== "undefined";
}

function readJson<T>(key: string, fallback: T): T {
  if (!hasWindow()) return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson(key: string, value: unknown) {
  if (!hasWindow()) return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage can be unavailable in private mode; the UI still works in memory
    // for the current page, but persistence is not guaranteed.
  }
}

export function getCode(): string {
  if (!hasWindow()) return "";
  return window.localStorage.getItem(CODE_KEY) ?? "";
}

export function setCode(code: string) {
  if (!hasWindow()) return;
  window.localStorage.setItem(CODE_KEY, code);
}

export function clearCode() {
  if (!hasWindow()) return;
  window.localStorage.removeItem(CODE_KEY);
}

export function getCustomerName(): string {
  if (!hasWindow()) return "";
  return window.localStorage.getItem(NAME_KEY) ?? "";
}

export function setCustomerName(name: string) {
  if (!hasWindow()) return;
  window.localStorage.setItem(NAME_KEY, name);
}

export function getCart(): CartLine[] {
  const value = readJson<unknown>(CART_KEY, []);
  if (!Array.isArray(value)) return [];
  return value
    .filter((item): item is { product_id: string; qty: number } => {
      if (!item || typeof item !== "object") return false;
      const row = item as { product_id?: unknown; qty?: unknown };
      return typeof row.product_id === "string" && Number.isInteger(row.qty) && Number(row.qty) > 0;
    })
    .map((item) => ({ product_id: item.product_id, qty: item.qty }));
}

export function setCart(cart: CartLine[]) {
  writeJson(CART_KEY, cart);
}

export function getLastOrder(): LastOrder | null {
  const value = readJson<unknown>(ORDER_KEY, null);
  if (!value || typeof value !== "object") return null;
  const row = value as { order_id?: unknown; total?: unknown };
  if (typeof row.order_id !== "string" || !Number.isFinite(Number(row.total))) return null;
  return { order_id: row.order_id, total: Number(row.total) };
}

export function setLastOrder(order: LastOrder) {
  writeJson(ORDER_KEY, order);
}

export function getNotice(): string {
  if (!hasWindow()) return "";
  try {
    return window.sessionStorage.getItem(NOTICE_KEY) ?? "";
  } catch {
    return "";
  }
}

export function setNotice(message: string) {
  if (!hasWindow()) return;
  try {
    window.sessionStorage.setItem(NOTICE_KEY, message);
  } catch {
    // no-op
  }
}

export function clearNotice() {
  if (!hasWindow()) return;
  try {
    window.sessionStorage.removeItem(NOTICE_KEY);
  } catch {
    // no-op
  }
}