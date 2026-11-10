# External Contrast Verification — WebAIM

Tool: [WebAIM Contrast Checker API](https://webaim.org/resources/contrastchecker/)  
Method: `GET https://webaim.org/resources/contrastchecker/?fcolor=<FG>&bcolor=<BG>&api`  
Checked: 2026-11-09

All pairs returned `AA: pass`, `AALarge: pass`, `AAA: pass`, `AAALarge: pass`.

| Label | Foreground | Background | Ratio | AAA |
|---|---|---|---:|---|
| ink on bg | `2A1712` | `FFF6F2` | 16.0:1 | pass |
| ink on surface | `2A1712` | `FFFFFF` | 17.0:1 | pass |
| ink on alt | `2A1712` | `F7E7DF` | 14.1:1 | pass |
| muted on bg | `5C443C` | `FFF6F2` | 8.40:1 | pass |
| muted on surface | `5C443C` | `FFFFFF` | 8.94:1 | pass |
| muted on alt | `5C443C` | `F7E7DF` | 7.43:1 | pass |
| muted on disabled card | `5C443C` | `EFE7E1` | 7.32:1 | pass |
| white on primary | `FFFFFF` | `8E1B3A` | 8.86:1 | pass |
| white on pressed | `FFFFFF` | `6B102A` | 12.1:1 | pass |
| primary on bg | `8E1B3A` | `FFF6F2` | 8.32:1 | pass |
| primary on surface | `8E1B3A` | `FFFFFF` | 8.86:1 | pass |
| white on success | `FFFFFF` | `145A3A` | 8.22:1 | pass |
| error on error bg | `701818` | `FBE9E7` | 9.77:1 | pass |
| disabled on card | `493D37` | `EFE7E1` | 8.56:1 | pass |
| disabled on button | `493D37` | `E0D5CE` | 7.26:1 | pass |
| white on ink | `FFFFFF` | `2A1712` | 17.0:1 | pass |

Minimum: **7.26:1** (disabled button) — above the required 7:1.

Raw examples:

```json
{"ratio":"16.0","AA":"pass","AALarge":"pass","AAA":"pass","AAALarge":"pass"}
{"ratio":"8.86","AA":"pass","AALarge":"pass","AAA":"pass","AAALarge":"pass"}
{"ratio":"7.26","AA":"pass","AALarge":"pass","AAA":"pass","AAALarge":"pass"}
```

The local WCAG formula used during design produced the same values within ±0.01; this file records the external tool values.
