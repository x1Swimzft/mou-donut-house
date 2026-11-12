import assert from "node:assert/strict";

// Simulate Safari iOS 15 Private Browsing where localStorage.setItem throws QuotaExceededError
const brokenStorage = {
  getItem: () => {
    // In some iOS versions it returns null or throws SecurityError
    return null;
  },
  setItem: () => {
    const err = new Error("QuotaExceededError: The quota has been exceeded.");
    err.name = "QuotaExceededError";
    throw err;
  },
  removeItem: () => {},
  clear: () => {},
  key: () => null,
  length: 0,
} as unknown as Storage;

// Simulate document.cookie
let cookieJar = "";
const mockDocument = {
  get cookie() {
    return cookieJar;
  },
  set cookie(val: string) {
    // Parse key=value
    const [pair] = val.split(";");
    const [k, v] = pair.split("=");
    if (val.includes("expires=Thu, 01 Jan 1970")) {
      // delete
      const cookies = cookieJar.split("; ").filter((c) => !c.startsWith(k + "="));
      cookieJar = cookies.join("; ");
    } else {
      const cookies = cookieJar ? cookieJar.split("; ").filter((c) => !c.startsWith(k + "=")) : [];
      cookies.push(`${k}=${v}`);
      cookieJar = cookies.join("; ");
    }
  }
};

(globalThis as unknown as { window: unknown }).window = {
  localStorage: brokenStorage,
  sessionStorage: brokenStorage,
};
(globalThis as unknown as { document: unknown }).document = mockDocument;

// Now import storage
import {
  getCode,
  setCode,
  clearCode,
  getCustomerName,
  setCustomerName,
  getNotice,
  setNotice,
  clearNotice
} from "../lib/storage";

console.log("=== Testing Safari iOS 15 Private Browsing Storage Fallbacks ===");

// 1. setCode should NOT throw even when localStorage throws QuotaExceededError
assert.doesNotThrow(() => {
  setCode("MDH4");
}, "setCode must not crash on QuotaExceededError");
console.log("✓ setCode did not throw on QuotaExceededError");

// 2. getCode should retrieve the saved code via Cookie or in-memory fallback
const retrievedCode = getCode();
assert.equal(retrievedCode, "MDH4", "getCode must return 'MDH4' even when localStorage failed");
console.log("✓ getCode successfully returned 'MDH4' from fallback");

// 3. Customer name should work similarly
assert.doesNotThrow(() => {
  setCustomerName("ต้า");
});
assert.equal(getCustomerName(), "ต้า", "getCustomerName must return 'ต้า' from fallback");
console.log("✓ setCustomerName / getCustomerName passed");

// 4. clearCode should clear cookie and memory
clearCode();
assert.equal(getCode(), "", "getCode must be empty after clearCode");
console.log("✓ clearCode passed");

// 5. Notice should work in-memory even when sessionStorage throws
assert.doesNotThrow(() => {
  setNotice("ข้อความทดสอบ");
});
assert.equal(getNotice(), "ข้อความทดสอบ");
clearNotice();
assert.equal(getNotice(), "");
console.log("✓ Notice fallback passed");

console.log("\nALL SAFARI IOS 15 STORAGE FALLBACK TESTS PASSED SUCCESSFULLY! ✓");
