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
  console.log("1. Finding Brave tab for Vercel...");
  const tabsRes = await cmd("list_browser_tabs");
  // Prefer tab 1799293513 if present
  let targetTab = tabsRes.tabs.find((t) => t.tabId === 1799293513) ||
                  tabsRes.tabs.find((t) => t.url && t.url.includes("mou-donut-house.vercel.app/cart"));
  const tabId = targetTab ? targetTab.tabId : undefined;
  console.log("Using tabId:", tabId);
  const session = "mdh-verify-live";

  console.log("2. Navigating to menu on live Vercel to pick items...");
  await cmd("navigate", { session, tab_id: tabId, url: "https://mou-donut-house.vercel.app/menu" });
  await new Promise((r) => setTimeout(r, 2500));

  console.log("3. Adding donuts to cart...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const btns = Array.from(document.querySelectorAll(".batch-btn"));
      if (btns.length >= 6) {
        btns[0].click(); // donut 1 (+5)
        btns[2].click(); // donut 2 (+5)
        btns[4].click(); // donut 3 (+5)
      }
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1200));

  console.log("4. Navigating to /cart on live Vercel...");
  await cmd("navigate", { session, tab_id: tabId, url: "https://mou-donut-house.vercel.app/cart" });
  await new Promise((r) => setTimeout(r, 2500));

  console.log("5. Inspecting inputs on live Vercel...");
  let inspect = await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const inputs = Array.from(document.querySelectorAll(".qty-direct-input"));
      const items = Array.from(document.querySelectorAll(".cart-item")).map(el => ({
        name: el.querySelector("h3")?.textContent.trim(),
        qty: el.querySelector(".qty-direct-input")?.value,
        total: el.querySelector(".line-total")?.textContent.trim()
      }));
      const header = document.querySelector("h2")?.textContent.trim();
      const grandTotal = document.querySelector(".summary-total")?.textContent.trim();
      return { inputsFound: inputs.length, items, header, grandTotal };
    })()`,
  });
  console.log("Initial live cart items:", JSON.stringify(inspect.value, null, 2));

  console.log("6. Directly typing quantities 60, 70, 65 on live Vercel...");
  await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const inputs = document.querySelectorAll(".qty-direct-input");
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      const targets = ["60", "70", "65"];
      inputs.forEach((input, i) => {
        input.focus();
        setter.call(input, targets[i]);
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));
        input.blur();
      });
    })()`,
  });
  await new Promise((r) => setTimeout(r, 1500));

  inspect = await cmd("evaluate", {
    session,
    tab_id: tabId,
    code: `(() => {
      const items = Array.from(document.querySelectorAll(".cart-item")).map(el => ({
        name: el.querySelector("h3")?.textContent.trim(),
        qty: el.querySelector(".qty-direct-input")?.value,
        total: el.querySelector(".line-total")?.textContent.trim()
      }));
      const header = document.querySelector("h2")?.textContent.trim();
      const grandTotal = document.querySelector(".summary-total")?.textContent.trim();
      const badge = document.querySelector(".header-cart-badge")?.textContent.trim();
      return { items, header, grandTotal, badge };
    })()`,
  });
  console.log("Updated live cart items:", JSON.stringify(inspect.value, null, 2));

  console.log("7. Taking live screenshot...");
  const ss = await cmd("screenshot", {
    session,
    tab_id: tabId,
    save_path: "live-vercel-cart-direct-input.png",
  });
  console.log("Screenshot res:", ss);
}

run().catch((err) => {
  console.error("Live test failed:", err);
  process.exit(1);
});
