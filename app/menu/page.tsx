"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import BrandHeader from "@/components/BrandHeader";
import CartBar from "@/components/CartBar";
import Notice from "@/components/Notice";
import ProductCard from "@/components/ProductCard";
import { addToCart, countItems, estimateTotal } from "@/lib/cart";
import {
  GENERIC_DATA_ERROR,
  GENERIC_VERIFY_ERROR,
  messageForCode
} from "@/lib/errors";
import { listMenu, subscribeProducts } from "@/lib/supabase";
import {
  clearCode,
  clearNotice,
  getCode,
  getCart,
  getNotice,
  setNotice as saveNotice
} from "@/lib/storage";
import type { CartLine, Product } from "@/lib/types";

export default function MenuPage() {
  const router = useRouter();
  const [catalog, setCatalog] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [filter, setFilter] = useState<"all" | "donut" | "drink">("all");

  async function loadMenu() {
    setLoading(true);
    setError(null);
    const code = getCode();
    if (!code) {
      router.replace("/");
      return;
    }

    const result = await listMenu(code);
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

    setCatalog([]);
    setError(GENERIC_DATA_ERROR);
    setLoading(false);
  }

  useEffect(() => {
    if (!getCode()) {
      router.replace("/");
      return;
    }
    setCart(getCart());
    const message = getNotice();
    if (message) {
      setNotice(message);
      clearNotice();
    }
    void loadMenu();
  }, [router]);

  useEffect(() => {
    const unsubscribe = subscribeProducts((updated) => {
      setCatalog((prev) => {
        const exists = prev.some((item) => item.id === updated.id);
        if (!exists) {
          if (!updated.name || updated.price === undefined) return prev;
          return [
            ...prev,
            {
              id: updated.id,
              name: updated.name,
              price: updated.price,
              category: (updated.category as "donut" | "drink") || "donut",
              is_available: updated.is_available
            }
          ];
        }
        return prev.map((item) =>
          item.id === updated.id
            ? {
                ...item,
                is_available: updated.is_available,
                ...(updated.name ? { name: updated.name } : {}),
                ...(updated.price !== undefined ? { price: updated.price } : {})
              }
            : item
        );
      });

      if (!updated.is_available) {
        const currentCart = getCart();
        const affected = currentCart.find((line) => line.product_id === updated.id);
        if (affected) {
          const prodName = updated.name ?? "สินค้าบางรายการ";
          setNotice(`"${prodName}" เพิ่งหมดชั่วคราว`);
        }
      }
    });

    return () => unsubscribe();
  }, []);

  function handleAdd(product: Product, amount: number = 5) {
    if (!product.is_available) return;
    setCart(addToCart(product.id, amount));
  }

  const donuts = catalog.filter((product) => product.category === "donut");
  const drinks = catalog.filter((product) => product.category === "drink");
  const itemCount = countItems(cart);
  const estimatedTotal = estimateTotal(cart, catalog);
  const cartByProductId = new Map(cart.map((line) => [line.product_id, line.qty]));

  const showDonuts = filter === "all" || filter === "donut";
  const showDrinks = filter === "all" || filter === "drink";

  return (
    <div className="screen menu-screen">
      <BrandHeader />
      <main className="menu-main">
        <header className="page-header">
          <div className="page-header-text">
            <h1 className="page-title">เลือกของที่ชอบ</h1>
            <p className="lead">กดเพิ่มทีละ 5 หรือ 10 ชิ้น เพื่อสั่งซื้อล่วงหน้า</p>
          </div>

          <div className="category-filters" role="tablist" aria-label="กรองประเภทสินค้า">
            <button
              type="button"
              role="tab"
              aria-selected={filter === "all"}
              className={`filter-chip${filter === "all" ? " active" : ""}`}
              onClick={() => setFilter("all")}
            >
              ทั้งหมด ({catalog.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={filter === "donut"}
              className={`filter-chip${filter === "donut" ? " active" : ""}`}
              onClick={() => setFilter("donut")}
            >
              🍩 โดนัท ({donuts.length})
            </button>
            {drinks.length > 0 ? (
              <button
                type="button"
                role="tab"
                aria-selected={filter === "drink"}
                className={`filter-chip${filter === "drink" ? " active" : ""}`}
                onClick={() => setFilter("drink")}
              >
                🥤 เครื่องดื่ม ({drinks.length})
              </button>
            ) : null}
          </div>
        </header>

        {notice ? <Notice message={notice} /> : null}
        {error ? (
          <div className="stack" style={{ marginBottom: "var(--space-6)" }}>
            <Notice message={error} />
            <button className="button secondary" type="button" onClick={() => void loadMenu()}>
              ลองโหลดใหม่
            </button>
          </div>
        ) : null}

        {loading ? (
          <div className="loading-state">
            <div className="spinner" aria-hidden="true" />
            <p className="loading" role="status">กำลังโหลดเมนูสดใหม่...</p>
          </div>
        ) : null}

        {!loading && !error && catalog.length === 0 ? (
          <p className="empty-state">ยังไม่มีสินค้าในเมนูขณะนี้</p>
        ) : null}

        {showDonuts && donuts.length > 0 ? (
          <section className="catalog-section" aria-labelledby="donuts-title">
            <div className="section-head">
              <h2 className="section-title" id="donuts-title">
                โดนัทโฮมเมด
              </h2>
              <span className="section-count">{donuts.length} รายการ</span>
            </div>
            <div className="product-grid">
              {donuts.map((product, index) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  index={index}
                  onAdd={handleAdd}
                  cartQty={cartByProductId.get(product.id) ?? 0}
                />
              ))}
            </div>
          </section>
        ) : null}

        {showDrinks && drinks.length > 0 ? (
          <section className="catalog-section" aria-labelledby="drinks-title">
            <div className="section-head">
              <h2 className="section-title" id="drinks-title">
                เครื่องดื่ม
              </h2>
              <span className="section-count">{drinks.length} รายการ</span>
            </div>
            <div className="product-grid">
              {drinks.map((product, index) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  index={index}
                  onAdd={handleAdd}
                  cartQty={cartByProductId.get(product.id) ?? 0}
                />
              ))}
            </div>
          </section>
        ) : null}

        {!loading && !error && catalog.some((product) => !product.is_available) ? (
          <p className="help catalog-note">
            * สินค้าที่แสดงสถานะ &quot;หมดชั่วคราว&quot; ยังไม่สามารถกดสั่งได้ในขณะนี้
          </p>
        ) : null}
      </main>

      <CartBar count={itemCount} total={estimatedTotal} />
    </div>
  );
}
