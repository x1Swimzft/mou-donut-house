export const ERROR_MESSAGES = {
  ZZ001: "ชื่อลูกค้าหรือรหัสผ่านไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง",
  ZZ002: "รหัสผ่านนี้ถูกปิดการใช้งานแล้ว กรุณาติดต่อร้าน",
  ZZ003: "สินค้านี้หมดชั่วคราว กรุณาเลือกใหม่",
  ZZ004: "จำนวนไม่ถูกต้อง",
  ZZ005: "ตะกร้าว่าง กรุณาเลือกสินค้า"
} as const;

export const GENERIC_ORDER_ERROR = "ส่งคำสั่งไม่สำเร็จ กรุณาลองอีกครั้ง";
export const GENERIC_VERIFY_ERROR = "ระบบขัดข้องชั่วคราว กรุณาลองใหม่อีกครั้ง";
export const GENERIC_DATA_ERROR = "ระบบขัดข้องชั่วคราว กรุณาลองใหม่อีกครั้ง";

export function messageForCode(code: string | null): string | null {
  if (!code) return null;
  if (code in ERROR_MESSAGES) return ERROR_MESSAGES[code as keyof typeof ERROR_MESSAGES];
  return null;
}