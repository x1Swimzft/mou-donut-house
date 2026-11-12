package com.moudonut.admin

import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.formatTime
import com.moudonut.admin.util.shiftDate
import com.moudonut.admin.util.shortOrderId
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun moneyUsesSatangOnlyWhenNeeded() {
        assertEquals("7.5 บาท", formatMoney(7.5))
        assertEquals("15 บาท", formatMoney(15.0))
        assertEquals("3.5 บาท", formatMoney(3.5))
    }

    @Test
    fun timeUsesBangkokZone() {
        assertEquals("21:03", formatTime("2026-11-09T14:03:24.581161+00:00"))
    }

    @Test
    fun dateShiftMovesDays() {
        assertEquals("2026-11-08", shiftDate("2026-11-09", -1))
        assertEquals("2026-11-10", shiftDate("2026-11-09", 1))
    }

    @Test
    fun shortOrderIdMatchesCustomerScreen() {
        assertEquals("942F6A38", shortOrderId("942f6a38-c546-4836-954b-bbdae6d72f7f"))
        assertEquals("0C316476", shortOrderId("0c316476-6a85-4a59-a667-e3c177efd0ac"))
    }
}
