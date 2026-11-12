package com.moudonut.admin.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AggregatedOrderItem
import com.moudonut.admin.data.BillRow
import com.moudonut.admin.util.NotificationNotifier
import com.moudonut.admin.util.PaperMenu
import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(onOpen: (String) -> Unit) {
    val vm: BillsViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmAll by remember { mutableStateOf(false) }
    var confirmCloseSelected by remember { mutableStateOf(false) }
    var showCombinedSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    BackHandler(enabled = state.isSelectionMode) {
        vm.toggleSelectionMode()
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                BillsEvent.NewOrder -> {
                    snackbar.showSnackbar("มีออเดอร์ใหม่เข้ามา!")
                }
            }
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.clearMessage()
        }
    }

    // Auto-dismiss sheet if selections pruned to empty
    LaunchedEffect(state.selectedBillIds) {
        if (state.selectedBillIds.isEmpty()) {
            showCombinedSheet = false
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
            if (!state.isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("บิลที่เปิดอยู่", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.bills.size} บิล",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.bills.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { vm.toggleSelectionMode() },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("รวมบิล", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        IconButton(onClick = { vm.refresh() }, enabled = !state.loading) {
                            Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("เลือกบิลเพื่อรวม", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "เลือกแล้ว ${state.selectedBillIds.size} จาก ${state.bills.size} บิล",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            if (state.selectedBillIds.size == state.bills.size) {
                                vm.clearSelection()
                            } else {
                                vm.selectAll()
                            }
                        }) {
                            Text(if (state.selectedBillIds.size == state.bills.size) "ยกเลิก" else "เลือกทั้งหมด")
                        }
                        TextButton(onClick = { vm.toggleSelectionMode() }) {
                            Text("เสร็จสิ้น")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.bills.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ยังไม่มีบิลที่เปิดอยู่", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.bills, key = { it.id }) { bill ->
                        BillCard(
                            bill = bill,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = bill.id in state.selectedBillIds,
                            onToggleSelect = { vm.toggleSelectBill(bill.id) },
                            onClick = { onOpen(bill.id) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (state.isSelectionMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "เลือก ${state.selectedBillIds.size} บิล (${state.totalSelectedPieces} ชิ้น)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatMoney(state.totalSelectedRevenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showCombinedSheet = true },
                                    enabled = state.selectedBillIds.isNotEmpty() && !state.closing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📋 ดูรวมไส้ขนม (${state.selectedBillIds.size})")
                                }
                                OutlinedButton(
                                    onClick = { confirmCloseSelected = true },
                                    enabled = state.selectedBillIds.isNotEmpty() && !state.closing,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("ปิด ${state.selectedBillIds.size} บิล")
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { confirmAll = true },
                        enabled = !state.closing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.closing) "กำลังปิด..."
                            else "ปิดทั้งร้าน (${state.bills.size} บิล)"
                        )
                    }
                }
            }
        }
    }

    if (showCombinedSheet && state.selectedBills.isNotEmpty()) {
        CombinedSelectedBillsSheet(
            bills = state.selectedBills,
            aggregatedItems = state.aggregatedItems,
            onDismiss = { showCombinedSheet = false }
        )
    }

    if (confirmCloseSelected) {
        AlertDialog(
            onDismissRequest = { confirmCloseSelected = false },
            title = { Text("ปิดบิลที่เลือก?") },
            text = {
                Text(
                    "ต้องการปิด ${state.selectedBillIds.size} บิลที่เลือก ยอดรวม ${formatMoney(state.totalSelectedRevenue)} ใช่หรือไม่?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCloseSelected = false
                        vm.closeSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ยืนยันปิดบิลที่เลือก")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloseSelected = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("ปิดทั้งร้าน?") },
            text = {
                Text(
                    "จะปิด ${state.bills.size} บิลที่แสดงอยู่ตอนนี้ " +
                        "บิลที่เปิดใหม่หลังจากนี้จะยังเปิดอยู่"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAll = false
                    vm.closeAll()
                }) { Text("ปิดทั้งหมด") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAll = false }) { Text("ยกเลิก") }
            }
        )
    }
}

@Composable
private fun BillCard(
    bill: BillRow,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    val sortedItems = remember(bill.items) {
        bill.items.sortedWith(compareBy({ PaperMenu.sortIndex(it.productName) }, { it.productName }))
    }

    Card(
        onClick = if (isSelectionMode) onToggleSelect else onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        bill.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatMoney(bill.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${bill.itemCount} ชิ้น · เปิด ${formatTime(bill.openedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sortedItems.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sortedItems.joinToString(" · ") { "${it.productName} x${it.qty}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CombinedSelectedBillsSheet(
    bills: List<BillRow>,
    aggregatedItems: List<AggregatedOrderItem>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val totalPieces = aggregatedItems.sumOf { it.totalQty }
    val totalRevenue = aggregatedItems.sumOf { it.totalAmount }
    val customerNames = bills.map { it.customerName }.distinct()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "รวมยอดทำขนม (${bills.size} บิล)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "รวม $totalPieces ชิ้น · ${formatMoney(totalRevenue)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(
                    onClick = {
                        val text = PaperMenu.formatSummaryText(bills, aggregatedItems)
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "คัดลอกสรุปรายการแล้ว", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("📋 คัดลอก")
                }
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                customerNames.forEach { name ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "👤 $name",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (aggregatedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ไม่มีรายการสินค้าในบิลที่เลือก", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(aggregatedItems, key = { it.productName }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            "${item.totalQty} ชิ้น",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "@ ${formatMoney(item.unitPrice)} · รวม ${formatMoney(item.totalAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (item.customerBreakdown.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item.customerBreakdown.forEach { cust ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    "${cust.customerName}: ${cust.qty} ชิ้น",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
