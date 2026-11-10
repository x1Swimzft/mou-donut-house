package com.moudonut.admin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AdminDayBill
import com.moudonut.admin.data.StoreSummary
import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.shiftDate
import com.moudonut.admin.util.todayBangkok
import com.moudonut.admin.data.moneyEquals

@Composable
fun SummaryScreen(onOpenBill: (String) -> Unit) {
    val vm: SummaryViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.onScreenResumed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("สรุปรายวัน", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { vm.setDate(shiftDate(state.date, -1)) }) {
                Text("ก่อนหน้า")
            }
            Text(state.date, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { vm.setDate(shiftDate(state.date, 1)) }) {
                Text("ถัดไป")
            }
        }
        TextButton(onClick = { vm.setDate(todayBangkok()) }) { Text("วันนี้") }

        Spacer(Modifier.height(8.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { vm.load(state.date) }) { Text("ลองใหม่") }
        } else {
            val summary = state.summary
            if (summary == null) {
                Text("ไม่มีข้อมูล", style = MaterialTheme.typography.bodyLarge)
            } else {
                if (state.refreshError != null) {
                    Text(
                        "อัปเดตไม่สำเร็จ: ${state.refreshError}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { vm.refresh() }) { Text("ลองใหม่") }
                }
                SummaryContent(summary, state.dayBills, onOpenBill)
            }
        }
    }
}

@Composable
private fun ColumnScope.SummaryContent(
    summary: StoreSummary,
    dayBills: List<AdminDayBill>,
    onOpenBill: (String) -> Unit
) {
    val dayTotal = dayBills.sumOf { it.dayTotal }
    val dayTotalMatchesClosed = moneyEquals(dayTotal, summary.total)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ปิดแล้ว", style = MaterialTheme.typography.bodyMedium)
            Text(
                formatMoney(summary.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${summary.closedBills.size} บิลที่ปิดแล้ว",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!dayTotalMatchesClosed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "รวมทั้งวัน (รวมบิลเปิด) ${formatMoney(dayTotal)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "บิลของวันนี้ (${dayBills.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (dayBills.isEmpty()) {
            item { Text("ยังไม่มีบิลในวันนี้", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(dayBills, key = { it.billId }) { bill ->
                DayBillRow(bill = bill, onClick = { onOpenBill(bill.billId) })
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text("ลูกค้าที่ยังไม่สั่งวันนี้", style = MaterialTheme.typography.titleMedium)
        }
        if (summary.customersWithoutOrders.isEmpty()) {
            item { Text("ไม่มี", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(summary.customersWithoutOrders) { customer ->
                Text(customer.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DayBillRow(bill: AdminDayBill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    bill.customerName ?: "-",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (bill.isOpen) {
                        "เปิด · ${bill.orderCount} ออเดอร์วันนี้"
                    } else {
                        "ปิดแล้ว · ${bill.orderCount} ออเดอร์วันนี้"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (bill.spansDays) {
                    Text(
                        "ทั้งบิล ${formatMoney(bill.billTotal)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                formatMoney(bill.dayTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
