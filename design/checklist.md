# Design Checklist — Mou Donut House (Phase 1)

Scope: 4 screens, 8 static mockups (360px + large-text), no Next.js, no cart/API logic, no secrets.

Test setup: local static server → Chrome via WebBridge; viewport width **360 CSS px**, DPR 2; screenshots in `design/mockups/screenshots/`. Typography is `rem`-based: root 112.5% (18px at browser default), large variant root 150% (24px). This lets browser/OS text-size settings scale the layout instead of relying on a fixed class.

## A. Type sizes

| Requirement | Normal | Large variant | Max-root test (200%) |
|---|---|---|---|
| body / helper ≥ 18px | 18px | 24px | 32px |
| heading ≥ 24px | h1 32, h2 28, h3 24 | h1 42.7, h2 37.3, h3 32 | h1 56.9, h2 49.8, h3 42.7 |
| button label ≥ 20px | 20px | 26.7px | 35.6px |
| code input | 34px | 36px (cap) | 36px (cap) |
| order number | 48px | 56px (cap) | 56px (cap) |

Measured in browser via computed `font-size`; the normal button is `max(1.1111rem, 20px)` so it cannot fall below 20px. Minimums are enforced with `max()` in `mockup.css`.

## B. Touch targets and spacing

| Requirement | Value | Verified |
|---|---|---|
| control height ≥ 56px | 56px normal; 64px large | browser check: `small=0` on all 8 pages and all 4 max-root pages |
| code input height ≥ 56px | 73px normal / 83px large | measured |
| gap between controls ≥ 12px | 12px (`--gap-control`) | stack/qty/product cards use ≥12px |
| no tiny controls | full-width or ≥88px wide | qty buttons min-width 88px; cart bar full-width |

## C. Contrast — external verification

