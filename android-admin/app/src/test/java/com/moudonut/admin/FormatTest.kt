package com.moudonut.admin

import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.formatTime
import com.moudonut.admin.util.shiftDate
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
}
