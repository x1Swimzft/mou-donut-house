# Mou Donut House — Design Tokens (Phase 1)

Direction: **อบอุ่น ชัดเจน เหมือนป้ายร้านที่อ่านจากระยะไกล** — warm cream paper, dark cocoa ink, deep raspberry action. Flat surfaces, thick edges, no decoration that competes with reading.

## 1. Color

| Token | Hex | Role |
|---|---|---|
| `--color-bg` | `#FFF6F2` | page ground (warm cream) |
| `--color-surface` | `#FFFFFF` | cards, input |
| `--color-surface-alt` | `#F7E7DF` | pay-at-shop note, image plate |
| `--color-ink` | `#2A1712` | body text, strong borders |
| `--color-ink-muted` | `#5C443C` | helper text, secondary labels |
| `--color-primary` | `#8E1B3A` | primary button, price, links |
| `--color-primary-pressed` | `#6B102A` | pressed primary |
| `--color-on-primary` | `#FFFFFF` | text on primary |
| `--color-success` | `#145A3A` | success panel |
| `--color-on-success` | `#FFFFFF` | text on success panel |
| `--color-error-bg` | `#FBE9E7` | error panel ground |
| `--color-error-ink` | `#701818` | error text |
| `--color-disabled-bg` | `#EFE7E1` | sold-out / disabled card ground |
| `--color-disabled-button` | `#E0D5CE` | disabled button ground |
| `--color-disabled-ink` | `#493D37` | sold-out / disabled text |
| `--color-focus` | `#2A1712` | 4px focus ring |

Rules:
- Text is never lighter than `--color-ink-muted`; no opacity on text.
- Disabled items use the explicit gray pair, never `opacity`.
- Borders are 4px `--color-ink` or a filled surface, never 1px hairlines.
- No gradients, no shadows at rest, no glow.

Contrast verified with **WebAIM Contrast Checker API** — full raw results in `design/contrast-evidence.md`:

| Pair | WebAIM ratio |
|---|---|
| ink on bg / surface / surface-alt | 16.0 / 17.0 / 14.1 |
| muted on bg / surface / surface-alt | 8.40 / 8.94 / 7.43 |
| white on primary / pressed | 8.86 / 12.1 |
| primary on bg / surface | 8.32 / 8.86 |
| white on success | 8.22 |
| error ink on error bg | 9.77 |
| disabled ink on disabled card | 8.56 |
| disabled ink on disabled button | 7.26 |
| white on ink | 17.0 |

Minimum used pair: **7.26:1**. All checked pairs returned WebAIM `AAA: pass`.

## 2. Typography

Family: **Noto Sans Thai** (fallback `Sarabun`, then system Thai sans). Weights 400, 600, 700. One family only.

Typography is implemented in `rem`: root `112.5%` (18px at browser default) for normal, `150%` (24px) for the large variant. Minimums are enforced with `max()` so a smaller browser default cannot take body below 18px or buttons below 20px.

| Role | Normal | Large-text | Hard minimum |
|---|---|---|---|
| body / helper | 18px | 24px | 18px |
| small label | 18px | 24px | 18px |
| h1 (page task) | 32px | 42.7px | 24px |
| h2 (section) | 28px | 37.3px | 24px |
| h3 (product / item) | 24px | 32px | 24px |
| price | 22px | 29.3px | 18px |
| button label | 20px | 26.7px | 20px |
| brand wordmark | 24px | 32px | 24px |
| code input | 34px | 36px (cap) | 18px |
| order number | 48px | 56px (cap) | 24px |

Line height: body 1.7; headings 1.3. Letter spacing: body 0; code input `0.08em`; order number `0.06em`.

At browser max text (root 200%, body 32px) the layout still fits 360px: h1 56.9px, buttons 35.6px, no horizontal overflow. Screenshots: `design/mockups/screenshots/max-*.png`.

## 3. Space, size, shape

| Token | Value |
|---|---|
| spacing scale | 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48px |
| minimum touch height | 56px (large-text 64px) |
| minimum gap between controls | 12px |
| card radius | 20px |
| control radius | 16px |
| border width | 4px |
| screen width | 360px, centered |

The menu cart bar is 64px tall (normal) and wraps to two lines if the text grows; page content reserves 140px / 190px bottom padding.

## 4. States

| State | Treatment |
|---|---|
| normal primary | `#8E1B3A` ground, white label, 4px same-color border |
| pressed primary | `#6B102A` ground (no hover state) |
| secondary | white ground, cocoa 4px border, cocoa label |
| focus | 4px cocoa outline, 3px offset, visible on all controls |
| disabled / sold out | `#EFE7E1` card ground; `#E0D5CE` button ground; `#493D37` text; `disabled` attribute; no pointer |
| error | `#FBE9E7` ground, `#701818` text, 4px error border |

No `:hover` styles are used anywhere. Interaction is tap/press only; no swipe, long-press, drag, or hidden gesture.

## 5. Content rules

- Thai only, everyday words. No “token”, “error”, “cart”, “API”, “QR”, “login”.
- One primary task per screen; one primary button.
- Every icon-like mark is paired with visible text; no icon-only buttons.
- Prices use `บาท`; totals are shown as a labeled block, not small print.
- The critical action is reviewed on the cart screen before “ยืนยันการสั่งซื้อ”.

## 6. Phase 2 handoff

- Keep the `rem` scaling and the hard minimums; do not replace them with fixed px in the app.
- Replace flat product illustrations with real shop photos when available; keep the 88px/96px image plate and the visible name/price.
- Re-run the physical-phone max-font test; the mockup max-root test is a browser simulation only.
