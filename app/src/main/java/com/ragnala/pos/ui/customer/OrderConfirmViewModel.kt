package com.ragnala.pos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.domain.Money
import com.ragnala.pos.service.CartLine
import com.ragnala.pos.service.ModifierChoice
import com.ragnala.pos.service.OrderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Confirmation & payment flow for Customer Mode.
 * Calls OrderService.confirmOrder then payOrderCash (cash path).
 * UI collects: cart items, customer name, scPercent/taxPercent from settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderConfirmViewModel(
    private val orderService: OrderService,
    private val cartItems: List<CartItem>,
    private val customerName: String,
    private val scPercent: Double,
    private val taxPercent: Double,
) : ViewModel() {

    sealed interface Result {
        data class Success(val orderId: String) : Result
        data class Failure(val message: String) : Result
    }

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    /**
     * A single stable draft marker for the whole confirmation flow. Reused across retries so
     * OrderService's in-transaction idempotency guard actually dedupes (PRD §17). The button is
     * disabled while [submitting] to prevent a double-tap from launching two confirms.
     */
    private val draftId: String = java.util.UUID.randomUUID().toString()

    /** Execute the customer-side flow: confirm draft -> WAITING_PAYMENT.
     *  Payment (with Barista PIN) is completed at the counter in Barista Mode. */
    fun confirmOrder(now: Long) {
        if (_submitting.value || _result.value is Result.Success) return
        _submitting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) confirmOrder -> WAITING_PAYMENT
                val cartLines = cartItems.map { item ->
                    CartLine(
                        productId = item.productId,
                        productName = item.productName,
                        unitPrice = item.unitPrice,
                        quantity = item.quantity,
                        note = item.note,
                        modifiers = item.modifiers.map { m ->
                            ModifierChoice(m.optionName, m.priceDelta)
                        },
                    )
                }
                val orderId = orderService.confirmOrder(
                    draftId = draftId,
                    customerName = if (customerName.isBlank()) null else customerName,
                    cartLines = cartLines,
                    scPercent = scPercent,
                    taxPercent = taxPercent,
                    now = now,
                )
                _result.value = Result.Success(orderId)
            } catch (e: Exception) {
                _result.value = Result.Failure(e.message ?: "Order failed")
            } finally {
                _submitting.value = false
            }
        }
    }

    class Factory(
        private val orderService: OrderService,
        private val cartItems: List<CartItem>,
        private val customerName: String,
        private val scPercent: Double,
        private val taxPercent: Double,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OrderConfirmViewModel(orderService, cartItems, customerName, scPercent, taxPercent) as T
    }
}