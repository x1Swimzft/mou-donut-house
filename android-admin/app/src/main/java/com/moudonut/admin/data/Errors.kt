package com.moudonut.admin.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException

fun Throwable.toThaiMessage(): String {
    val code = (this as? PostgrestRestException)?.code
    return when (code) {
        "ZZ006" -> "ไม่พบบิล หรือบิลนี้ปิดแล้ว"
        "ZZ007" -> "ป้ายกำกับต้องยาว 1–40 ตัว"
        "ZZ008" -> "บัญชีนี้ไม่มีสิทธิ์แอดมิน"
        "ZZ009" -> "รหัสไม่ถูกต้อง หรือรหัสนี้ถูกใช้แล้ว"
        "ZZ010" -> "ไม่พบลูกค้า หรือไม่พบรหัสนี้"
        "ZZ011" -> "ชื่อลูกค้าต้องยาว 1–80 ตัว"
        "ZZ012" -> "ไม่พบสินค้านี้"
        "ZZ013" -> "พารามิเตอร์ไม่ถูกต้อง"
        "ZZ001" -> "รหัสลูกค้าไม่ถูกต้อง"
        "ZZ002" -> "รหัสลูกค้าถูกปิดใช้งาน"
        "PGRST301" -> "เซสชันหมดอายุ กรุณาเข้าสู่ระบบใหม่"
        else -> {
            val raw = message.orEmpty()
            when {
                raw.contains("Invalid login credentials", ignoreCase = true) ->
                    "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
                raw.contains("Email not confirmed", ignoreCase = true) ->
                    "อีเมลนี้ยังไม่ได้รับการยืนยัน"
                else -> "ทำรายการไม่สำเร็จ กรุณาลองใหม่"
            }
        }
    }
}
