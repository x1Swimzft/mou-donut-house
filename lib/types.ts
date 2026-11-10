export type Product = {
  id: string;
  name: string;
  price: number;
  category: string | null;
  is_available: boolean;
};

export type CartLine = {
  product_id: string;
  qty: number;
};

export type LastOrder = {
  order_id: string;
  total: number;
};

export type CustomerBillItem = {
  product_name: string;
  qty: number;
  line_total: number;
};

export type CustomerBill = {
  bill_id: string;
  status: string; // 'open' | 'closed'
  business_date: string;
  opened_at: string;
  closed_at: string | null;
  total: number;
  items: CustomerBillItem[];
};

export type VerifyResult =
  | { ok: true; customerName: string }
  | { ok: false; code: string | null };

export type MenuResult =
  | { ok: true; products: Product[] }
  | { ok: false; code: string | null };

export type OrderResult =
  | { ok: true; orderId: string; total: number }
  | { ok: false; code: string | null };

export type MyBillsResult =
  | { ok: true; bills: CustomerBill[] }
  | { ok: false; code: string | null };
