"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import BrandHeader from "@/components/BrandHeader";
import Notice from "@/components/Notice";
import { formatMoney } from "@/lib/format";
import { GENERIC_DATA_ERROR, GENERIC_VERIFY_ERROR, messageForCode } from "@/lib/errors";
import { listMyBills } from "@/lib/supabase";
import { clearCode, getCode, setNotice as saveNotice } from "@/lib/storage";
import type { CustomerBill } from "@/lib/types";

function formatDateTime(isoString: string): string {
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleString("th-TH", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Asia/Bangkok"
    });
  } catch {
    return isoString;
  }
}

const PAPER_MENU_ORDER = [
  "ไส้กรอก",
  "หมูหยอง",
  "ลูกเกด",
  "สับปะรด",
  "ถั่วแดง",
  "เผือก",
  "สังขยา",
  "ปูอัด",
  "ไก่",
  "แฮม",
  "มะพร้าว",
  "ช็อคโกแลต",
  "มันม่วง",
  "ข้าวโพด",
  "พิซซ่า",
  "โดนัท",
  "ตุ๊กตา"
];
const paperMenuIndex = new Map(PAPER_MENU_ORDER.map((name, idx) => [name, idx]));
function getPaperMenuIndex(name: string): number {
  return paperMenuIndex.get(name) ?? 999;
}

export default function MyBillsPage() {
  const router = useRouter();
  const [bills, setBills] = useState<CustomerBill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<"all" | "open" | "closed">("all");

  async function loadBills() {
    setLoading(true);
    setError(null);
    const code = getCode();
    if (!code) {
      router.replace("/");
      return;
    }

    const result = await listMyBills(code);
    if (result.ok) {
      setBills(result.bills);
      setLoading(false);
      return;
    }

    if (result.code === "ZZ001" || result.code === "ZZ002") {
      clearCode();
      saveNotice(messageForCode(result.code) ?? GENERIC_VERIFY_ERROR);
      router.replace("/");
      return;
    }

    setError(GENERIC_DATA_ERROR);
    setLoading(false);
  }

  useEffect(() => {
    if (!getCode()) {
      router.replace("/");
      return;
    }
    void loadBills();
  }, [router]);

  const openBills = bills.filter((b) => b.status === "open");
  const closedBills = bills.filter((b) => b.status === "closed");

  const displayedBills =
    filter === "open" ? openBills : filter === "closed" ? closedBills : bills;

  return (
    <div className="screen bills-screen">
      <BrandHeader />
      <main className="bills-main">
        <div className="cart-header-row">
          <Link className="back-link" href="/menu">
            ← กลับไปหน้าเมนู
          </Link>
          <div className="cart-title-wrap">
            <h1 className="page-title">บิลของฉัน</h1>
            <p className="lead">ประวัติการสั่งซื้อและสถานะการชำระเงินที่ร้าน</p>
          </div>
        </div>

        {error ? (
          <div className="stack" style={{ margin: "var(--space-4) 0" }}>
            <Notice message={error} />
            <button className="button secondary" type="button" onClick={() => void loadBills()}>
              ลองโหลดใหม่
            </button>
          </div>
        ) : null}

        {loading ? (
          <div className="loading-state">
            <div className="spinner" aria-hidden="true" />
            <p className="loading" role="status">กำลังโหลดข้อมูลบิล...</p>
          </div>
        ) : null}

        {!loading && bills.length > 0 ? (
          <div className="category-filters" role="tablist" aria-label="กรองสถานะบิล" style={{ marginBottom: "var(--space-6)" }}>
            <button
              type="button"
              role="tab"
              aria-selected={filter === "all"}
              className={`filter-chip${filter === "all" ? " active" : ""}`}
              onClick={() => setFilter("all")}
            >
              ทั้งหมด ({bills.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={filter === "open"}
              className={`filter-chip${filter === "open" ? " active" : ""}`}
              onClick={() => setFilter("open")}
            >
              ⏳ รอชำระที่ร้าน ({openBills.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={filter === "closed"}
              className={`filter-chip${filter === "closed" ? " active" : ""}`}
              onClick={() => setFilter("closed")}
            >
              ✓ ชำระแล้ว ({closedBills.length})
            </button>
          </div>
        ) : null}

        {!loading && bills.length === 0 ? (
          <div className="empty-cart-card">
            <span className="empty-cart-icon" aria-hidden="true">🧾</span>
            <h2>ยังไม่มีประวัติการสั่งซื้อ</h2>
            <p className="help">เมื่อสั่งซื้อขนมแล้ว ข้อมูลบิลและยอดเงินจะแสดงที่นี่</p>
            <div className="page-actions">
              <Link className="button primary" href="/menu">
                ไปเลือกสินค้าที่หน้าเมนู →
              </Link>
            </div>
          </div>
        ) : null}

        {!loading && displayedBills.length > 0 ? (
          <div className="bills-grid">
            {displayedBills.map((bill) => {
              const isOpen = bill.status === "open";
              const totalPieces = bill.items.reduce((sum, item) => sum + item.qty, 0);

              return (
                <article className={`bill-card${isOpen ? " open" : " closed"}`} key={bill.bill_id}>
                  <div className="bill-card-head">
                    <div className="bill-meta">
                      <span className="bill-date">{formatDateTime(bill.opened_at)}</span>
                      <span className="bill-pieces-count">{totalPieces} ชิ้น</span>
                    </div>
                    <span className={`bill-status-pill ${isOpen ? "open" : "closed"}`}>
                      {isOpen ? "⏳ รอชำระที่ร้าน" : "✓ ชำระแล้ว"}
                    </span>
                  </div>

                  <div className="bill-items-list">
                    {bill.items
                      .slice()
                      .sort((a, b) => getPaperMenuIndex(a.product_name) - getPaperMenuIndex(b.product_name))
                      .map((item, idx) => (
                      <div className="bill-item-row" key={idx}>
                        <span className="bill-item-name">{item.product_name}</span>
                        <span className="bill-item-qty">{item.qty} ชิ้น</span>
                        <span className="bill-item-price">{formatMoney(item.line_total)}</span>
                      </div>
                    ))}
                  </div>

                  <div className="bill-card-foot">
                    <div className="bill-total-row">
                      <span className="bill-total-label">ยอดสุทธิ</span>
                      <span className="bill-total-val">{formatMoney(bill.total)}</span>
                    </div>

                    {isOpen ? (
                      <p className="bill-payment-reminder">
                        * กรุณาชำระเงินกับพนักงานตอนรับสินค้าหน้าร้าน
                      </p>
                    ) : (
                      <p className="bill-payment-closed">
                        ✓ ชำระเงินเรียบร้อยแล้วเมื่อ {bill.closed_at ? formatDateTime(bill.closed_at) : "ที่ร้าน"}
                      </p>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        ) : null}

        {!loading && bills.length > 0 && displayedBills.length === 0 ? (
          <div className="empty-state" style={{ marginTop: "var(--space-6)" }}>
            <p>ไม่มีบิลในหมวดหมู่นี้</p>
          </div>
        ) : null}
      </main>
    </div>
  );
}
