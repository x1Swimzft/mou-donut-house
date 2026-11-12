import assert from "node:assert/strict";

// Mock localStorage & window for node environment before importing storage / cart
const store = new Map<string, string>();
const mockStorage = {
  getItem: (key: string) => store.get(key) ?? null,
  setItem: (key: string, val: string) => {
    store.set(key, String(val));
  },
  removeItem: (key: string) => {
    store.delete(key);
  },
  clear: () => {
    store.clear();
  },
  key: (index: number) => Array.from(store.keys())[index] ?? null,
  length: 0,
} as unknown as Storage;

(globalThis as unknown as { localStorage: Storage }).localStorage = mockStorage;
(globalThis as unknown as { window: unknown }).window = {
  localStorage: mockStorage,
};

// Now import cart modules
import { addToCart, changeQty, setLineQty, removeLine, countItems, estimateTotal } from "../lib/cart";
import { clearCartNote, getCartNote, getLastOrder, setLastOrder, setCartNote } from "../lib/storage";
import type { Product } from "../lib/types";

const mockProducts: Product[] = [
  { id: "p1", name: "ไส้กรอก", price: 3.5, category: null, is_available: true },
  { id: "p2", name: "หมูหยอง", price: 3.5, category: null, is_available: true },
  { id: "p3", name: "ลูกเกด", price: 3.5, category: null, is_available: true },
];

console.log("=== 1. Testing addToCart ===");
let cart = addToCart("p1", 5);
assert.equal(cart.length, 1);
assert.equal(cart[0].product_id, "p1");
assert.equal(cart[0].qty, 5);
assert.equal(countItems(cart), 5);
assert.equal(estimateTotal(cart, mockProducts), 17.5);
console.log("✓ addToCart passed: 5 pcs = 17.50 THB");

console.log("=== 2. Testing direct input: setLineQty to 85 ===");
cart = setLineQty("p1", 85);
assert.equal(cart.length, 1);
assert.equal(cart[0].qty, 85);
assert.equal(countItems(cart), 85);
assert.equal(estimateTotal(cart, mockProducts), 297.5);
console.log("✓ setLineQty(85) passed: 85 pcs = 297.50 THB");

console.log("=== 3. Testing steppers after direct input ===");
cart = changeQty("p1", 10);
assert.equal(cart[0].qty, 95);
assert.equal(estimateTotal(cart, mockProducts), 332.5);
console.log("✓ changeQty(+10) after direct input passed: 95 pcs = 332.50 THB");

cart = changeQty("p1", -5);
assert.equal(cart[0].qty, 90);
assert.equal(estimateTotal(cart, mockProducts), 315);
console.log("✓ changeQty(-5) after direct input passed: 90 pcs = 315.00 THB");

console.log("=== 4. Testing multiple items direct input ===");
cart = addToCart("p2", 20);
cart = setLineQty("p2", 70); // direct type 70
cart = addToCart("p3", 10);
cart = setLineQty("p3", 65); // direct type 65
cart = setLineQty("p1", 60); // match user screenshot: p1=60, p2=70, p3=65

assert.equal(cart.length, 3);
assert.equal(cart.find((c) => c.product_id === "p1")?.qty, 60);
assert.equal(cart.find((c) => c.product_id === "p2")?.qty, 70);
assert.equal(cart.find((c) => c.product_id === "p3")?.qty, 65);

const totalPieces = countItems(cart);
assert.equal(totalPieces, 195); // 60 + 70 + 65 = 195 pieces
const grandTotal = estimateTotal(cart, mockProducts);
assert.equal(grandTotal, 682.5); // 195 * 3.5 = 682.50 THB
console.log(`✓ Multiple items direct input: ${totalPieces} pcs = ${grandTotal} THB passed!`);

console.log("=== 5. Testing setLineQty edge cases (0, negative) ===");
cart = setLineQty("p1", 0);
assert.equal(cart.find((c) => c.product_id === "p1"), undefined, "p1 should be removed when set to 0");
assert.equal(cart.length, 2);

cart = setLineQty("p2", -10);
assert.equal(cart.find((c) => c.product_id === "p2"), undefined, "p2 should be removed when set to negative");
assert.equal(cart.length, 1);
assert.equal(cart[0].product_id, "p3");
console.log("✓ Zero & negative handling passed (removes line clean)");

console.log("=== 6. Testing cart note draft ===");
assert.equal(getCartNote(), "");
setCartNote("ไม่ใส่ถั่ว");
assert.equal(getCartNote(), "ไม่ใส่ถั่ว");
setCartNote("");
assert.equal(getCartNote(), "");
setCartNote("แยกถุง");
clearCartNote();
assert.equal(getCartNote(), "");
console.log("✓ cart note draft passed");

console.log("=== 7. Testing last order note ===");
setLastOrder({ order_id: "abc", total: 3.5, note: "รับตอน 15:00" });
assert.equal(getLastOrder()?.note, "รับตอน 15:00");
setLastOrder({ order_id: "abc", total: 3.5, note: null });
assert.equal(getLastOrder()?.note, null);
console.log("✓ last order note passed");

console.log("\nALL CART TESTS PASSED SUCCESSFULLY! ✓");
