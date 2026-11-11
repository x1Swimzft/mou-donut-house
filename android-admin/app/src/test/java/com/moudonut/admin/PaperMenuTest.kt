package com.moudonut.admin

import com.moudonut.admin.data.BillItemRow
import com.moudonut.admin.data.BillRow
import com.moudonut.admin.data.CustomerRow
import com.moudonut.admin.util.PaperMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperMenuTest {

    @Test
    fun testAllSeventeenItemsAreInCorrectOrder() {
        assertEquals(17, PaperMenu.FLAVOR_ORDER.size)
        assertEquals("ไส้กรอก", PaperMenu.FLAVOR_ORDER[0])
        assertEquals("หมูหยอง", PaperMenu.FLAVOR_ORDER[1])
        assertEquals("ลูกเกด", PaperMenu.FLAVOR_ORDER[2])
        assertEquals("สับปะรด", PaperMenu.FLAVOR_ORDER[3])
        assertEquals("ถั่วแดง", PaperMenu.FLAVOR_ORDER[4])
        assertEquals("เผือก", PaperMenu.FLAVOR_ORDER[5])
        assertEquals("สังขยา", PaperMenu.FLAVOR_ORDER[6])
        assertEquals("ปูอัด", PaperMenu.FLAVOR_ORDER[7])
        assertEquals("ไก่", PaperMenu.FLAVOR_ORDER[8])
        assertEquals("แฮม", PaperMenu.FLAVOR_ORDER[9])
        assertEquals("มะพร้าว", PaperMenu.FLAVOR_ORDER[10])
        assertEquals("ช็อคโกแลต", PaperMenu.FLAVOR_ORDER[11])
        assertEquals("มันม่วง", PaperMenu.FLAVOR_ORDER[12])
        assertEquals("ข้าวโพด", PaperMenu.FLAVOR_ORDER[13])
        assertEquals("พิซซ่า", PaperMenu.FLAVOR_ORDER[14])
        assertEquals("โดนัท", PaperMenu.FLAVOR_ORDER[15])
        assertEquals("ตุ๊กตา", PaperMenu.FLAVOR_ORDER[16])

        for (i in 0 until 17) {
            assertEquals(i, PaperMenu.sortIndex(PaperMenu.FLAVOR_ORDER[i]))
        }
        assertEquals(999, PaperMenu.sortIndex("ขนมปังเนย"))
    }

    @Test
    fun testAggregateBillsCombinesQuantitiesAndMaintainsPaperMenuOrder() {
        val bill1 = BillRow(
            id = "b1",
            status = "open",
            total = 52.5,
            openedAt = "2026-11-11T10:00:00Z",
            customers = CustomerRow("ต้า"),
            items = listOf(
                BillItemRow(productName = "ลูกเกด", qty = 5, unitPrice = 3.5),
                BillItemRow(productName = "ไส้กรอก", qty = 10, unitPrice = 3.5)
            )
        )
        val bill2 = BillRow(
            id = "b2",
            status = "open",
            total = 122.5,
            openedAt = "2026-11-11T10:05:00Z",
            customers = CustomerRow("ต่อ"),
            items = listOf(
                BillItemRow(productName = "ไส้กรอก", qty = 20, unitPrice = 3.5),
                BillItemRow(productName = "หมูหยอง", qty = 15, unitPrice = 3.5)
            )
        )
        val bill3 = BillRow(
            id = "b3",
            status = "open",
            total = 17.5,
            openedAt = "2026-11-11T11:00:00Z",
            customers = CustomerRow("ต้า"),
            items = listOf(
                BillItemRow(productName = "ไส้กรอก", qty = 5, unitPrice = 3.5)
            )
        )

        val aggregated = PaperMenu.aggregateBills(listOf(bill1, bill2, bill3))

        // Must be 3 distinct products: ไส้กรอก, หมูหยอง, ลูกเกด in exact paper menu sequence!
        assertEquals(3, aggregated.size)
        assertEquals("ไส้กรอก", aggregated[0].productName)
        assertEquals("หมูหยอง", aggregated[1].productName)
        assertEquals("ลูกเกด", aggregated[2].productName)

        // ไส้กรอก total = 10 (ต้า) + 20 (ต่อ) + 5 (ต้า) = 35
        assertEquals(35, aggregated[0].totalQty)
        assertEquals(122.5, aggregated[0].totalAmount, 0.001)
        // ต้า should be consolidated to 15, ต่อ to 20
        assertEquals(2, aggregated[0].customerBreakdown.size)
        assertEquals("ต่อ", aggregated[0].customerBreakdown[0].customerName)
        assertEquals(20, aggregated[0].customerBreakdown[0].qty)
        assertEquals("ต้า", aggregated[0].customerBreakdown[1].customerName)
        assertEquals(15, aggregated[0].customerBreakdown[1].qty)

        // หมูหยอง total = 15 (ต่อ)
        assertEquals(15, aggregated[1].totalQty)

        // ลูกเกด total = 5 (ต้า)
        assertEquals(5, aggregated[2].totalQty)

        // Text summary
        val text = PaperMenu.formatSummaryText(listOf(bill1, bill2, bill3), aggregated)
        assertTrue(text.contains("ไส้กรอก 35 ชิ้น"))
        assertTrue(text.contains("หมูหยอง 15 ชิ้น"))
        assertTrue(text.contains("ลูกเกด 5 ชิ้น"))
        assertTrue(text.contains("55 ชิ้น"))
    }
}
