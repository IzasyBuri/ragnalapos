package com.ragnala.pos.ui.barista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.OrderDao
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.domain.OrderStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Read-only live queue of active orders for Barista Mode.
 * Shows everything still in the kitchen flow (not yet completed/archived/cancelled/voided).
 */
class BaristaQueueViewModel(orderDao: OrderDao) : ViewModel() {

    val activeOrders: StateFlow<List<OrderEntity>> =
        orderDao
            .observeByStatuses(
                listOf(
                    OrderStatus.WAITING_PAYMENT,
                    OrderStatus.PAID,
                    OrderStatus.FULFILLED,
                ).map { it.name },
            )
            .map { orders ->
                // progress order: WAITING_PAYMENT(0) -> PAID(1) -> FULFILLED(2)
                val rank = mapOf(
                    OrderStatus.WAITING_PAYMENT to 0,
                    OrderStatus.PAID to 1,
                    OrderStatus.FULFILLED to 2,
                )
                orders.sortedWith(compareBy({ rank[it.status] ?: 9 }, { it.createdAt }))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    class Factory(private val orderDao: OrderDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BaristaQueueViewModel(orderDao) as T
    }
}
