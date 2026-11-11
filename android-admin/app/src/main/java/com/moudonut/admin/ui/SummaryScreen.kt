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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AdminDayBill
import com.moudonut.admin.data.AggregatedOrderItem
import com.moudonut.admin.data.StoreSummary
import com.moudonut.admin.data.moneyEquals
import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.shiftDate
import com.moudonut.admin.util.todayBangkok

@Composable
fun SummaryScreen(
    onOpenBill: (String) -> Unit,
    onOpenCombined: (String) -> Unit = {}
) {
    val vm: SummaryViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var customerToDelete by remember { mutableStateOf<AdminDayBill?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.onScreenResumed()
    }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text("สรุปรายวัน", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { vm.setDate(shiftDate(state.date, -1)) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("← ก่อนหน้า")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        state.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = { vm.setDate(todayBangkok()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("วันนี้", style = MaterialTheme.typography.labelSmall)
                    }
                }
                TextButton(
                    onClick = { vm.setDate(shiftDate(state.date, 1)) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("ถัดไป →")
                }
            }

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
                    SummaryContent(
                        date = state.date,
                        summary = summary,
                        dayBills = state.dayBills,
                        dayOrderItems = state.dayOrderItems,
                        onOpenBill = onOpenBill,
                        onOpenCombined = onOpenCombined,
                        onDeleteCustomer = { customerToDelete = it },
                        onClearAll = { showClearAllConfirm = true }
                    )
                }
            }
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("เคลียร์ออเดอร์ทั้งหมด?") },
            text = {
                Text("ต้องการลบออเดอร์และบิลทั้งหมดของวันที่ ${state.date} ใช่หรือไม่?\nยอดสรุปทั้งหมดของวันนี้จะถูกรีเซ็ตเป็น 0")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        vm.clearDayOrders()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ยืนยันเคลียร์ทั้งหมด")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    customerToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("ลบออเดอร์ของ ${bill.customerName}?") },
            text = {
                Text("ต้องการลบออเดอร์ทั้งหมดในวันนี้ของ \"${bill.customerName}\" (${bill.orderCount} ออเดอร์ ยอด ${formatMoney(bill.dayTotal)}) ใช่หรือไม่?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = bill.customerId
                        val cname = bill.customerName
                        customerToDelete = null
                        if (cid != null) {
                            vm.deleteCustomerDayOrders(cid, cname)
                        } else {
                            vm.deleteBill(bill.billId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ยืนยันลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
private fun ColumnScope.SummaryContent(
    date: String,
    summary: StoreSummary,
    dayBills: List<AdminDayBill>,
    dayOrderItems: List<AggregatedOrderItem>,
    onOpenBill: (String) -> Unit,
    onOpenCombined: (String) -> Unit,
    onDeleteCustomer: (AdminDayBill) -> Unit,
    onClearAll: () -> Unit
) {
    val dayTotal = dayBills.sumOf { it.dayTotal }
    val dayTotalMatchesClosed = moneyEquals(dayTotal, summary.total)
    val totalPieces = dayOrderItems.sumOf { it.totalQty }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCombined(date) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ปิดแล้ว",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "ดูรวมออเดอร์ทั้งหมด →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
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
            if (totalPieces > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "🍩 รวมไส้ขนมทั้งหมด $totalPieces ชิ้น (แตะเพื่อดูรายละเอียด)",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "บิลของวันนี้ (${dayBills.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                if (dayBills.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            "🗑️ เคลียร์ทั้งหมด",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        if (dayBills.isEmpty()) {
            item { Text("ยังไม่มีบิลในวันนี้", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(dayBills, key = { it.billId }) { bill ->
                DayBillRow(
                    bill = bill,
                    onClick = { onOpenBill(bill.billId) },
                    onDelete = { onDeleteCustomer(bill) }
                )
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
private fun DayBillRow(
    bill: AdminDayBill,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    bill.customerName ?: "-",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (bill.isOpen) {
                        "เปิด · ${bill.orderCount} ออเดอร์วันนี้"
                    } else {
                        "ปิดแล้ว · ${bill.orderCount} ออเดอร์วันนี้"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bill.spansDays) {
                    Text(
                        "ทั้งบิล ${formatMoney(bill.billTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    formatMoney(bill.dayTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "ลบออเดอร์ของ ${bill.customerName}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
