package com.ragnala.pos.ui.barista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.OrderDao
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.data.db.PaymentDao
import com.ragnala.pos.data.db.PaymentEntity
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.service.OrderService
import com.ragnala.pos.service.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Barista Mode — slice 2: order detail + pay flow.
 *
 * The Barista PIN is only required to ENTER Barista Mode (queue gate, see BaristaUnlockScreen);
 * no PIN is re-prompted here at payment/void. Payment/void still hand `pinVerified = true` to the
 * service so stock/audit side effects proceed (session already authenticated).
 */
class BaristaDetailViewModel(
    private val orderId: String,
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val orderService: OrderService,
    private val settingsService: SettingsService,
) : ViewModel() {

    private val _items = MutableStateFlow<List<OrderItemEntity>>(emptyList())
    val items: StateFlow<List<OrderItemEntity>> = _items

    private val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments

    private val _storeName = MutableStateFlow("Ragnala Coffee & Botanee")
    val storeName: StateFlow<String> = _storeName

    private val _logoPath = MutableStateFlow<String?>(null)
    val logoPath: StateFlow<String?> = _logoPath

    private val _qrisPath = MutableStateFlow<String?>(null)
    val qrisPath: StateFlow<String?> = _qrisPath

    /** Persist the logo file path so it survives restarts. */
    fun setLogoPath(path: String) {
        _logoPath.value = path
        viewModelScope.launch { settingsService.setLogoPath(path) }
    }

    /** Persist the QRIS image path so it survives restarts (owner sets it in Management). */
    fun setQrisImagePath(path: String) {
        _qrisPath.value = path
        viewModelScope.launch { settingsService.setQrisImagePath(path) }
    }

    /** Clear the persisted QRIS image and update print state immediately. */
    fun removeQrisImage() {
        _qrisPath.value = null
        viewModelScope.launch { settingsService.setQrisImagePath("") }
    }

    /** Clear the persisted receipt logo and update print state immediately. */
    fun removeLogo() {
        _logoPath.value = null
        viewModelScope.launch { settingsService.setLogoPath("") }
    }

    init {
        viewModelScope.launch {
            _items.value = orderDao.itemsForOrder(orderId)
            _payments.value = paymentDao.forOrder(orderId)
            _storeName.value = settingsService.storeName()
            _logoPath.value = settingsService.logoPath().ifBlank { null }
            _qrisPath.value = settingsService.qrisImagePath().ifBlank { null }
        }
    }

    val order: Flow<OrderEntity?> =
        orderDao.observeByStatuses(OrderStatus.entries.map { it.name })
            .map { list -> list.find { o -> o.id == orderId } }

    private val _paid = MutableStateFlow(false)
    val paid: StateFlow<Boolean> = _paid

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Surface a transient UI error message. */
    fun showError(message: String) {
        _error.value = message
    }

    /** Pay the order in cash. No PIN — the barista already passed Barista Mode. */
    fun payCash(tendered: Long, userLabel: String, now: Long) {
        if (_paid.value) return
        viewModelScope.launch {
            try {
                orderService.payOrderCash(
                    orderId = orderId,
                    tendered = tendered,
                    pinVerified = true,
                    userLabel = userLabel,
                    now = now,
                )
                _payments.value = paymentDao.forOrder(orderId)
                _paid.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Payment failed"
            }
        }
    }

    /** Confirm a non-cash payment (QRIS/card/transfer). No PIN needed in Barista Mode. */
    fun payNonCash(method: String, userLabel: String, now: Long) {
        if (_paid.value) return
        viewModelScope.launch {
            try {
                orderService.payOrderNonCash(
                    orderId = orderId,
                    method = method,
                    pinVerified = true,
                    userLabel = userLabel,
                    now = now,
                )
                _payments.value = paymentDao.forOrder(orderId)
                _paid.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Payment failed"
            }
        }
    }

    /** Cancels a WAITING_PAYMENT order (pre-payment). Reason required (PRD §9). */
    fun cancelOrder(reason: String, userLabel: String, now: Long) {
        viewModelScope.launch {
            try {
                orderService.cancel(orderId = orderId, reason = reason, userLabel = userLabel, now = now)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Cancel failed"
            }
        }
    }

    /** Voids a PAID order. Requires a reason; restores stock (PRD §9). */
    fun voidOrder(reason: String, userLabel: String, now: Long) {
        viewModelScope.launch {
            try {
                orderService.void(orderId = orderId, reason = reason, pinVerified = true, userLabel = userLabel, now = now)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Void failed"
            }
        }
    }

    fun transitionTo(to: OrderStatus, now: Long) {
        viewModelScope.launch {
            try {
                orderService.transition(orderId, to, now)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Transition failed"
            }
        }
    }

    fun clearError() { _error.value = null }
}

class BaristaDetailViewModelFactory(
    private val orderId: String,
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val orderService: OrderService,
    private val settingsService: SettingsService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BaristaDetailViewModel(orderId, orderDao, paymentDao, orderService, settingsService) as T
}
