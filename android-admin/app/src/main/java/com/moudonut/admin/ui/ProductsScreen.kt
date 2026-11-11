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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moudonut.admin.data.AdminProduct
import com.moudonut.admin.util.formatMoney

@Composable
fun ProductsScreen(vm: ProductsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<AdminProduct?>(null) }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            creating = false
            editingProduct = null
            snackbar.showSnackbar(msg)
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
                    Text("สินค้า", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    val activeCount = state.products.count { it.isAvailable }
                    Text(
                        "${state.products.size} รายการ (เปิดขาย $activeCount รายการ)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.refresh() }, enabled = !state.loading && !state.saving) {
                    Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { creating = true },
                enabled = !state.saving && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("เพิ่มสินค้าใหม่")
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
                state.loading && state.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ไม่พบรายการสินค้า", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            ProductRowCard(
                                product = product,
                                isUpdating = state.updatingId == product.id,
                                onToggle = { active -> vm.toggleProduct(product.id, active) },
                                onEditPrice = { editingProduct = product }
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        CreateProductDialog(
            saving = state.saving,
            onDismiss = { if (!state.saving) creating = false },
            onConfirm = { name, price, category ->
                vm.createProduct(name, price, category)
            }
        )
    }

    editingProduct?.let { product ->
        EditPriceDialog(
            product = product,
            saving = state.updatingId == product.id,
            onDismiss = { if (state.updatingId != product.id) editingProduct = null },
            onConfirm = { newPrice ->
                vm.updatePrice(product.id, newPrice)
            }
        )
    }
}

@Composable
private fun ProductRowCard(
    product: AdminProduct,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isAvailable) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (product.isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatMoney(product.price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (product.category == "drink") "เครื่องดื่ม" else "โดนัท",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (product.isAvailable) "เปิดขาย" else "หมดชั่วคราว",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = if (product.isAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditPrice,
                    enabled = !isUpdating,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "แก้ไขราคา",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Switch(
                        checked = product.isAvailable,
                        onCheckedChange = onToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPriceDialog(
    product: AdminProduct,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (price: Double) -> Unit
) {
    var priceText by remember(product) { mutableStateOf(product.price.toString()) }
    val priceVal = priceText.toDoubleOrNull()
    val isValid = priceVal != null && priceVal >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("แก้ไขราคา") },
        text = {
            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("ราคา (บาท)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (priceVal != null) onConfirm(priceVal) },
                enabled = isValid && !saving
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
private fun CreateProductDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("donut") }

    val priceVal = priceText.toDoubleOrNull()
    val isValid = name.isNotBlank() && priceVal != null && priceVal >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มสินค้าใหม่") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อสินค้า") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("ราคา (บาท)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("หมวดหมู่สินค้า", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "donut",
                        onClick = { category = "donut" },
                        label = { Text("โดนัท") }
                    )
                    FilterChip(
                        selected = category == "drink",
                        onClick = { category = "drink" },
                        label = { Text("เครื่องดื่ม") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (priceVal != null) onConfirm(name.trim(), priceVal, category) },
                enabled = isValid && !saving
            ) {
                Text(if (saving) "กำลังบันทึก..." else "เพิ่มสินค้า")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("ยกเลิก")
            }
        }
    )
}
