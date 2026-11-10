# Mou Donut House — Customer Web

Next.js 16 (App Router) + TypeScript + Tailwind CSS 4 + `@supabase/supabase-js`.

Customers sign in with the customer name and the access code the shop issued (their password), choose items from the 17-item shop menu, review the cart, and place an order. Payment happens at the shop. There is no payment in the web app.

## Run locally

```bash
npm install
cp .env.example .env.local
# fill NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_ANON_KEY (publishable key)
npm run dev
```

`.env.local` is gitignored. Never put a service_role key in the app or in `.env*`.

For a build/test without `.env.local`:

```bash
NEXT_PUBLIC_SUPABASE_URL=... NEXT_PUBLIC_SUPABASE_ANON_KEY=... npm run build
npm run start
```

## Pages

| Route | Purpose |
|---|---|
| `/` | Verify customer name + access code with `verify_login`; store `mdh_code` + `mdh_customer_name`; go to menu |
| `/menu` | `list_menu` returns all products + `is_available`; one `donut` section with 17 items; add to `cart_v1` |
| `/cart` | Review items, increase/decrease/remove, client estimate, confirm via `create_order` |
| `/success` | Server `order_id` (short + full) and server `total`; clear `cart_v1` |

### Login (Phase 4)

- `/` asks for ชื่อลูกค้า and รหัสผ่าน, then calls `verify_login(p_name, p_code)`; the RPC returns only the canonical `customer_name`.
- A wrong name and an unknown code both return `ZZ001`. The name is checked before `is_active`, so a disabled code only reveals `ZZ002` when the name matches.
- `access_codes.code` stays globally unique and a customer may still hold several codes. Each code maps to exactly one customer, so `create_order(p_code)` and `my_bills(p_code)` need no name and are unchanged.
- The name is a confirmation step, not a security boundary: a code alone still authenticates `create_order`/`my_bills`, so treat codes as secrets.
- Codes are stored uppercase (`admin_create_customer`/`admin_add_code` uppercase them) and the login form uppercases input.

## Data and constraints

- `cart_v1` stores only `{ product_id, qty }`. Prices are loaded from `list_menu`; the server recalculates the real total.
- No price/total is sent in `p_items`. The RPC receives only `product_id` and `qty`.
- Error codes are mapped to plain Thai in `lib/errors.ts`; raw codes/errors are never shown.
- `create_order`, `verify_login`, `verify_access_code`, and `list_menu` are called with the publishable key only. No service_role anywhere.
- Migrations:
  - `supabase/migrations/20261109000000_phase1_init.sql`
  - `supabase/migrations/20261109180000_phase2_verify_code.sql`
  - `supabase/migrations/20261109210000_phase2_list_menu.sql`
  - `supabase/migrations/20261109211000_phase2_thai_product_names.sql`
  - `supabase/migrations/20261109230000_phase2_5_bills.sql`
  - `supabase/migrations/20261109233000_phase2_5_bangkok_business_date.sql`
  - `supabase/migrations/20261109234500_phase2_6_real_menu_17_items.sql`
  - `supabase/migrations/20261110000000_phase2_7_product_sort_order.sql`
  - `supabase/migrations/20261110010000_phase4_customer_admin.sql`
  - `supabase/migrations/20261110020000_phase4_login_name_code.sql`
  - `supabase/migrations/20261110030000_phase5_admin_day_bills.sql`
- SQL tests:
  - `supabase/tests/phase1_acceptance.sql`
  - `supabase/tests/phase2_verify_code.sql`
  - `supabase/tests/phase2_list_menu.sql`
  - `supabase/tests/phase2_5_bills.sql`
  - `supabase/tests/phase4_customer_admin.sql`
  - `supabase/tests/phase4_login.sql`
  - `supabase/tests/phase5_admin_day_bills.sql`
  - `supabase/tests/anon_rest.sh`

### Menu RPC and sold-out items

Phase 7B changed this: `products_select_public` is now `using (true)` for `anon` and `authenticated`, and `products` uses `replica identity full`, so Supabase Realtime delivers both `true → false` and `false → true` UPDATE events to the customer web. Direct anon reads therefore include sold-out rows. `list_menu(p_code text)` still verifies the customer code and returns all products with exactly `id`, `name`, `price`, `category`, `is_available`; the menu and cart still use this RPC only and keep **no client product cache**, so a fresh browser shows unavailable products gray as “หมดชั่วคราว”.

