"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import BrandHeader from "@/components/BrandHeader";
import Notice from "@/components/Notice";
import { formatMoney } from "@/lib/format";
import { changeQty, estimateTotal, removeLine } from "@/lib/cart";
import {
  ERROR_MESSAGES,
  GENERIC_DATA_ERROR,
  GENERIC_ORDER_ERROR,
  GENERIC_VERIFY_ERROR,
  messageForCode
} from "@/lib/errors";
import { createOrder, listMenu, subscribeProducts } from "@/lib/supabase";
import {
  clearCode,
  clearNotice,
  getCode,
  getCart,
  getNotice,
  setCart as saveCart,
  setLastOrder,
  setNotice as saveNotice
} from "@/lib/storage";
import type { CartLine, Product } from "@/lib/types";

export default function CartPage() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [cart, setCartState] = useState<CartLine[]>([]);
  const [catalog, setCatalog] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  async function loadCart(currentCart: CartLine[], currentCode: string) {
    if (currentCart.length === 0) {
      setLoading(false);
      return;
    }

    const result = await listMenu(currentCode);
    if (result.ok) {
      setCatalog(result.products);
      setLoading(false);
      return;
    }

    if (result.code === "ZZ001" || result.code === "ZZ002") {
      clearCode();
      saveNotice(messageForCode(result.code) ?? GENERIC_VERIFY_ERROR);
      router.replace("/");
      return;
    }

    setNotice(GENERIC_DATA_ERROR);
    setLoading(false);
  }

  useEffect(() => {
    const storedCode = getCode();
    if (!storedCode) {
      router.replace("/");
      return;
    }
    setCode(storedCode);
    const initialCart = getCart();
    setCartState(initialCart);
    const message = getNotice();
    if (message) {
      setNotice(message);
      clearNotice();
    }
    void loadCart(initialCart, storedCode);
  }, [router]);

  const cartRef = useRef<CartLine[]>([]);
  useEffect(() => {
    cartRef.current = cart;
  }, [cart]);

  useEffect(() => {
    const unsubscribe = subscribeProducts((updated) => {
      setCatalog((prev) =>
        prev.map((item) =>
          item.id === updated.id
            ? {
                ...item,
                is_available: updated.is_available,
                ...(updated.name ? { name: updated.name } : {}),
                ...(updated.price !== undefined ? { price: updated.price } : {})
              }
            : item
        )
      );

      if (!updated.is_available) {
        const affected = cartRef.current.find((line) => line.product_id === updated.id);
        if (affected) {
          const prodName = updated.name ?? "สินค้าบางรายการ";
          setNotice(`"${prodName}" เพิ่งหมดชั่วคราว กรุณาลบออกจากตะกร้าก่อนสั่งซื้อ`);
        }
      }
    });

    return () => unsubscribe();
  }, []);

  function handleQty(productId: string, delta: number) {
    setCartState(changeQty(productId, delta));
  }

  function handleRemove(productId: string) {
    setCartState(removeLine(productId));
  }

  async function handleConfirm() {
    if (submitting) return;
    if (cart.length === 0) {
      setNotice(ERROR_MESSAGES.ZZ005);
      return;
    }
    const hasSoldOut = lines.some((line) => line.product && !line.product.is_available);
    if (hasSoldOut) {
      setNotice("มีสินค้าหมดชั่วคราวอยู่ในตะกร้า กรุณาลบออกก่อนยืนยันการสั่งซื้อ");
      return;
    }
    if (!code) {
      router.replace("/");
      return;
    }

    setSubmitting(true);
    setNotice(null);
    const items = cart.map(({ product_id, qty }) => ({ product_id, qty }));
    const result = await createOrder(code, items);

    if (result.ok) {
      saveCart([]);
      setCartState([]);
      setLastOrder({ order_id: result.orderId, total: result.total });
      router.push("/success");
      return;
    }

    if (result.code === "ZZ001") {
      clearCode();
      saveNotice(ERROR_MESSAGES.ZZ001);
      setNotice(ERROR_MESSAGES.ZZ001);
      setSubmitting(false);
      router.push("/");
      return;
    }

    if (result.code === "ZZ002") {
      saveNotice(ERROR_MESSAGES.ZZ002);
      setNotice(ERROR_MESSAGES.ZZ002);
      setSubmitting(false);
      router.push("/");
      return;
    }

    if (result.code === "ZZ003") {
      try {
        const menu = await listMenu(code);
        if (menu.ok) {
          setCatalog(menu.products);
          const availableIds = new Set(
            menu.products.filter((product) => product.is_available).map((product) => product.id)
          );
          const kept = cart.filter((line) => availableIds.has(line.product_id));
          saveCart(kept);
          setCartState(kept);
        } else if (menu.code === "ZZ001" || menu.code === "ZZ002") {
          clearCode();
          saveNotice(messageForCode(menu.code) ?? GENERIC_VERIFY_ERROR);
          setSubmitting(false);
          router.push("/");
          return;
        }
      } catch {
        // Keep the cart when the refresh also fails; the user can retry later.
      }
      saveNotice(ERROR_MESSAGES.ZZ003);
      setNotice(ERROR_MESSAGES.ZZ003);
      setSubmitting(false);
      router.push("/menu");
      return;
    }

    if (result.code === "ZZ004" || result.code === "ZZ005") {
      setNotice(ERROR_MESSAGES[result.code]);
      setSubmitting(false);
      return;
    }

    setNotice(GENERIC_ORDER_ERROR);
    setSubmitting(false);
  }

  const productById = new Map(catalog.map((product) => [product.id, product]));
  const lines = cart.map((line) => ({
    ...line,
    product: productById.get(line.product_id) ?? null
  }));
  const total = estimateTotal(cart, catalog);
  const totalCount = cart.reduce((sum, item) => sum + item.qty, 0);

  return (
    <div className="screen cart-screen">
      <BrandHeader />
      <main className="cart-main-container">
        <div className="cart-header-row">
          <Link className="back-link" href="/menu">
            ← กลับไปเลือกเมนูเพิ่ม
          </Link>
          <div className="cart-title-wrap">
            <h1 className="page-title">ตะกร้าของคุณ</h1>
            <p className="lead">ตรวจรายการให้ครบถ้วน แล้วกดยืนยันการสั่งซื้อ</p>
          </div>
        </div>

        {notice ? <Notice message={notice} /> : null}
        {loading ? (
          <div className="loading-state">
            <div className="spinner" aria-hidden="true" />
            <p className="loading" role="status">กำลังโหลดตะกร้า...</p>
          </div>
        ) : null}

        {!loading && cart.length === 0 ? (
          <div className="empty-cart-card">
            <span className="empty-cart-icon" aria-hidden="true">🛒</span>
            <h2>ยังไม่มีสินค้าในตะกร้า</h2>
            <p className="help">เลือกโดนัทหรือเครื่องดื่มที่ชอบได้จากหน้าเมนู</p>
            <div className="page-actions">
              <Link className="button primary" href="/menu">
                ไปที่หน้าเมนู
              </Link>
            </div>
          </div>
        ) : null}

        {!loading && cart.length > 0 ? (
          <div className="cart-layout">
            <section className="cart-items-col" aria-label="รายการสินค้าในตะกร้า">
              <div className="cart-items-header">
                <h2>รายการที่เลือก ({totalCount} ชิ้น)</h2>
              </div>

              <div className="cart-items-list">
                {lines.map((line) => (
                  <article className={`cart-item${line.product && !line.product.is_available ? " sold-out" : ""}`} key={line.product_id}>
                    <div className="cart-item-head">
                      <div className="cart-item-title-group">
                        <h3>{line.product?.name ?? "สินค้า"}</h3>
                        <span className="cart-item-unit-price">
                          ราคาชิ้นละ {formatMoney(line.product?.price ?? 0)}
                        </span>
                      </div>
                      <span className="cart-item-subtotal">
                        {formatMoney((line.product?.price ?? 0) * line.qty)}
                      </span>
                    </div>

                    {line.product && !line.product.is_available ? (
                      <p className="item-soldout-alert">
                        ⚠️ สินค้านี้เพิ่งหมดชั่วคราว กรุณากดลบรายการออก
                      </p>
                    ) : null}
                    {!line.product ? (
                      <p className="help">ไม่พบข้อมูลสินค้า กรุณากลับไปเลือกใหม่</p>
                    ) : null}

                    <div className="cart-item-controls">
                      <div className="qty-stepper-group">
                        <button
                          className="button secondary qty-button"
                          type="button"
                          aria-label={`ลด 5 ชิ้น ${line.product?.name ?? "สินค้า"}`}
                          onClick={() => handleQty(line.product_id, -5)}
                        >
                          <span aria-hidden="true">−</span> 5
                        </button>
                        <span className="qty-value" aria-label={`จำนวน ${line.qty} ชิ้น`}>
                          {line.qty} ชิ้น
                        </span>
                        <button
                          className="button secondary qty-button"
                          type="button"
                          aria-label={`เพิ่ม 5 ชิ้น ${line.product?.name ?? "สินค้า"}`}
                          onClick={() => handleQty(line.product_id, 5)}
                        >
                          <span aria-hidden="true">+</span> 5
                        </button>
                        <button
                          className="button secondary qty-button"
                          type="button"
                          aria-label={`เพิ่ม 10 ชิ้น ${line.product?.name ?? "สินค้า"}`}
                          onClick={() => handleQty(line.product_id, 10)}
                        >
                          <span aria-hidden="true">+</span> 10
                        </button>
                      </div>

                      <button
                        className="text-button remove-btn"
                        type="button"
                        onClick={() => handleRemove(line.product_id)}
                      >
                        ลบรายการ
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <aside className="cart-summary-col">
              <div className="cart-summary-card">
                <h2 className="summary-title">สรุปคำสั่งซื้อ</h2>

                <div className="summary-line">
                  <span>จำนวนรวมทั้งหมด</span>
                  <span className="summary-val">{totalCount} ชิ้น</span>
                </div>

                <div className="total-panel" aria-label="ยอดรวมทั้งหมด">
                  <span className="total-label">ยอดชำระโดยประมาณ</span>
                  <span className="total">{formatMoney(total)}</span>
                  <p className="total-note">ยืนยันยอดสุทธิเมื่อมารับสินค้าที่หน้าร้าน</p>
                </div>

                <section className="pay-note" aria-labelledby="pay-title">
                  <div className="pay-note-head">
                    <span className="pay-icon" aria-hidden="true">🏪</span>
                    <h3 id="pay-title">ชำระเงินที่หน้าร้าน</h3>
                  </div>
                  <p>สั่งล่วงหน้าผ่านเว็บ แล้วมาชำระเงินสดหรือสแกนจ่ายที่ร้านตอนรับของได้เลย</p>
                </section>

                <div className="confirm-stack">
                  {lines.some((line) => line.product && !line.product.is_available) ? (
                    <button
                      className="button disabled"
                      type="button"
                      disabled
                    >
                      มีสินค้าหมดในตะกร้า
                    </button>
                  ) : (
                    <button
                      className="button primary confirm-btn"
                      type="button"
                      onClick={() => void handleConfirm()}
                      disabled={submitting}
                      aria-busy={submitting}
                    >
                      {submitting ? "กำลังส่งคำสั่งซื้อ..." : "ยืนยันการสั่งซื้อ →"}
                    </button>
                  )}
                  <Link href="/menu" className="button secondary continue-btn">
                    + เลือกสินค้าอื่นเพิ่ม
                  </Link>
                </div>
              </div>
            </aside>
          </div>
        ) : null}
      </main>
    </div>
  );
}