**Tool:** [WebAIM Contrast Checker API](https://webaim.org/resources/contrastchecker/) — JSON via `?fcolor=<hex>&bcolor=<hex>&api`. Checked on 2026-11-09. All pairs returned `AA: pass`, `AAA: pass`. Raw results: `design/contrast-evidence.md`.

| Foreground / background | WebAIM ratio | Used on |
|---|---|---|
| `#2A1712` on `#FFF6F2` | **16.0:1** | body, headings, brand |
| `#2A1712` on `#FFFFFF` | **17.0:1** | product/cart/order cards |
| `#2A1712` on `#F7E7DF` | **14.1:1** | pay note |
| `#5C443C` on `#FFF6F2` | **8.40:1** | helper, brand sub |
| `#5C443C` on `#FFFFFF` | **8.94:1** | order helper |
| `#5C443C` on `#F7E7DF` | **7.43:1** | pay-note body |
| `#5C443C` on `#EFE7E1` | **7.32:1** | “หมดชั่วคราว” |
| `#FFFFFF` on `#8E1B3A` | **8.86:1** | primary button |
| `#FFFFFF` on `#6B102A` | **12.1:1** | primary pressed |
| `#8E1B3A` on `#FFF6F2` | **8.32:1** | back link |
| `#8E1B3A` on `#FFFFFF` | **8.86:1** | price |
| `#FFFFFF` on `#145A3A` | **8.22:1** | success panel |
| `#701818` on `#FBE9E7` | **9.77:1** | error panel |
| `#493D37` on `#EFE7E1` | **8.56:1** | sold-out name/price |
| `#493D37` on `#E0D5CE` | **7.26:1** | disabled button |
| `#FFFFFF` on `#2A1712` | **17.0:1** | total panel |

Minimum used pair: **7.26:1** → passes the 7:1 requirement. Focus ring (cocoa on cream/white) is 16.0/17.0:1; 4px thick. Borders are 4px, not thin rules.

## D. Font

- **Noto Sans Thai** (400, 600, 700) with `Sarabun` fallback; loaded from Google Fonts in the mockups.
- One family; Thai copy throughout; no English UI terms except the brand and display order code.

## E. One primary task per screen

| Screen | Single task | Primary action |
|---|---|---|
| 01 Code | enter the customer code | “เข้าสู่ร้าน” |
| 02 Menu | choose items | “เพิ่ม” per item; one cart bar |
| 03 Cart | review and confirm | “ยืนยันการสั่งซื้อ” |
| 04 Success | keep the order and leave | “กลับไปหน้าเมนู” |

No nested menu, no tab bar. The cart screen is the required summary before the critical action.

## F. Icons, gestures, states

- No icon-only button. Quantity controls have visible “ลด” / “เพิ่ม”; the cart bar is text; product illustrations are decorative images, not controls.
- No `:hover` rule in `mockup.css`; grep result: **0**.
- No swipe, long-press, drag, or hidden gesture. Buttons only; `:active` darkens the primary surface; `:focus-visible` shows a 4px cocoa ring.
- Disabled/sold-out item uses explicit gray tokens, `disabled` attribute, and visible text “หมดชั่วคราว” / “เพิ่มไม่ได้”; no opacity.

## G. Human confirmation

Success page contains:

> **สั่งสำเร็จแล้ว**
> กรุณามารับและชำระเงินที่ร้าน

Order number `MD-2481` and total `125 บาท` appear in a large card with “เก็บหน้านี้ไว้แสดงที่ร้าน”.

## H. 360px, large text, and max-root test

Rendered all 8 files at **360 CSS px**, DPR 2. Browser measurement: `scrollWidth == clientWidth` on every page → **no horizontal overflow**.

| Page | Height normal | Height large | Max-root height | Overflow | Buttons pass |
|---|---|---|---|---|---|
| 01 Code | 900 | 1032 | 1353 | none | 1/1 |
| 02 Menu | 2341 | 2959 | 3262 | none | 7/7 |
| 03 Cart | 1133 | 1468 | 2132 | none | 5/5 |
| 04 Success | 900 | 900 | 1143 | none | 1/1 |

Max-root test = `document.documentElement.style.fontSize = '200%'` (body 32px, h1 56.9px). This is a browser simulation, **not** an OS font setting on a physical phone.

Physical-phone test (required by review): **BLOCKED in this environment** — no physical phone or device farm is available. The design uses `rem` and the max-root test passes; to close this item, open the pages on a real phone, set the phone/browser text size to maximum, and capture the four screens. No fake screenshot is claimed.

## I. Page-specific requirements

### 01 — Code entry
- [x] Single input; visible label “รหัสลูกค้า”; typed value shown large (`DN-TEST-0001`), letter-spaced, uppercase.
- [x] Large “เข้าสู่ร้าน” button (56px normal / 64px large).
- [x] Error text: “รหัสนี้ใช้ไม่ได้ กรุณาตรวจสอบอีกครั้ง” plus “หรือสอบถามพนักงานที่ร้าน”.
- [x] No 4-digit assumption; accepts the real alphanumeric code format.

### 02 — Menu
- [x] Sections “โดนัท” and “เครื่องดื่ม”.
- [x] 6 sample products; each has a flat illustration, Thai name, price, and “เพิ่ม” button.
- [x] Sold-out “โดนัทสตรอว์เบอร์รี” is gray, says “หมดชั่วคราว”, button disabled “เพิ่มไม่ได้”.
- [x] One cart bar shows “ดูตะกร้า · 3 ชิ้น” and “125 บาท”.

### 03 — Cart and confirm
- [x] Two line items with unit price, quantity, line total.
- [x] “ลด” / “เพิ่ม” buttons ≥56px (64px large); quantity visible.
- [x] Prominent total block “ยอดรวมทั้งหมด 125 บาท”.
- [x] Pay-at-shop note “ชำระเงินที่หน้าร้าน”.
- [x] “ยืนยันการสั่งซื้อ” is the only primary action; summary is above it.

### 04 — Success
- [x] Confirmation message in plain Thai.
- [x] Total “125 บาท” and order number “MD-2481” large and readable.
- [x] “กลับไปหน้าเมนู” single primary action.
- [x] No payment, QR, promotion, signup, or extra feature.

## J. Scope and phase-2 handoff

- [x] No Next.js, no cart logic, no API calls, no database code in this design task.
- [x] No service_role key or other secret in any mockup; only public font/CDN links.
- [x] No extra screens/features beyond the 4 requested.
- [x] Sample content (product list, order MD-2481, totals) is design-only and must be replaced with real data.
- **Phase 2 must replace the flat product illustrations with real shop photos** when available; keep the same image plate (88px normal / 96px large), square/round treatment, and visible name/price.
- **Phase 2 must keep the `rem` scaling** (root 112.5%/150%) and re-run the physical-phone max-font test.

## Result

**PASS on all measured design criteria**: all text pairs ≥ 7:1 (WebAIM external check), all controls ≥ 56px, body ≥ 18px, headings ≥ 24px, buttons ≥ 20px, no horizontal overflow at 360px including a 200% root test, no icon-only controls, no hover/gesture dependency, and the critical action has a visible summary.

**One item remains unverified:** the physical-phone max-font screenshot. This is an environment limitation, not a design claim.