The menu/cart also handle `ZZ001`/`ZZ002` from `list_menu` by clearing the stored code, showing the Thai message on `/`, and re-entering the code.

### Products (Phase 2.6)

All 17 products use `category = 'donut'`, so the menu renders one section. Prices are per piece (บาท):

| 3.50 | 4.00 |
|---|---|
| ไส้กรอก, หมูหยอง, ลูกเกด, สับปะรด, ถั่วแดง, เผือก, สังขยา, ปูอัด, ไก่, แฮม, มันม่วง, ข้าวโพด, โดนัท, ตุ๊กตา | มะพร้าว, ช็อคโกแลต, พิซซ่า |

`list_menu` orders by `sort_order, name`, so the menu follows the paper order 1–17. The existing donut illustration is used for every item; per-item art/photos are a later design task.

### Bills (Phase 2.5)

- One customer can have multiple access codes (`access_codes.label`, no unique on `customer_id`).
- A customer has at most one `open` bill (partial unique index).
- Every order carries `bill_id` and `business_date` (`Asia/Bangkok`).
- `my_bills(p_code)` returns only that customer's bills and items.
- `close_bill`, `close_all_bills`, and `store_summary` are admin RPCs; anon calls return `ZZ008`, and anon direct reads of `bills`/`bill_items` are denied.

## UX

- All four pages are Thai, plain language, no technical terms.
- Body ≥18px, headings ≥24px, button text ≥20px, controls ≥56px; design tokens are in `design/tokens.md` and copied into `app/globals.css`.
- No hover-only interaction, no swipe/long-press, no icon-only buttons.
- 360px and root-font 200% checks are in `phase2-evidence/`, including the physical vivo V2318 test.
- Critical action is reviewed on `/cart` before “ยืนยันการสั่งซื้อ”.

## Acceptance evidence

`phase2-evidence/ACCEPTANCE.md` contains the SQL/RPC/UI evidence. `phase2-evidence/` screenshots are light-mode at 360 CSS px, including:

- `menu-fresh-soldout-light-360.png` — fresh browser, `products_cache_v1=null`, sold-out item gray via `list_menu`
- `menu-light-360.png`, `cart-light-360.png`, `success-light-360.png`
- `code-light-360.png`, `wrong-code-light-360.png`, `inactive-code-light-360.png`, `empty-cart-light-360.png`, `offline-light-360.png`
- `max-menu-light-360.png`, `max-cart-light-360.png`, `max-success-light-360.png`
- `phone-code-light-max.png`, `phone-menu-light-max.png`, `phone-cart-light-max.png`, `phone-success-light-max.png`

## Admin Android app (Phase 3)

`android-admin/` is a Kotlin + Jetpack Compose app for the shop admin.

- Login with Supabase Auth (email/password); the account must exist in `public.admins`. There is no signup in the app.
- “จดจำรหัส” checkbox stores the email/password encrypted with Android Keystore (AES-GCM) and prefills them after logout/restart.
- Open bills with customer, total, items, and opened time.
- Close one bill from its detail screen.
- Close all currently displayed open bills with `close_all_bills`; bills opened after the list was loaded stay open.
- Daily summary from `store_summary`: total, closed bills, and customers without orders (Asia/Bangkok date).
- “บิลของวันนี้” from `admin_day_bills(p_business_date)`: every bill with at least one order that day, open or closed, with `day_total` vs `bill_total` and that day's order count. Cards open the existing `bill/{id}` route.
- Bill detail lists each order with its time, items and total, loaded from `orders` + `order_items` through PostgREST (admin RLS), and can close an open bill with `close_bill`.
- “ลูกค้า” tab: list customers with their codes, create a customer with a first code, add a code, and switch a code on/off via `admin_list_customers`, `admin_create_customer`, `admin_add_code`, `admin_toggle_code`. New codes are stored uppercase; label 1–40, name 1–80, code 1–32.

Build:

```bash
cd android-admin
# JAVA_HOME must point to Android Studio's JBR or another JDK 17+
./gradlew assembleDebug
```

`android-admin/local.properties` (gitignored) needs:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<publishable key>
```

No `service_role` is used. Phase 3 evidence is in `phase3-evidence/`.

## Not included

No membership, customer accounts/auth, QR/payment, promotions, stock system, product options, or production deployment. Customer access is a shop-issued name + code, not an account.
