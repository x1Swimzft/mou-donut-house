package com.moudonut.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moudonut.admin.data.AggregatedOrderItem
import com.moudonut.admin.data.BillRow
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.toThaiMessage
import com.moudonut.admin.util.PaperMenu
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BillsEvent {
    data object NewOrder : BillsEvent
}

data class BillsUiState(
    val loading: Boolean = true,
    val bills: List<BillRow> = emptyList(),
    val error: String? = null,
    val closing: Boolean = false,
    val message: String? = null,
    val selectedBillIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
) {
    val selectedBills: List<BillRow> get() = bills.filter { it.id in selectedBillIds }
    val aggregatedItems: List<AggregatedOrderItem> get() = PaperMenu.aggregateBills(selectedBills)
    val totalSelectedPieces: Int get() = aggregatedItems.sumOf { it.totalQty }
    val totalSelectedRevenue: Double get() = aggregatedItems.sumOf { it.totalAmount }
}

class BillsViewModel(
    private val repository: BillsRepository = BillsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(BillsUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<BillsEvent>()
    val events = _events.asSharedFlow()

    init {
        refresh()
        observeRealtime()
    }

    private fun observeRealtime() {
        repository.observeChanges(viewModelScope) { action ->
            if (action is PostgresAction.Insert) {
                _events.emit(BillsEvent.NewOrder)
            }
            refreshSilently()
        }
    }

    private fun refreshSilently() {
        viewModelScope.launch {
            try {
                val bills = repository.openBills()
                _state.update { current ->
                    val activeIds = bills.map { it.id }.toSet()
                    val pruned = current.selectedBillIds.intersect(activeIds)
                    current.copy(bills = bills, selectedBillIds = pruned, error = null)
                }
            } catch (_: Throwable) {}
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val bills = repository.openBills()
                _state.update { current ->
                    val activeIds = bills.map { it.id }.toSet()
                    val pruned = current.selectedBillIds.intersect(activeIds)
                    current.copy(loading = false, bills = bills, selectedBillIds = pruned)
                }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun toggleSelectionMode() {
        _state.update {
            if (it.isSelectionMode) {
                it.copy(isSelectionMode = false, selectedBillIds = emptySet())
            } else {
                it.copy(isSelectionMode = true)
            }
        }
    }

    fun toggleSelectBill(id: String) {
        _state.update { current ->
            val next = current.selectedBillIds.toMutableSet()
            if (next.contains(id)) {
                next.remove(id)
            } else {
                next.add(id)
            }
            current.copy(selectedBillIds = next)
        }
    }

    fun selectAll() {
        _state.update { current ->
            current.copy(selectedBillIds = current.bills.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedBillIds = emptySet()) }
    }

    fun closeSelected() {
        val ids = _state.value.selectedBillIds.toList()
        if (ids.isEmpty() || _state.value.closing) return
        viewModelScope.launch {
            _state.update { it.copy(closing = true, error = null) }
            try {
                val count = repository.closeAll(ids)
                _state.update {
                    it.copy(
                        closing = false,
                        selectedBillIds = emptySet(),
                        isSelectionMode = false,
                        message = "ปิด $count บิลที่เลือกแล้ว"
                    )
                }
                refresh()
            } catch (t: Throwable) {
                _state.update { it.copy(closing = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun closeAll() {
        val ids = _state.value.bills.map { it.id }
        if (ids.isEmpty() || _state.value.closing) return
        viewModelScope.launch {
            _state.update { it.copy(closing = true, error = null) }
            try {
                val count = repository.closeAll(ids)
                _state.update { it.copy(closing = false, message = "ปิด $count บิลแล้ว") }
                refresh()
            } catch (t: Throwable) {
                _state.update { it.copy(closing = false, error = t.toThaiMessage()) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}

