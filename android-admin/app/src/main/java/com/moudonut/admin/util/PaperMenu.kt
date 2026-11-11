package com.moudonut.admin.util

import com.moudonut.admin.data.AggregatedCustomerQty
import com.moudonut.admin.data.AggregatedOrderItem
import com.moudonut.admin.data.BillRow

object PaperMenu {
    val FLAVOR_ORDER: List<String> = listOf(
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
    )

    private val FLAVOR_INDEX_MAP: Map<String, Int> =
        FLAVOR_ORDER.mapIndexed { index, name -> name to index }.toMap()

    fun sortIndex(productName: String): Int =
        FLAVOR_INDEX_MAP[productName] ?: 999

    fun aggregateBills(bills: List<BillRow>): List<AggregatedOrderItem> {
        if (bills.isEmpty()) return emptyList()

        // (product_name, unit_price) -> (customer_name -> qty)
        val map = mutableMapOf<Pair<String, Double>, MutableMap<String, Int>>()

        for (bill in bills) {
            val customer = bill.customerName
            for (item in bill.items) {
                val key = Pair(item.productName, item.unitPrice)
                val custMap = map.getOrPut(key) { mutableMapOf() }
                custMap[customer] = (custMap[customer] ?: 0) + item.qty
            }
        }

        return map.map { (key, custMap) ->
            val (productName, unitPrice) = key
            val breakdown = custMap.map { (name, qty) ->
                AggregatedCustomerQty(customerName = name, qty = qty)
            }.sortedWith(compareByDescending<AggregatedCustomerQty> { it.qty }.thenBy { it.customerName })
            val totalQty = breakdown.sumOf { it.qty }
            AggregatedOrderItem(
                productName = productName,
                unitPrice = unitPrice,
                totalQty = totalQty,
                totalAmount = totalQty * unitPrice,
                customerBreakdown = breakdown
            )
        }.filter { it.totalQty > 0 }
         .sortedWith(compareBy({ sortIndex(it.productName) }, { it.productName }))
    }

    fun formatSummaryText(bills: List<BillRow>, aggregated: List<AggregatedOrderItem>): String {
        val totalPieces = aggregated.sumOf { it.totalQty }
        val totalRevenue = aggregated.sumOf { it.totalAmount }
        val customers = bills.map { it.customerName }.distinct().joinToString(", ")
        val sb = StringBuilder()
        sb.appendLine("📋 รวมยอดทำขนม (${bills.size} บิล)")
        sb.appendLine("ลูกค้า: $customers")
        sb.appendLine("ยอดรวม: $totalPieces ชิ้น · ${formatMoney(totalRevenue)}")
        sb.appendLine("---------------------------------")
        aggregated.forEachIndexed { idx, item ->
            val breakdownStr = item.customerBreakdown.joinToString(", ") { "${it.customerName}: ${it.qty}" }
            sb.appendLine("${idx + 1}. ${item.productName} ${item.totalQty} ชิ้น ($breakdownStr)")
        }
        return sb.toString().trimEnd()
    }
}
