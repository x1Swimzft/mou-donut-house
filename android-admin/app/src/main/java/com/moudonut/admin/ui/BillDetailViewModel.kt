package com.moudonut.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moudonut.admin.data.BillRow
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.OrderRow
import com.moudonut.admin.data.toThaiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillDetailUiState(
    val loading: Boolean = true,
    val bill: BillRow? = null,
    val orders: List<OrderRow> = emptyList(),
    val error: String? = null,
    val closing: Boolean = false,
    val message: String? = null
)

class BillDetailViewModel : ViewModel() {
    private val repository = BillsRepository()
    private val _state = MutableStateFlow(BillDetailUiState())
    val state = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val bill = repository.bill(id)
                val orders = repository.billOrders(id)
                _state.update { it.copy(loading = false, bill = bill, orders = orders) }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun close(id: String) {
        if (_state.value.closing) return
        viewModelScope.launch {
            _state.update { it.copy(closing = true, error = null) }
            try {
                repository.closeBill(id)
                _state.update { it.copy(closing = false, message = "ปิดบิลแล้ว") }
                load(id)
            } catch (t: Throwable) {
                _state.update { it.copy(closing = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun delete(id: String, onDeleted: () -> Unit) {
        if (_state.value.closing) return
        viewModelScope.launch {
            _state.update { it.copy(closing = true, error = null) }
            try {
                repository.deleteBill(id)
                _state.update { it.copy(closing = false, message = "ลบบิลเรียบร้อย") }
                onDeleted()
            } catch (t: Throwable) {
                _state.update { it.copy(closing = false, error = t.toThaiMessage()) }
            }
        }
    }
}
