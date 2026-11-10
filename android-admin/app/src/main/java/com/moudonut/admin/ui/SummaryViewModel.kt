package com.moudonut.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moudonut.admin.data.AdminDayBill
import com.moudonut.admin.data.AggregatedOrderItem
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.StoreSummary
import com.moudonut.admin.data.SummarySource
import com.moudonut.admin.data.toThaiMessage
import com.moudonut.admin.util.todayBangkok
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SummaryUiState(
    val date: String = todayBangkok(),
    val loading: Boolean = true,
    val summary: StoreSummary? = null,
    val dayBills: List<AdminDayBill> = emptyList(),
    val dayOrderItems: List<AggregatedOrderItem> = emptyList(),
    val error: String? = null,
    val refreshError: String? = null,
    val operating: Boolean = false,
    val message: String? = null
)

class SummaryViewModel(
    private val source: SummarySource = BillsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(SummaryUiState())
    val state = _state.asStateFlow()

    init {
        load(todayBangkok())
        observeRealtime()
    }

    private fun observeRealtime() {
        if (source is BillsRepository) {
            source.observeChanges(viewModelScope) {
                refresh()
            }
        }
    }

    fun setDate(date: String) {
        _state.update { it.copy(date = date) }
        load(date)
    }

    fun onScreenResumed() {
        if (_state.value.loading) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val date = _state.value.date
            try {
                val summary = source.summary(date)
                val bills = source.dayBills(date)
                val orderItems = source.dayOrderItems(date)
                if (_state.value.date != date) return@launch
                _state.update {
                    it.copy(
                        summary = summary,
                        dayBills = bills,
                        dayOrderItems = orderItems,
                        error = null,
                        refreshError = null
                    )
                }
            } catch (t: Throwable) {
                if (_state.value.date != date) return@launch
                _state.update { it.copy(refreshError = t.toThaiMessage()) }
            }
        }
    }

    fun load(date: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(date = date, loading = true, error = null, refreshError = null)
            }
            try {
                val summary = source.summary(date)
                val bills = source.dayBills(date)
                val orderItems = source.dayOrderItems(date)
                if (_state.value.date != date) return@launch
                _state.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        dayBills = bills,
                        dayOrderItems = orderItems,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                if (_state.value.date != date) return@launch
                _state.update { it.copy(loading = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun deleteCustomerDayOrders(customerId: String, customerName: String?) {
        viewModelScope.launch {
            _state.update { it.copy(operating = true) }
            val date = _state.value.date
            try {
                val res = source.deleteCustomerDayOrders(customerId, date)
                _state.update {
                    it.copy(
                        operating = false,
                        message = "ลบออเดอร์ของ ${customerName ?: "ลูกค้า"} (${res.ordersDeleted} รายการ) สำเร็จ"
                    )
                }
                load(date)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(operating = false, message = "ลบไม่สำเร็จ: ${t.toThaiMessage()}")
                }
            }
        }
    }

    fun clearDayOrders() {
        viewModelScope.launch {
            _state.update { it.copy(operating = true) }
            val date = _state.value.date
            try {
                val res = source.clearDayOrders(date)
                _state.update {
                    it.copy(
                        operating = false,
                        message = "เคลียร์ออเดอร์ทั้งหมดของวันนี้ (${res.ordersDeleted} รายการ) เรียบร้อย"
                    )
                }
                load(date)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(operating = false, message = "เคลียร์ไม่สำเร็จ: ${t.toThaiMessage()}")
                }
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            _state.update { it.copy(operating = true) }
            val date = _state.value.date
            try {
                source.deleteBill(billId)
                _state.update {
                    it.copy(operating = false, message = "ลบบิลเรียบร้อย")
                }
                load(date)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(operating = false, message = "ลบบิลไม่สำเร็จ: ${t.toThaiMessage()}")
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
