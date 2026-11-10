# Mou Donut House — Design Deliverables (Phase 1)

Static design mockups only. No Next.js, no cart logic, no API/database code, no secrets.

## Files

| Path | What |
|---|---|
| `design/tokens.md` | colors, rem type scale, spacing, control sizes, states, phase-2 handoff |
| `design/checklist.md` | elderly-accessibility checklist with WebAIM contrast and 360px/max-root results |
| `design/contrast-evidence.md` | raw WebAIM Contrast Checker API results for all used pairs |
| `design/skill-evidence.md` | skill paths, command outputs, and why `design` was not available |
| `design/mockups/mockup.css` | shared CSS; `rem` typography with 18px/20px hard floors |
| `design/mockups/01-code-360.html` / `01-code-large.html` | code entry, normal / large text |
| `design/mockups/02-menu-360.html` / `02-menu-large.html` | menu, normal / large text |
| `design/mockups/03-cart-360.html` / `03-cart-large.html` | cart + confirm, normal / large text |
| `design/mockups/04-success-360.html` / `04-success-large.html` | success, normal / large text |
| `design/mockups/screenshots/` | 8 normal/large captures + 4 max-root captures, 360 CSS px / DPR 2 |

Open any HTML file directly, or serve the folder:

```bash
cd design/mockups
python -m http.server 8765
# http://127.0.0.1:8765/01-code-360.html
```

The `-large` files set `html.large` (root 150%, body 24px, buttons 26.7px, touch 64px). Typography uses `rem` with `max()` floors, so browser/OS text scaling affects the layout. The `max-*.png` captures are a 200% root test (body 32px), not a physical-phone test.

`01-code` shows the invalid-code state so the error copy can be reviewed; the normal state is the same screen without the error panel.

## Superdesign

Project canvas:
https://superdesign.dev/teams/38a53270-5a68-4c8c-b294-0ae288b60530/projects/7448faf4-8bc6-4d24-a913-e46e40a4a211

Draft previews:
- Code: https://p.superdesign.dev/draft/cf3031b2-05b8-4dd6-8e51-ccb848ff467f
- Menu: https://p.superdesign.dev/draft/07b8cee0-6cfe-45f0-9bcc-51ee2c97a30a
- Cart: https://p.superdesign.dev/draft/6747bdd8-ce5d-45ae-8223-2021b966de56
- Success: https://p.superdesign.dev/draft/f2d74342-487c-41ec-8591-d1a26902912e

The Superdesign drafts were the exploration layer; the HTML/CSS files here are the reviewed mockups with exact rem sizes, 56/64px targets, and externally checked 7:1 contrast. Superdesign favored icon illustrations and a 4-digit code; the final mockups use alphanumeric code text, text-labeled quantity controls, and vertical product cards for large text.

## Skills used

- `impeccable` — loaded and applied for craft/quality floor. Its context CLI was not run because this project has no existing UI/DESIGN.md.
- `ui-ux-pro-max` — ran the design-system search; used its accessibility guidance. Its suggested blue/green palette was not used because it does not fit the donut shop; the warm palette was measured with WebAIM.
- `superdesign` — CLI used to create the project and 4 drafts on canvas (links above).
- `design-taste-frontend` — used for visual direction/house rules; elderly constraints override its motion/hover defaults.
- `ponytail` — active; kept scope to design artifacts only.
- **`design`** — not available in this session. `/mnt/skills/plugins/design/SKILL.md` is not mounted on this Windows host, and no `design` skill exists under the local skill roots. `design-taste-frontend` was used instead. Details and command output: `design/skill-evidence.md`.

## Physical-phone test

No physical phone or device farm is available in this environment. The design is `rem`-based and passes a 200% root test at 360px, but the review requirement is a real phone with the phone/browser text size set to maximum. To close it:

1. Serve `design/mockups` on the LAN: `python -m http.server 8765 --bind 0.0.0.0`.
2. On the phone, open the four `-360` pages via the PC's LAN IP.
3. Set the phone/browser text size to maximum.
4. Capture all four screens and confirm no clipping or overlap.

No fake screenshot is claimed for this item.

## Sample content and phase 2

Product names/prices, the order number `MD-2481`, and totals are design samples. Phase 2 must bind them to real products/orders and **replace the flat product illustrations with real shop photos** when available, keeping the same image plate and visible name/price. Keep the `rem` scaling and re-run the physical-phone max-font test.
