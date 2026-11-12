package com.moudonut.admin.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface ProductsSource {
    suspend fun adminProducts(): List<AdminProduct>
    suspend fun toggleProduct(productId: String, isAvailable: Boolean): ToggledProduct
    suspend fun updateProductPrice(productId: String, price: Double): AdminProduct
    suspend fun createProduct(name: String, price: Double, category: String): AdminProduct
}

class BillsRepository : SummarySource, ProductsSource {
    private val client get() = SupabaseHolder.client

    fun observeChanges(scope: CoroutineScope, onChange: suspend (PostgresAction) -> Unit): RealtimeChannel? {
        if (!SupabaseHolder.configured) return null
        return try {
            val channel = client.channel("admin_realtime")
            val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public")
            scope.launch {
                channel.subscribe()
                changes.collect { action ->
                    onChange(action)
                }
            }
            channel
        } catch (_: Throwable) {
            null
        }
    }

    private val billColumns = Columns.raw(
        "id,status,total,opened_at,closed_at,customers(name),bill_items(product_name,qty,unit_price)"
    )

    suspend fun openBills(): List<BillRow> =
        client.from("bills").select(billColumns) {
            filter { eq("status", "open") }
            order("opened_at", Order.ASCENDING)
        }.decodeList<BillRow>()

    suspend fun bill(id: String): BillRow? =
        client.from("bills").select(billColumns) {
            filter { eq("id", id) }
        }.decodeList<BillRow>().firstOrNull()

    suspend fun closeBill(id: String): CloseBillResult =
        client.postgrest.rpc("close_bill", buildJsonObject {
            put("p_bill_id", id)
        }).decodeAs<CloseBillResult>()

    suspend fun closeAll(ids: List<String>): Int =
        client.postgrest.rpc("close_all_bills", buildJsonObject {
            put("p_bill_ids", buildJsonArray { ids.forEach { add(it) } })
        }).decodeAs<Int>()

    override suspend fun summary(date: String): StoreSummary =
        client.postgrest.rpc("store_summary", buildJsonObject {
            put("p_business_date", date)
        }).decodeAs<StoreSummary>()

    override suspend fun dayBills(date: String): List<AdminDayBill> =
        client.postgrest.rpc("admin_day_bills", buildJsonObject {
            put("p_business_date", date)
        }).decodeAs<List<AdminDayBill>>()

    override suspend fun dayOrderItems(date: String): List<AggregatedOrderItem> =
        client.postgrest.rpc("admin_day_order_items", buildJsonObject {
            put("p_business_date", date)
        }).decodeAs<List<AggregatedOrderItem>>()

    override suspend fun deleteCustomerDayOrders(customerId: String, date: String): DeleteOrderResult =
        client.postgrest.rpc("admin_delete_customer_day_orders", buildJsonObject {
            put("p_customer_id", customerId)
            put("p_business_date", date)
        }).decodeAs<DeleteOrderResult>()

    override suspend fun clearDayOrders(date: String): DeleteOrderResult =
        client.postgrest.rpc("admin_clear_day_orders", buildJsonObject {
            put("p_business_date", date)
        }).decodeAs<DeleteOrderResult>()

    override suspend fun deleteBill(billId: String): DeleteOrderResult =
        client.postgrest.rpc("admin_delete_bill", buildJsonObject {
            put("p_bill_id", billId)
        }).decodeAs<DeleteOrderResult>()

    suspend fun updateCustomer(customerId: String, name: String): UpdatedCustomer =
        client.postgrest.rpc("admin_update_customer", buildJsonObject {
            put("p_customer_id", customerId)
            put("p_name", name)
        }).decodeAs<UpdatedCustomer>()

    suspend fun deleteCustomer(customerId: String): DeleteCustomerResult =
        client.postgrest.rpc("admin_delete_customer", buildJsonObject {
            put("p_customer_id", customerId)
        }).decodeAs<DeleteCustomerResult>()

    suspend fun deleteCode(code: String): DeleteCodeResult =
        client.postgrest.rpc("admin_delete_code", buildJsonObject {
            put("p_code", code)
        }).decodeAs<DeleteCodeResult>()

    suspend fun updateCode(code: String, label: String): UpdatedCode =
        client.postgrest.rpc("admin_update_code", buildJsonObject {
            put("p_code", code)
            put("p_label", label)
        }).decodeAs<UpdatedCode>()

    suspend fun customers(): List<AdminCustomer> =
        client.postgrest.rpc("admin_list_customers").decodeAs<List<AdminCustomer>>()

    suspend fun createCustomer(name: String, code: String, label: String): CreatedCustomer =
        client.postgrest.rpc("admin_create_customer", buildJsonObject {
            put("p_name", name)
            put("p_code", code)
            put("p_label", label)
        }).decodeAs<CreatedCustomer>()

    suspend fun addCode(customerId: String, code: String, label: String): AddedCode =
        client.postgrest.rpc("admin_add_code", buildJsonObject {
            put("p_customer_id", customerId)
            put("p_code", code)
            put("p_label", label)
        }).decodeAs<AddedCode>()

    suspend fun toggleCode(code: String, active: Boolean): ToggledCode =
        client.postgrest.rpc("admin_toggle_code", buildJsonObject {
            put("p_code", code)
            put("p_active", active)
        }).decodeAs<ToggledCode>()

    suspend fun billOrders(billId: String): List<OrderRow> =
        client.from("orders")
            .select(Columns.raw("id,created_at,total,note,order_items(product_name,qty,unit_price)")) {
                filter { eq("bill_id", billId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<OrderRow>()

    override suspend fun toggleProduct(productId: String, isAvailable: Boolean): ToggledProduct =
        client.postgrest.rpc("admin_toggle_product", buildJsonObject {
            put("p_product_id", productId)
            put("p_is_available", isAvailable)
        }).decodeAs<ToggledProduct>()

    override suspend fun updateProductPrice(productId: String, price: Double): AdminProduct =
        client.postgrest.rpc("admin_update_product_price", buildJsonObject {
            put("p_product_id", productId)
            put("p_price", price)
        }).decodeAs<AdminProduct>()

    override suspend fun createProduct(name: String, price: Double, category: String): AdminProduct =
        client.postgrest.rpc("admin_create_product", buildJsonObject {
            put("p_name", name)
            put("p_price", price)
            put("p_category", category)
        }).decodeAs<AdminProduct>()

    override suspend fun adminProducts(): List<AdminProduct> =
        client.from("products")
            .select(Columns.raw("id,name,price,category,is_available,sort_order")) {
                order("sort_order", Order.ASCENDING)
                order("name", Order.ASCENDING)
            }.decodeList<AdminProduct>()

    suspend fun isAdmin(): Boolean =
        client.from("admins")
            .select(Columns.raw("user_id"))
            .decodeList<AdminRow>()
            .isNotEmpty()
}
