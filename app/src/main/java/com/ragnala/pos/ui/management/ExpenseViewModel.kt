package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.ExpenseEntity
import com.ragnala.pos.data.repo.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Expenses (PRD §9): daily expense capture — category, amount, note, date. */
class ExpenseViewModel(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val monthStart: Long = startOfMonth(System.currentTimeMillis())

    val expenses: Flow<List<ExpenseEntity>> = repository.observeBetween(monthStart, System.currentTimeMillis())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addExpense(category: String, amount: Long, note: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.save(
                    ExpenseEntity(
                        id = UUID.randomUUID().toString(),
                        category = category.trim(),
                        amount = amount,
                        note = note.trim(),
                        date = System.currentTimeMillis(),
                    ),
                )
                _message.value = "Expense added"
            } catch (e: Exception) {
                _error.value = e.message ?: "Save failed"
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.delete(expense)
            _message.value = "Expense removed"
        }
    }

    fun clearError() { _error.value = null }
    fun clearMessage() { _message.value = null }

    private fun startOfMonth(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    class Factory(
        private val repository: ExpenseRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExpenseViewModel(repository) as T
    }
}