"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import BrandHeader from "@/components/BrandHeader";
import { formatMoney, shortOrderId } from "@/lib/format";
import { getLastOrder } from "@/lib/storage";
import type { LastOrder } from "@/lib/types";

export default function SuccessPage() {
  const [order, setOrder] = useState<LastOrder | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    setOrder(getLastOrder());
    setLoaded(true);
  }, []);

  function handleCopy() {
    if (!order?.order_id) return;
    navigator.clipboard?.writeText(order.order_id);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="screen success-screen">
      <BrandHeader />
      <main className="success-screen-wrap">
        {!loaded ? (
          <div className="loading-state">
            <div className="spinner" aria-hidden="true" />
            <p className="loading">กำลังโหลดข้อมูลคำสั่งซื้อ...</p>
          </div>
        ) : null}

        {loaded && !order ? (
          <div className="success-card empty-state">
            <span className="empty-cart-icon" aria-hidden="true">📋</span>
            <h2>ไม่พบข้อมูลออเดอร์</h2>
            <p className="help">อาจมีการสั่งซื้อเสร็จสิ้นไปแล้ว หรือยังไม่มีรายการ</p>
            <div className="page-actions">
              <Link className="button primary" href="/menu">
                กลับไปหน้าเมนู
              </Link>
            </div>
          </div>
        ) : null}

        {loaded && order ? (
          <div className="success-card">
            <div className="success-banner">
              <span className="success-check-icon" aria-hidden="true">✅</span>
              <h1 id="success-title">สั่งซื้อสำเร็จแล้ว!</h1>
              <p>กรุณามารับสินค้าและชำระเงินที่ร้าน</p>
            </div>

            <section className="order-ticket" aria-labelledby="order-title">
              <div className="ticket-header">
                <span className="order-label" id="order-title">
                  เลขอ้างอิงออเดอร์
                </span>
                <span className="order-status-badge">รอรับที่ร้าน</span>
              </div>

              <p className="order-no">{shortOrderId(order.order_id)}</p>
              <p className="order-help">แจ้งพนักงานด้วยเลขอ้างอิงนี้เมื่อมารับของ</p>

              <div className="order-id-box">
                <span className="order-id-label">รหัสออเดอร์เต็ม:</span>
                <code className="order-id-code">{order.order_id}</code>
                <button
                  type="button"
                  className="copy-id-btn"
                  onClick={handleCopy}
                  title="คัดลอกรหัสออเดอร์เต็ม"
                >
                  {copied ? "คัดลอกแล้ว ✓" : "คัดลอก"}
                </button>
              </div>

              <div className="total-line">
                <span>ยอดเงินที่ต้องชำระ</span>
                <span className="order-total-price">{formatMoney(order.total)}</span>
              </div>
            </section>

            <div className="pickup-instructions">
              <h3>คำแนะนำการรับสินค้า</h3>
              <ul>
                <li>เปิดหน้านี้หรือแคปหน้าจอเก็บเลขออเดอร์ไว้</li>
                <li>เดินทางมารับของที่หน้าร้าน &quot;Mou Donut House&quot;</li>
                <li>ชำระเงินสดหรือแสกน QR Code จ่ายกับทางร้านได้ทันที</li>
              </ul>
            </div>

            <div className="page-actions">
              <Link className="button primary" href="/menu">
                สั่งรายการอื่นเพิ่ม →
              </Link>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}
