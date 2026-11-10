package com.moudonut.admin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class CustomerRow(
    val name: String? = null
)

@Serializable
data class BillItemRow(
    @SerialName("product_name") val productName: String,
    val qty: Int,
    @SerialName("unit_price") val unitPrice: Double
)

@Serializable
data class BillRow(
    val id: String,
    val status: String,
    val total: Double,
    @SerialName("opened_at") val openedAt: String,
    @SerialName("closed_at") val closedAt: String? = null,
    val customers: CustomerRow? = null,
    @SerialName("bill_items") val items: List<BillItemRow> = emptyList()
) {
    val customerName: String get() = customers?.name ?: "ลูกค้า"
    val itemCount: Int get() = items.sumOf { it.qty }
}

@Serializable
data class ClosedBill(
    @SerialName("bill_id") val billId: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val total: Double = 0.0,
    @SerialName("closed_at") val closedAt: String? = null
)

@Serializable
data class CustomerWithoutOrder(
    @SerialName("customer_id") val customerId: String,
    val name: String
)

@Serializable
data class StoreSummary(
    @SerialName("business_date") val businessDate: String? = null,
    val total: Double = 0.0,
    @SerialName("closed_bills") val closedBills: List<ClosedBill> = emptyList(),
    @SerialName("customers_without_orders") val customersWithoutOrders: List<CustomerWithoutOrder> = emptyList()
)

@Serializable
data class CloseBillResult(
    @SerialName("bill_id") val billId: String? = null,
    val status: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    val total: Double? = null
)

@Serializable
data class AdminRow(
    @SerialName("user_id") val userId: String
)

@Serializable
data class AdminCode(
    val code: String,
    val label: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("first_used_at") val firstUsedAt: String? = null
)

@Serializable
data class AdminCustomer(
    @SerialName("customer_id") val customerId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    val codes: List<AdminCode> = emptyList()
)

@Serializable
data class CreatedCustomer(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val label: String? = null
)

@Serializable
data class AddedCode(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val label: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ToggledCode(
    val code: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class AdminDayBill(
    @SerialName("bill_id") val billId: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val status: String,
    @SerialName("bill_total") val billTotal: Double = 0.0,
    @SerialName("day_total") val dayTotal: Double = 0.0,
    @SerialName("order_count") val orderCount: Int = 0,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null
) {
    val isOpen: Boolean get() = status == "open"
    val spansDays: Boolean get() = !moneyEquals(billTotal, dayTotal)
}

fun moneyEquals(a: Double, b: Double): Boolean =
    (a * 100).roundToInt() == (b * 100).roundToInt()

@Serializable
data class OrderItemRow(
    @SerialName("product_name") val productName: String,
    val qty: Int,
    @SerialName("unit_price") val unitPrice: Double
)

@Serializable
data class OrderRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val total: Double,
    @SerialName("order_items") val items: List<OrderItemRow> = emptyList()
)

@Serializable
data class AdminProduct(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class AggregatedCustomerQty(
    @SerialName("customer_name") val customerName: String,
    val qty: Int
)

@Serializable
data class AggregatedOrderItem(
    @SerialName("product_name") val productName: String,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("total_qty") val totalQty: Int,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("customer_breakdown") val customerBreakdown: List<AggregatedCustomerQty> = emptyList()
)

@Serializable
data class ToggledProduct(
    @SerialName("product_id") val productId: String,
    val name: String? = null,
    @SerialName("is_available") val isAvailable: Boolean
)

