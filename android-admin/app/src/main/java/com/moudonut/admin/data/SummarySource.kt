package com.moudonut.admin.data

/**
 * The two reads the summary screen needs. `BillsRepository` is the production implementation;
 * tests use a fake so call counts and timing can be asserted.
 */
interface SummarySource {
    suspend fun summary(date: String): StoreSummary
    suspend fun dayBills(date: String): List<AdminDayBill>
    suspend fun dayOrderItems(date: String): List<AggregatedOrderItem>
}
