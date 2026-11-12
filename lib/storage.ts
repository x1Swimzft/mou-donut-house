import type { CartLine, LastOrder } from "./types";

const CODE_KEY = "mdh_code";
const NAME_KEY = "mdh_customer_name";
const CART_KEY = "cart_v1";
const CART_NOTE_KEY = "mdh_cart_note_v1";
const ORDER_KEY = "mdh_last_order_v1";
const NOTICE_KEY = "mdh_notice_v1";

// In-memory store (RAM) - resilient against Safari iOS 15 Private Browsing QuotaExceededError
// and strict cross-site / cookie blocking.
const memoryStore = new Map<string, string>();

function hasWindow(): boolean {
  return typeof window !== "undefined";
}

function hasDocument(): boolean {
  return typeof document !== "undefined";
}

function getCookie(name: string): string {
  if (!hasDocument()) return "";
  try {
    const encodedName = encodeURIComponent(name);
    const cookies = document.cookie.split(";");
    for (const c of cookies) {
      const trimmed = c.trim();
      if (trimmed.startsWith(encodedName + "=")) {
        return decodeURIComponent(trimmed.substring(encodedName.length + 1));
      }
    }
  } catch {
    // Cookie reading might throw in sandboxed environments
  }
  return "";
}

function setCookie(name: string, value: string, days = 365): void {
  if (!hasDocument()) return;
  try {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`;
  } catch {
    // Cookie writing might throw if cookies are completely disabled
  }
}

function deleteCookie(name: string): void {
  if (!hasDocument()) return;
  try {
    document.cookie = `${encodeURIComponent(name)}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; SameSite=Lax`;
  } catch {
    // ignore
  }
}

function getFromLocalStorage(key: string): string {
  if (!hasWindow()) return "";
  try {
    return window.localStorage.getItem(key) ?? "";
  } catch {
    // In Safari iOS 15 Private Browsing or with blocked cookies, accessing localStorage can throw SecurityError
    return "";
  }
}

function setToLocalStorage(key: string, value: string): void {
  if (!hasWindow()) return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Throws QuotaExceededError in iOS Safari Private Browsing mode
  }
}

function removeFromLocalStorage(key: string): void {
  if (!hasWindow()) return;
  try {
    window.localStorage.removeItem(key);
  } catch {
    // ignore
  }
}

// Multi-tier storage: RAM -> localStorage -> Cookie
function readStorageItem(key: string): string {
  // 1. In-memory check (fastest, guaranteed working within SPA session)
  const mem = memoryStore.get(key);
  if (mem) return mem;

  // 2. localStorage check
  const local = getFromLocalStorage(key);
  if (local) {
    memoryStore.set(key, local);
    return local;
  }

  // 3. Cookie check (persists across navigation even when localStorage is 0-quota in Safari Private Browsing)
  const cookie = getCookie(key);
  if (cookie) {
    memoryStore.set(key, cookie);
    setToLocalStorage(key, cookie);
    return cookie;
  }

  return "";
}

function writeStorageItem(key: string, value: string, persistCookie = true): void {
  // 1. Save in RAM
  memoryStore.set(key, value);

  // 2. Save in localStorage (safely)
  setToLocalStorage(key, value);

  // 3. Save in Cookie (for iOS 15 Safari Private Browsing resilience)
  if (persistCookie) {
    setCookie(key, value);
  }
}

function deleteStorageItem(key: string): void {
  memoryStore.delete(key);
  removeFromLocalStorage(key);
  deleteCookie(key);
}

function readJson<T>(key: string, fallback: T): T {
  const raw = readStorageItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson(key: string, value: unknown) {
  try {
    const raw = JSON.stringify(value);
    writeStorageItem(key, raw, true);
  } catch {
    // ignore serialization failure
  }
}

export function getCode(): string {
  return readStorageItem(CODE_KEY);
}

export function setCode(code: string) {
  writeStorageItem(CODE_KEY, code, true);
}

export function clearCode() {
  deleteStorageItem(CODE_KEY);
}

export function getCustomerName(): string {
  return readStorageItem(NAME_KEY);
}

export function setCustomerName(name: string) {
  writeStorageItem(NAME_KEY, name, true);
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
  if (hasWindow() && typeof window.dispatchEvent === "function") {
    try {
      window.dispatchEvent(new Event("mdh_cart_change"));
    } catch {
      // Ignore in non-standard window envs
    }
  }
}

export function getCartNote(): string {
  return readStorageItem(CART_NOTE_KEY);
}

export function setCartNote(note: string) {
  if (note) {
    writeStorageItem(CART_NOTE_KEY, note, true);
  } else {
    deleteStorageItem(CART_NOTE_KEY);
  }
}

export function clearCartNote() {
  setCartNote("");
}

export function getLastOrder(): LastOrder | null {
  const value = readJson<unknown>(ORDER_KEY, null);
  if (!value || typeof value !== "object") return null;
  const row = value as { order_id?: unknown; total?: unknown; note?: unknown };
  if (typeof row.order_id !== "string" || !Number.isFinite(Number(row.total))) return null;
  const note = typeof row.note === "string" && row.note.trim() ? row.note : null;
  return { order_id: row.order_id, total: Number(row.total), note };
}

export function setLastOrder(order: LastOrder) {
  writeJson(ORDER_KEY, order);
}

export function getNotice(): string {
  const mem = memoryStore.get(NOTICE_KEY);
  if (mem) return mem;

  if (!hasWindow()) return "";
  try {
    return window.sessionStorage.getItem(NOTICE_KEY) ?? "";
  } catch {
    return "";
  }
}

export function setNotice(message: string) {
  memoryStore.set(NOTICE_KEY, message);
  if (!hasWindow()) return;
  try {
    window.sessionStorage.setItem(NOTICE_KEY, message);
  } catch {
    // no-op in private mode
  }
}

export function clearNotice() {
  memoryStore.delete(NOTICE_KEY);
  if (!hasWindow()) return;
  try {
    window.sessionStorage.removeItem(NOTICE_KEY);
  } catch {
    // no-op in private mode
  }
}
