package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.data.repo.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Inventory screen (PRD §9 Inventory): ingredients, low-stock surfacing, adjustments. */
class InventoryViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {

    val ingredients: Flow<List<IngredientEntity>> = repository.ingredients()
    val lowStock: Flow<List<IngredientEntity>> = repository.lowStock()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Stock adjustment with mandatory reason (audited). [delta] positive = restock. */
    fun adjustStock(ingredientId: String, delta: Double, reason: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.adjust(
                    ingredientId = ingredientId,
                    delta = delta,
                    reason = reason,
                    userLabel = "owner",
                    now = System.currentTimeMillis(),
                )
                _message.value = "Stock adjusted"
            } catch (e: Exception) {
                _error.value = e.message ?: "Adjustment failed"
            }
        }
    }

    /** Creates or updates an ingredient. */
    fun saveIngredient(id: String?, name: String, unit: String, currentStock: Double, minStock: Double, costPerUnit: Long) {
        viewModelScope.launch {
            _error.value = null
            try {
                val now = System.currentTimeMillis()
                val existing = id?.let { repository.ingredient(it) }
                val entity = existing?.copy(
                    name = name.trim(),
                    unit = unit.trim(),
                    currentStock = currentStock,
                    minStock = minStock,
                    costPerUnit = costPerUnit,
                    updatedAt = now,
                ) ?: IngredientEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    unit = unit.trim(),
                    currentStock = currentStock,
                    minStock = minStock,
                    costPerUnit = costPerUnit,
                    createdAt = now,
                    updatedAt = now,
                )
                repository.save(entity)
                _message.value = "Ingredient saved"
            } catch (e: Exception) {
                _error.value = e.message ?: "Save failed"
            }
        }
    }

    /** Soft-deletes an ingredient; blocked while referenced by any recipe. */
    fun deleteIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.deleteBlockedIfReferenced(ingredient)
                _message.value = "Ingredient deleted"
            } catch (e: Exception) {
                _error.value = e.message ?: "Delete failed"
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearMessage() { _message.value = null }

    class Factory(
        private val repository: InventoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InventoryViewModel(repository) as T
    }
}
