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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AdminCode
import com.moudonut.admin.data.AdminCustomer

@Composable
fun CustomersScreen() {
    val vm: CustomersViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var addingTo by remember { mutableStateOf<AdminCustomer?>(null) }
    var editingCustomer by remember { mutableStateOf<AdminCustomer?>(null) }
    var deletingCustomer by remember { mutableStateOf<AdminCustomer?>(null) }
    var editingCode by remember { mutableStateOf<AdminCode?>(null) }
    var deletingCode by remember { mutableStateOf<AdminCode?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            creating = false
            addingTo = null
            editingCustomer = null
            deletingCustomer = null
            editingCode = null
            deletingCode = null
            snackbar.showSnackbar(message)
            vm.clearMessage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
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
                            onAddCode = { addingTo = customer },
                            onEditCustomer = { editingCustomer = customer },
                            onDeleteCustomer = { deletingCustomer = customer },
                            onEditCode = { editingCode = it },
                            onDeleteCode = { deletingCode = it }
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

    editingCustomer?.let { customer ->
        EditNameDialog(
            currentName = customer.name,
            saving = state.saving,
            onDismiss = { if (!state.saving) editingCustomer = null },
            onConfirm = { newName -> vm.updateCustomer(customer.customerId, newName) }
        )
    }

    deletingCustomer?.let { customer ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) deletingCustomer = null },
            title = { Text("ลบลูกค้า?") },
            text = {
                Text("ต้องการลบลูกค้า \"${customer.name}\" ใช่หรือไม่?\nรหัสผ่านและประวัติออเดอร์ทั้งหมดของลูกค้ารายนี้จะถูกลบถาวร")
            },
            confirmButton = {
                Button(
                    onClick = { vm.deleteCustomer(customer.customerId) },
                    enabled = !state.saving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (state.saving) "กำลังลบ..." else "ยืนยันลบลูกค้า")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCustomer = null }, enabled = !state.saving) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    editingCode?.let { code ->
        EditCodeDialog(
            code = code.code,
            currentLabel = code.label ?: "",
            saving = state.saving,
            onDismiss = { if (!state.saving) editingCode = null },
            onConfirm = { newLabel -> vm.updateCode(code.code, newLabel) }
        )
    }

    deletingCode?.let { code ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) deletingCode = null },
            title = { Text("ลบรหัสผ่าน?") },
            text = {
                Text("ต้องการลบรหัสผ่าน \"${code.code}\" (${code.label ?: "ไม่มีป้ายกำกับ"}) ใช่หรือไม่?")
            },
            confirmButton = {
                Button(
                    onClick = { vm.deleteCode(code.code) },
                    enabled = !state.saving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (state.saving) "กำลังลบ..." else "ยืนยันลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCode = null }, enabled = !state.saving) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: AdminCustomer,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onAddCode: () -> Unit,
    onEditCustomer: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onEditCode: (AdminCode) -> Unit,
    onDeleteCode: (AdminCode) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEditCustomer,
                        enabled = enabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "แก้ไขชื่อลูกค้า",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteCustomer,
                        enabled = enabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "ลบลูกค้า",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                code.code,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                code.label ?: "ไม่มีป้ายกำกับ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Switch(
                                checked = code.isActive,
                                onCheckedChange = { onToggle(code.code, it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            IconButton(
                                onClick = { onEditCode(code) },
                                enabled = enabled,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "แก้ไขป้ายกำกับ",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteCode(code) },
                                enabled = enabled,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "ลบรหัสนี้",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
private fun EditNameDialog(
    currentName: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val valid = name.isNotBlank() && name.trim() != currentName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("แก้ไขชื่อลูกค้า") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ชื่อลูกค้า (1–80 ตัวอักษร)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
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

@Composable
private fun EditCodeDialog(
    code: String,
    currentLabel: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newLabel: String) -> Unit
) {
    var label by remember { mutableStateOf(currentLabel) }
    val valid = label.isNotBlank() && label.trim() != currentLabel

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("แก้ไขป้ายกำกับรหัส $code") },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("ป้ายกำกับ (1–40 ตัว)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim()) },
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
