package com.ragnala.pos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * A line in the customer's cart. Mirrors service.CartLine but keeps the
 * order in memory until the customer confirms (DESIGN.md ACart).
 */
data class CartItem(
    val productId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val note: String? = null,
    val modifiers: List<CartModifier> = emptyList(),
    val imagePath: String? = null,
)

data class CartModifier(
    val groupName: String,
    val optionName: String,
    val priceDelta: Long,
)

/**
 * In-memory cart for Customer Mode. No DB writes until order confirmation
 * (OrderService.confirmOrder). Totals computed with [Money] - integer rupiah.
 * Process-death survival via SavedStateHandle is deferred (PRD A17/18).
 */
class CartViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    /** Add a product line; same product + same modifiers merges quantities. */
    fun add(
        productId: String,
        productName: String,
        unitPrice: Long,
        quantity: Int,
        note: String? = null,
        modifiers: List<CartModifier> = emptyList(),
        imagePath: String? = null,
    ) {
        _items.update { current ->
            val existing = current.indexOfFirst {
                it.productId == productId && it.modifiers == modifiers
            }
            if (existing >= 0) {
                current.mapIndexed { i, item ->
                    if (i == existing) item.copy(quantity = item.quantity + quantity) else item
                }
            } else {
                current + CartItem(
                    productId = productId,
                    productName = productName,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    note = note,
                    modifiers = modifiers,
                    imagePath = imagePath,
                )
            }
        }
    }

    /** Decrement the unmodified/base line for a product; remove it when quantity reaches zero. */
    fun decrementBaseProduct(productId: String) {
        _items.update { current -> decrementBaseProduct(current, productId) }
    }

    fun removeAt(index: Int) {
        _items.update { current -> current.filterIndexed { i, _ -> i != index } }
    }

    fun setQuantity(index: Int, quantity: Int) {
        if (quantity <= 0) {
            removeAt(index)
            return
        }
        _items.update { current ->
            current.mapIndexed { i, item ->
                if (i == index) item.copy(quantity = quantity) else item
            }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }

    /** Subtotal across lines, integer rupiah. */
    val subtotal: StateFlow<Long> = _items
        .map { items -> items.sumOf { Money.lineTotal(it.unitPrice, it.quantity) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val itemCount: StateFlow<Int> = _items
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

internal fun decrementBaseProduct(items: List<CartItem>, productId: String): List<CartItem> {
    val index = items.indexOfFirst {
        it.productId == productId && it.modifiers.isEmpty()
    }
    if (index < 0) return items

    val item = items[index]
    return if (item.quantity <= 1) {
        items.filterIndexed { i, _ -> i != index }
    } else {
        items.mapIndexed { i, line ->
            if (i == index) line.copy(quantity = line.quantity - 1) else line
        }
    }
}