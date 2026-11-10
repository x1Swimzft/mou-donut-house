import { getCart, setCart } from "./storage";
import type { CartLine, Product } from "./types";

export function addToCart(productId: string, amount: number = 5): CartLine[] {
  const cart = getCart();
  const existing = cart.find((line) => line.product_id === productId);
  const next = existing
    ? cart.map((line) =>
        line.product_id === productId ? { ...line, qty: line.qty + amount } : line
      )
    : [...cart, { product_id: productId, qty: amount }];
  setCart(next);
  return next;
}

export function changeQty(productId: string, delta: number): CartLine[] {
  const cart = getCart();
  const next = cart
    .map((line) =>
      line.product_id === productId ? { ...line, qty: line.qty + delta } : line
    )
    .filter((line) => line.qty > 0);
  setCart(next);
  return next;
}

export function removeLine(productId: string): CartLine[] {
  const next = getCart().filter((line) => line.product_id !== productId);
  setCart(next);
  return next;
}

export function countItems(cart: CartLine[]): number {
  return cart.reduce((sum, line) => sum + line.qty, 0);
}

export function estimateTotal(cart: CartLine[], products: Product[]): number {
  const priceById = new Map(products.map((product) => [product.id, product.price]));
  return cart.reduce((sum, line) => sum + (priceById.get(line.product_id) ?? 0) * line.qty, 0);
}