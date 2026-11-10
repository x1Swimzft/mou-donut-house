"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import BrandHeader from "@/components/BrandHeader";
import Notice from "@/components/Notice";
import { GENERIC_VERIFY_ERROR, messageForCode } from "@/lib/errors";
import { verifyLogin } from "@/lib/supabase";
import {
  clearCode,
  clearNotice,
  getCode,
  getCustomerName,
  getNotice,
  setCode as saveCode,
  setCustomerName
} from "@/lib/storage";

export default function LoginPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const message = getNotice();
    if (message) {
      setNotice(message);
      clearNotice();
    }

    const storedCode = getCode();
    if (!storedCode) return;
    setCode(storedCode);

    const storedName = getCustomerName();
    if (!storedName) return;
    setName(storedName);
    setSaved(true);

    let cancelled = false;
    void (async () => {
      const result = await verifyLogin(storedName, storedCode);
      if (cancelled || result.ok) return;

      if (result.code === "ZZ001" || result.code === "ZZ002") {
        clearCode();
        setCustomerName("");
        setCode("");
        setName("");
        setSaved(false);
        setNotice(messageForCode(result.code) ?? GENERIC_VERIFY_ERROR);
        return;
      }
      setNotice(GENERIC_VERIFY_ERROR);
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (checking) return;

    const valueName = name.trim();
    const valueCode = code.trim().toUpperCase();

    if (!valueName) {
      setNotice("กรุณากรอกชื่อลูกค้า");
      return;
    }
    if (!valueCode) {
      setNotice("กรุณากรอกรหัสผ่าน");
      return;
    }

    setChecking(true);
    setNotice(null);
    const result = await verifyLogin(valueName, valueCode);
    if (result.ok) {
      saveCode(valueCode);
      setCustomerName(result.customerName);
      router.push("/menu");
      return;
    }

    if (result.code === "ZZ001" || result.code === "ZZ002") {
      clearCode();
      setCustomerName("");
      setSaved(false);
      setNotice(messageForCode(result.code) ?? GENERIC_VERIFY_ERROR);
    } else {
      setNotice(GENERIC_VERIFY_ERROR);
    }
    setChecking(false);
  }

  return (
    <div className="screen login-screen">
      <BrandHeader />
      <main className="login-screen-wrap">
        <div className="login-card">
          <div className="login-card-head">
            <span className="login-badge-icon" aria-hidden="true">🍩</span>
            <h1 className="login-title">เข้าสู่ร้าน</h1>
            <p className="lead">กรอกชื่อลูกค้าและรหัสผ่านเพื่อสั่งซื้อล่วงหน้า</p>
          </div>

          <form className="stack" onSubmit={handleSubmit} noValidate>
            <div className="field">
              <label className="field-label" htmlFor="name">
                ชื่อลูกค้า
              </label>
              <input
                className="code-input"
                id="name"
                name="name"
                type="text"
                placeholder="เช่น ต้า หรือ ลูกค้าประจำ"
                value={name}
                onChange={(event) => setName(event.target.value)}
                maxLength={80}
                autoComplete="off"
                spellCheck={false}
              />
            </div>

            <div className="field">
              <label className="field-label" htmlFor="code">
                รหัสผ่านของร้าน
              </label>
              <input
                className="code-input"
                id="code"
                name="code"
                type="text"
                placeholder="เช่น MDH4"
                value={code}
                onChange={(event) => setCode(event.target.value.toUpperCase())}
                maxLength={32}
                autoComplete="off"
                spellCheck={false}
                inputMode="text"
                aria-describedby="login-help"
              />
              <p className="help" id="login-help">
                พิมพ์รหัสให้ครบ แล้วกด &quot;เข้าสู่ร้าน&quot;
              </p>
            </div>

            {notice ? <Notice message={notice} /> : null}

            <button className="button primary login-submit-btn" type="submit" disabled={checking}>
              {checking ? "กำลังตรวจสอบข้อมูล..." : "เข้าสู่ร้าน →"}
            </button>
          </form>

          {saved && !notice ? (
            <p className="help saved-info-note">
              ✓ บันทึกข้อมูลของท่านไว้แล้วในเครื่องนี้
            </p>
          ) : null}

          <div className="login-card-footer">
            <p className="foot-note">หากยังไม่มีรหัส สามารถสอบถามพนักงานที่หน้าร้านได้เลยครับ</p>
          </div>
        </div>
      </main>
    </div>
  );
}
