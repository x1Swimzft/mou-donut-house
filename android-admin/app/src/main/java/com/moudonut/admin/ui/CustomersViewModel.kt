package com.moudonut.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moudonut.admin.data.AdminCustomer
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.toThaiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomersUiState(
    val loading: Boolean = true,
    val customers: List<AdminCustomer> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null
)

class CustomersViewModel : ViewModel() {
    private val repository = BillsRepository()
    private val _state = MutableStateFlow(CustomersUiState())
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

    fun createCustomer(name: String, code: String, label: String) =
        save("เพิ่มลูกค้าแล้ว") { repository.createCustomer(name, code, label) }

    fun updateCustomer(customerId: String, name: String) =
        save("แก้ไขชื่อลูกค้าเรียบร้อย") { repository.updateCustomer(customerId, name) }

    fun deleteCustomer(customerId: String) =
        save("ลบลูกค้าเรียบร้อย") { repository.deleteCustomer(customerId) }

    fun addCode(customerId: String, code: String, label: String) =
        save("เพิ่มรหัสแล้ว") { repository.addCode(customerId, code, label) }

    fun updateCode(code: String, label: String) =
        save("แก้ไขป้ายกำกับรหัสเรียบร้อย") { repository.updateCode(code, label) }

    fun deleteCode(code: String) =
        save("ลบรหัส $code เรียบร้อย") { repository.deleteCode(code) }

    fun toggleCode(code: String, active: Boolean) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val result = repository.toggleCode(code, active)
                fetch()
                _state.update {
                    it.copy(
                        saving = false,
                        message = if (result.isActive) "เปิดรหัส ${result.code} แล้ว" else "ปิดรหัส ${result.code} แล้ว"
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, error = t.toThaiMessage()) }
            }
        }
    }

    private fun save(success: String, block: suspend () -> Unit) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                block()
                fetch()
                _state.update { it.copy(saving = false, message = success) }
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, error = t.toThaiMessage()) }
            }
        }
    }

    private suspend fun fetch() {
        val customers = repository.customers()
        _state.update { it.copy(loading = false, customers = customers) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
