package com.moudonut.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moudonut.admin.data.AdminProduct
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.ProductsSource
import com.moudonut.admin.data.toThaiMessage
import com.moudonut.admin.util.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsUiState(
    val loading: Boolean = true,
    val products: List<AdminProduct> = emptyList(),
    val error: String? = null,
    val updatingId: String? = null,
    val saving: Boolean = false,
    val message: String? = null
)

class ProductsViewModel(
    private val repository: ProductsSource = BillsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ProductsUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                fetch()
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun toggleProduct(productId: String, isAvailable: Boolean) {
        if (_state.value.updatingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(updatingId = productId, error = null) }
            try {
                val result = repository.toggleProduct(productId, isAvailable)
                fetch()
                val statusText = if (result.isAvailable) "เปิดขาย" else "ปิดขายชั่วคราว"
                _state.update {
                    it.copy(
                        updatingId = null,
                        message = "$statusText \"${result.name ?: ""}\" แล้ว"
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(updatingId = null, error = t.toThaiMessage()) }
            }
        }
    }

    fun updatePrice(productId: String, newPrice: Double) {
        if (_state.value.updatingId != null) return
        if (newPrice < 0) {
            _state.update { it.copy(error = "ราคาต้องไม่ติดลบ") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(updatingId = productId, error = null) }
            try {
                val updated = repository.updateProductPrice(productId, newPrice)
                fetch()
                _state.update {
                    it.copy(
                        updatingId = null,
                        message = "อัปเดตราคา \"${updated.name}\" เป็น ${formatMoney(updated.price)} แล้ว"
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(updatingId = null, error = t.toThaiMessage()) }
            }
        }
    }

    fun createProduct(name: String, price: Double, category: String) {
        if (_state.value.saving) return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _state.update { it.copy(error = "กรุณากรอกชื่อสินค้า") }
            return
        }
        if (price < 0) {
            _state.update { it.copy(error = "ราคาต้องไม่ติดลบ") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val created = repository.createProduct(trimmedName, price, category)
                fetch()
                _state.update {
                    it.copy(
                        saving = false,
                        message = "เพิ่มสินค้า \"${created.name}\" เรียบร้อยแล้ว"
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, error = t.toThaiMessage()) }
            }
        }
    }

    private suspend fun fetch() {
        val products = repository.adminProducts()
        _state.update { it.copy(loading = false, products = products) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
