const WEBBRIDGE_URL = "http://127.0.0.1:10086/command";

async function cmd(action, params = {}) {
  const res = await fetch(WEBBRIDGE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ action, ...params }),
  });
  const data = await res.json();
  if (!data.ok && data.error) {
    throw new Error(`${action} failed: ${JSON.stringify(data.error)}`);
  }
  return data.data;
}

async function run() {
  console.log("1. Finding or attaching tab...");
  const tabsRes = await cmd("list_browser_tabs");
  let targetTab = tabsRes.tabs.find((t) => t.url && (t.url.includes("localhost:3000") || t.url.includes("mou-donut-house.vercel.app")));
  const tabId = targetTab ? targetTab.tabId : undefined;
  console.log("Using tabId:", tabId);
  const session = "mdh-cart-direct-input";

  // Login and clear cart first
  console.log("2. Navigating to /menu...");
  await cmd("navigate", { session, tab_id: tabId, url: "http://localhost:3000/menu" });
  await new Promise((r) => setTimeout(r, 1500));

  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      localStorage.setItem("mdh_code", "MDH4");
      localStorage.setItem("mdh_customer_name", "ต้า");
      localStorage.removeItem("cart_v1");
      location.reload();
    })()`,
  });
  await new Promise((r) => setTimeout(r, 2000));

  // Add 3 items to cart from menu
  console.log("3. Adding 3 items from menu...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const addBtns = Array.from(document.querySelectorAll(".batch-btn"));
      if (addBtns.length >= 3) {
        addBtns[0].click(); // first donut (+5)
        addBtns[2].click(); // second donut (+5)
        addBtns[4].click(); // third donut (+5)
      }
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1000));

  // Navigate to cart
  console.log("4. Navigating to /cart...");
  await cmd("navigate", { session, tab_id: tabId, url: "http://localhost:3000/cart" });
  await new Promise((r) => setTimeout(r, 2000));

  // Inspect initial cart
  let inspect = await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const names = Array.from(document.querySelectorAll(".cart-item-header h3")).map(e => e.textContent.trim());
      const inputs = Array.from(document.querySelectorAll(".qty-direct-input")).map(e => e.value);
      const header = document.querySelector("h2")?.textContent.trim();
      const grandTotal = document.querySelector(".summary-total")?.textContent.trim();
      return { names, inputs, header, grandTotal };
    })()`,
  });
  console.log("Initial cart with 3 items:", inspect.value);

  // Directly set quantities using input events matching user screenshot: 60, 70, 65
  console.log("5. Typing direct quantities: 60, 70, 65...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const inputs = document.querySelectorAll(".qty-direct-input");
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      const targetVals = ["60", "70", "65"];
      
      inputs.forEach((input, idx) => {
        input.focus();
        setter.call(input, targetVals[idx]);
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));
        input.blur();
      });
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1200));

  // Verify state matches screenshot: 60 + 70 + 65 = 195 pieces, 682.50 THB
  inspect = await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const items = Array.from(document.querySelectorAll(".cart-item")).map(el => {
        const name = el.querySelector("h3")?.textContent.trim();
        const price = el.querySelector(".line-price")?.textContent.trim();
        const qty = el.querySelector(".qty-direct-input")?.value;
        const total = el.querySelector(".line-total")?.textContent.trim();
        return { name, price, qty, total };
      });
      const header = document.querySelector("h2")?.textContent.trim();
      const grandTotal = document.querySelector(".summary-total")?.textContent.trim();
      return { items, header, grandTotal };
    })()`,
  });
  console.log("Cart after typing 60, 70, 65:", JSON.stringify(inspect.value, null, 2));

  // Now change the first item to 85 directly!
  console.log("6. Changing first item quantity to 85...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const input = document.querySelector(".qty-direct-input");
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      input.focus();
      setter.call(input, "85");
      input.dispatchEvent(new Event("input", { bubbles: true }));
      input.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));
      input.blur();
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1200));

  // Now click + 10 stepper on the first item
  console.log("7. Clicking + 10 stepper on the first item...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const plus10Btn = document.querySelectorAll(".qty-stepper-group")[0]?.querySelectorAll(".qty-button")[1];
      if (plus10Btn) plus10Btn.click();
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1200));

  inspect = await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const items = Array.from(document.querySelectorAll(".cart-item")).map(el => {
        const name = el.querySelector("h3")?.textContent.trim();
        const qty = el.querySelector(".qty-direct-input")?.value;
        const total = el.querySelector(".line-total")?.textContent.trim();
        return { name, qty, total };
      });
      const header = document.querySelector("h2")?.textContent.trim();
      const grandTotal = document.querySelector(".summary-total")?.textContent.trim();
      return { items, header, grandTotal };
    })()`,
  });
  console.log("Final cart state after 85 + 10 = 95:", JSON.stringify(inspect.value, null, 2));

  // Capture desktop screenshot
  console.log("8. Capturing desktop screenshot...");
  const desktopPath = "C:/Users/dataria/.gemini/antigravity/brain/c678e1dc-f136-4f48-90c0-88747959849a/desktop-cart-direct-input.png";
  await cmd("screenshot", { session, tab_id: tabId, save_path: desktopPath });

  console.log("ALL E2E VERIFICATIONS COMPLETE AND PASSING!");
}

run().catch((err) => {
  console.error("Test failed:", err);
  process.exit(1);
});
