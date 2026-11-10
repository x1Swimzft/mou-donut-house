import Link from "next/link";
import { formatMoney } from "@/lib/format";

export default function CartBar({ count, total }: { count: number; total: number }) {
  return (
    <aside className="cart-bar" aria-label="แถบสรุปตะกร้าสินค้า">
      <Link
        href="/cart"
        className="button primary cart-button"
        aria-label={`ดูตะกร้า ${count} ชิ้น ยอดรวม ${formatMoney(total)}`}
      >
        <div className="cart-bar-content">
          <span className="cart-bar-main">
            <span className="cart-bar-icon" aria-hidden="true">🛒</span>
            <span>ดูตะกร้า · {count} ชิ้น</span>
          </span>
          <span className="cart-bar-total">{formatMoney(total)}</span>
        </div>
      </Link>
    </aside>
  );
}
