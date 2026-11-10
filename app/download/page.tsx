import Link from "next/link";
import BrandHeader from "@/components/BrandHeader";

export const metadata = {
  title: "ดาวน์โหลดแอปผู้ดูแลร้าน (Admin APK) — Mou Donut House",
  description: "ดาวน์โหลด Mou Donut House Admin APK สำหรับโทรศัพท์ Android",
};

export default function DownloadPage() {
  return (
    <div className="page-shell">
      <BrandHeader />

      <main className="main-content">
        <div className="login-card" style={{ maxWidth: "560px", margin: "0 auto" }}>
          <div className="login-card-header">
            <div style={{ fontSize: "2.5rem", marginBottom: "8px" }}>📱</div>
            <h1 className="login-title">แอปผู้ดูแลร้าน (Android)</h1>
            <p className="login-subtitle">
              Mou Donut House Admin v1.0.0 สำหรับโทรศัพท์และแท็บเล็ต
            </p>
          </div>

          <div
            style={{
              background: "var(--color-surface-subtle, #f8f6f0)",
              border: "1px solid var(--color-border, #e5e0d8)",
              borderRadius: "12px",
              padding: "16px",
              marginBottom: "20px",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
              <span style={{ fontWeight: 600 }}>เวอร์ชัน:</span>
              <span className="badge" style={{ background: "#781d42", color: "#fff", padding: "2px 8px", borderRadius: "6px", fontSize: "0.85rem" }}>
                v1.0.0
              </span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
              <span style={{ fontWeight: 600 }}>ขนาดไฟล์:</span>
              <span style={{ color: "#666" }}>~23.1 MB</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ fontWeight: 600 }}>ระบบปฏิบัติการ:</span>
              <span style={{ color: "#666" }}>Android 8.0 ขึ้นไป</span>
            </div>
          </div>

          <div style={{ marginBottom: "24px" }}>
            <a
              href="/download/mou-donut-admin.apk"
              download="mou-donut-admin-v1.0.0.apk"
              className="button primary"
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "8px",
                width: "100%",
                padding: "14px",
                fontSize: "1.05rem",
                textDecoration: "none",
                fontWeight: 600,
                textAlign: "center",
              }}
            >
              ⬇️ ดาวน์โหลด Admin APK (v1.0.0)
            </a>
          </div>


          <div
            style={{
              fontSize: "0.88rem",
              color: "#555",
              lineHeight: 1.6,
              marginBottom: "20px",
            }}
          >
            <h4 style={{ margin: "0 0 8px 0", color: "#333" }}>📖 วิธีติดตั้งบนโทรศัพท์ Android:</h4>
            <ol style={{ paddingLeft: "20px", margin: 0 }}>
              <li>กดปุ่ม &quot;ดาวน์โหลด Admin APK&quot; ด้านบน</li>
              <li>เปิดไฟล์ APK เมื่อดาวน์โหลดเสร็จสิ้น</li>
              <li>
                หากมีข้อความแจ้งเตือนความปลอดภัย ให้เลือก <em>&quot;อนุญาตให้ติดตั้งจากแหล่งนี้&quot;</em> หรือ <em>&quot;Install anyway&quot;</em>
              </li>
              <li>เปิดแอปแล้วเข้าสู่ระบบด้วยบัญชีแอดมินของร้านเพื่อใช้งานทันที</li>
            </ol>
          </div>

          <div style={{ textAlign: "center", borderTop: "1px solid var(--color-border, #e5e0d8)", paddingTop: "16px" }}>
            <Link href="/" style={{ color: "#781d42", textDecoration: "none", fontSize: "0.9rem" }}>
              ← กลับไปหน้าร้าน
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
