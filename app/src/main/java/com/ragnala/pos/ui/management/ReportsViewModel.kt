package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.service.ReportsService
import com.ragnala.pos.service.ReportSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReportPeriod(val label: String) {
    DAILY("Today"),
    WEEKLY("Last 7 days"),
    MONTHLY("Last 30 days"),
}

/** Reports (PRD §9): daily / weekly / monthly sales, profit, best sellers, payment split. */
class ReportsViewModel(
    private val reportsService: ReportsService,
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.DAILY)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    private val _summary = MutableStateFlow<ReportSummary?>(null)
    val summary: StateFlow<ReportSummary?> = _summary.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun selectPeriod(p: ReportPeriod) {
        if (_period.value == p) return
        _period.value = p
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val now = System.currentTimeMillis()
                val end = now
                val start = when (_period.value) {
                    ReportPeriod.DAILY -> startOfDay(now)
                    ReportPeriod.WEEKLY -> startOfDay(now) - 6L * 24 * 3600 * 1000
                    ReportPeriod.MONTHLY -> startOfDay(now) - 29L * 24 * 3600 * 1000
                }
                _summary.value = reportsService.summary(start, end)
            } catch (e: Exception) {
                _error.value = e.message ?: "Report failed"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun startOfDay(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    class Factory(
        private val reportsService: ReportsService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportsViewModel(reportsService) as T
    }
}