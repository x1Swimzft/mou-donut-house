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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AdminCustomer

@Composable
fun CustomersScreen() {
    val vm: CustomersViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var addingTo by remember { mutableStateOf<AdminCustomer?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            creating = false
            addingTo = null
            snackbar.showSnackbar(message)
            vm.clearMessage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ลูกค้า", style = MaterialTheme.typography.headlineSmall)
                    Text("${state.customers.size} ราย", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { vm.refresh() }, enabled = !state.loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { creating = true },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("เพิ่มลูกค้า")
            }

            Spacer(Modifier.height(12.dp))

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

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.customers.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ยังไม่มีลูกค้า", style = MaterialTheme.typography.titleMedium)
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.customers, key = { it.customerId }) { customer ->
                        CustomerCard(
                            customer = customer,
                            enabled = !state.saving,
                            onToggle = vm::toggleCode,
                            onAddCode = { addingTo = customer }
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        CustomerDialog(
            title = "เพิ่มลูกค้า",
            saving = state.saving,
            onDismiss = { if (!state.saving) creating = false },
            onConfirm = { name, code, label -> vm.createCustomer(name, code, label) }
        )
    }

    addingTo?.let { customer ->
        CustomerDialog(
            title = "เพิ่มรหัสให้ ${customer.name}",
            saving = state.saving,
            onDismiss = { if (!state.saving) addingTo = null },
            onConfirm = { _, code, label -> vm.addCode(customer.customerId, code, label) }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: AdminCustomer,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onAddCode: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (customer.codes.isEmpty()) {
                Text("ยังไม่มีรหัส", style = MaterialTheme.typography.bodyMedium)
            } else {
                customer.codes.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                code.code,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                code.label ?: "ไม่มีป้ายกำกับ",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = code.isActive,
                            onCheckedChange = { onToggle(code.code, it) },
                            enabled = enabled
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = onAddCode, enabled = enabled) {
                Text("เพิ่มรหัส")
            }
        }
    }
}

@Composable
private fun CustomerDialog(
    title: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, label: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val withName = title == "เพิ่มลูกค้า"
    val valid = code.isNotBlank() && label.isNotBlank() && (!withName || name.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (withName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("ชื่อลูกค้า") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("รหัสผ่าน") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("ป้ายกำกับ (1–40 ตัว)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), code.trim(), label.trim()) },
                enabled = valid && !saving
            ) {
                Text(if (saving) "กำลังบันทึก..." else "บันทึก")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("ยกเลิก")
            }
        }
    )
}
