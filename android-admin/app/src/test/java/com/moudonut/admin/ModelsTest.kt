package com.moudonut.admin

import com.moudonut.admin.data.AdminDayBill
import com.moudonut.admin.data.AdminProduct
import com.moudonut.admin.data.OrderRow
import com.moudonut.admin.data.StoreSummary
import com.moudonut.admin.data.ToggledProduct
import com.moudonut.admin.data.toThaiMessage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun storeSummaryDecodes() {
        val json = """
            {
              "business_date": "2026-11-09",
              "total": 15.00,
              "closed_bills": [
                {
                  "bill_id": "b1",
                  "customer_name": "ลูกค้า A",
                  "total": 7.50,
                  "closed_at": "2026-11-09T14:03:24+00:00"
                }
              ],
              "customers_without_orders": [
                {"customer_id": "c1", "name": "ลูกค้า E"}
              ]
            }
        """.trimIndent()

        val summary = Json.decodeFromString<StoreSummary>(json)

        assertEquals(15.0, summary.total, 0.001)
        assertEquals(1, summary.closedBills.size)
        assertEquals("ลูกค้า E", summary.customersWithoutOrders.single().name)
    }

    @Test
    fun adminDayBillDecodes() {
        val json = """
            [
              {
                "bill_id": "b1",
                "customer_id": "c1",
                "customer_name": "ลูกค้า A",
                "status": "open",
                "bill_total": 17.50,
                "day_total": 10.50,
                "order_count": 1,
                "opened_at": "2026-11-09T09:00:00+07:00",
                "closed_at": null
              }
            ]
        """.trimIndent()

        val bills = Json.decodeFromString<List<AdminDayBill>>(json)
        val bill = bills.single()

        assertEquals(10.5, bill.dayTotal, 0.001)
        assertEquals(17.5, bill.billTotal, 0.001)
        assertEquals(1, bill.orderCount)
        assertTrue(bill.isOpen)
        assertTrue(bill.spansDays)
    }

    @Test
    fun billOrdersDecode() {
        val json = """
            [
              {
                "id": "o1",
                "created_at": "2026-11-10T10:19:00+07:00",
                "total": 10.50,
                "note": "ไม่ใส่ถั่ว รับตอน 15:00",
                "order_items": [
                  {"product_name": "ไส้กรอก", "qty": 3, "unit_price": 3.50}
                ]
              },
              {
                "id": "o2",
                "created_at": "2026-11-10T11:00:00+07:00",
                "total": 3.50,
                "note": null,
                "order_items": [
                  {"product_name": "หมูหยอง", "qty": 1, "unit_price": 3.50}
                ]
              }
            ]
        """.trimIndent()

        val orders = Json.decodeFromString<List<OrderRow>>(json)
        val order = orders.first()

        assertEquals(10.5, order.total, 0.001)
        assertEquals("ไม่ใส่ถั่ว รับตอน 15:00", order.note)
        assertEquals("ไส้กรอก", order.items.single().productName)
        assertEquals(3, order.items.single().qty)
        assertEquals(null, orders.last().note)
    }

    @Test
    fun billOrdersDecodeWithoutNoteField() {
        val json = """
            [
              {
                "id": "o1",
                "created_at": "2026-11-10T10:19:00+07:00",
                "total": 10.50,
                "order_items": []
              }
            ]
        """.trimIndent()

        val orders = Json.decodeFromString<List<OrderRow>>(json)
        assertEquals(null, orders.single().note)
    }

    @Test
    fun spansDaysComparesSatang() {
        // Raw doubles differ, but both round to 30 satang.
        assertTrue((0.1 + 0.2) != 0.3)

        val sameSatang = AdminDayBill(
            billId = "b1",
            status = "open",
            billTotal = 0.1 + 0.2,
            dayTotal = 0.3
        )
        assertTrue(!sameSatang.spansDays)

        val crossDay = AdminDayBill(
            billId = "b2",
            status = "open",
            billTotal = 17.5,
            dayTotal = 10.5
        )
        assertTrue(crossDay.spansDays)
    }

    @Test
    fun genericErrorIsThai() {
        assertTrue(Exception("boom").toThaiMessage().contains("ไม่สำเร็จ"))
    }

    @Test
    fun adminProductDecodes() {
        val json = """
            [
              {
                "id": "p1",
                "name": "ไส้กรอก",
                "price": 3.50,
                "category": "donut",
                "is_available": true,
                "sort_order": 1
              }
            ]
        """.trimIndent()
        val products = Json.decodeFromString<List<AdminProduct>>(json)
        val p = products.single()
        assertEquals("p1", p.id)
        assertEquals("ไส้กรอก", p.name)
        assertEquals(3.50, p.price, 0.001)
        assertTrue(p.isAvailable)
    }

    @Test
    fun toggledProductDecodes() {
        val json = """
            {
              "product_id": "f1a672a0-4f78-4aba-8393-32bafb748033",
              "name": "ไส้กรอก",
              "is_available": false
            }
        """.trimIndent()
        val res = Json.decodeFromString<ToggledProduct>(json)
        assertEquals("f1a672a0-4f78-4aba-8393-32bafb748033", res.productId)
        assertEquals("ไส้กรอก", res.name)
        assertTrue(!res.isAvailable)
    }

    @Test
    fun productRpcPayloadDecodesWithoutSortOrder() {
        val json = """
            {
              "id": "prod-123",
              "name": "ชาไทยเย็น",
              "price": 25.0,
              "category": "drink",
              "is_available": true
            }
        """.trimIndent()
        val p = Json.decodeFromString<AdminProduct>(json)
        assertEquals("prod-123", p.id)
        assertEquals("ชาไทยเย็น", p.name)
        assertEquals(25.0, p.price, 0.001)
        assertEquals("drink", p.category)
        assertTrue(p.isAvailable)
        assertEquals(0, p.sortOrder)
    }
}
