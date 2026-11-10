package com.moudonut.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.util.formatDateTime
import com.moudonut.admin.util.formatMoney
import com.moudonut.admin.util.formatTime

@Composable
fun BillDetailScreen(id: String, onBack: () -> Unit) {
    val vm: BillDetailViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(id) {
        vm.load(id)
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("กลับ") }
                TextButton(onClick = { vm.load(id) }, enabled = !state.loading) {
                    Text("รีเฟรช")
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val bill = state.bill
                if (bill == null) {
                    Text(
                        state.error ?: "ไม่พบบิล",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(bill.customerName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "สถานะ: ${if (bill.status == "open") "เปิด" else "ปิดแล้ว"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "เปิดเมื่อ ${formatDateTime(bill.openedAt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (bill.closedAt != null) {
                        Text(
                            "ปิดเมื่อ ${formatDateTime(bill.closedAt)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    if (state.orders.isEmpty()) {
                        Text("ยังไม่มีออเดอร์", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.orders, key = { it.id }) { order ->
                                Column(Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "ออเดอร์ ${formatTime(order.createdAt)}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            formatMoney(order.total),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    order.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${item.productName} ×${item.qty}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                formatMoney(item.unitPrice * item.qty),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ยอดรวมทั้งบิล", style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatMoney(bill.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(12.dp))
                    if (bill.status == "open") {
                        Button(
                            onClick = { vm.close(bill.id) },
                            enabled = !state.closing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.closing) "กำลังปิด..." else "ปิดบิลนี้")
                        }
                    } else {
                        Text("บิลนี้ปิดแล้ว", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
