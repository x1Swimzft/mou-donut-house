"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearCode, clearNotice, getCart, getCustomerName, getCode } from "@/lib/storage";
import { countItems } from "@/lib/cart";

export default function BrandHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [customerName, setName] = useState<string | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [cartCount, setCartCount] = useState(0);

  useEffect(() => {
    function sync() {
      setName(getCustomerName());
      setCode(getCode());
      setCartCount(countItems(getCart()));
    }
    sync();
    window.addEventListener("mdh_cart_change", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("mdh_cart_change", sync);
      window.removeEventListener("storage", sync);
    };
  }, [pathname]);

  function handleLogout() {
    clearCode();
    clearNotice();
    router.replace("/");
  }

  const isLoggedIn = !!code;

  return (
    <header className="brand-header">
      <div className="brand-header-inner">
        <Link href={isLoggedIn ? "/menu" : "/"} className="brand-logo-group">
          <div className="brand-icon" aria-hidden="true">
            🍩
          </div>
          <div className="brand-text">
            <span className="brand-title">Mou Donut House</span>
            <span className="brand-tagline">หมู · บ้านโดนัท ขนมโฮมเมดสดใหม่</span>
          </div>
        </Link>

        {isLoggedIn ? (
          <div className="brand-nav">
            <div className="customer-chip" title={`รหัสเข้าใช้: ${code}`}>
              <span className="customer-avatar" aria-hidden="true">👤</span>
              <span className="customer-name">{customerName || "ลูกค้าประจำ"}</span>
            </div>

            <nav className="header-links" aria-label="เมนูหลัก">
              <Link
                href="/menu"
                className={`header-nav-link${pathname === "/menu" ? " active" : ""}`}
              >
                หน้าเมนู
              </Link>
              <Link
                href="/bills"
                className={`header-nav-link${pathname === "/bills" ? " active" : ""}`}
              >
                บิลของฉัน
              </Link>
              <Link
                href="/cart"
                className={`header-nav-link cart-link${pathname === "/cart" ? " active" : ""}`}
              >
                <span>ตะกร้า</span>
                {cartCount > 0 ? (
                  <span className="header-cart-badge">{cartCount}</span>
                ) : null}
              </Link>
              <button
                type="button"
                className="header-logout-button"
                onClick={handleLogout}
                title="ออกจากระบบ"
              >
                ออก
              </button>
            </nav>
          </div>
        ) : null}
      </div>
    </header>
  );
}
