package com.ragnala.pos.service

import com.ragnala.pos.data.db.IngredientDao
import com.ragnala.pos.data.db.OrderDao
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.data.db.OrderItemModifierEntity
import com.ragnala.pos.data.db.PaymentEntity
import com.ragnala.pos.domain.LineItem
import com.ragnala.pos.domain.Money
import com.ragnala.pos.domain.OrderFlow
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.domain.Pricing
import androidx.room.withTransaction
import java.util.UUID

/** A product line the customer is about to confirm. unitPrice already includes modifier deltas. */
data class CartLine(
    val productId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val note: String?,
    val modifiers: List<ModifierChoice> = emptyList(),
)

data class ModifierChoice(val optionName: String, val priceDelta: Long)

class OrderService(
    private val db: com.ragnala.pos.data.db.RagnalaDatabase,
    private val orderDao: OrderDao,
    private val ingredientDao: IngredientDao,
    private val auditService: AuditService,
) {

    /** Marks a draft as confirmed: creates the order in WAITING_PAYMENT. Idempotent. */
    suspend fun confirmOrder(
        draftId: String,
        customerName: String?,
        cartLines: List<CartLine>,
        scPercent: Double,
        taxPercent: Double,
        now: Long,
    ): String {
        val totals = Pricing.calculate(
            cartLines.map { LineItem(it.unitPrice, it.quantity) },
            scPercent,
            taxPercent,
        )
        // COGS snapshot at payment time (PRD §9 Reports) — computed in payOrder.
        val cogs = computeCogs(cartLines)

        db.withTransaction {
            // Idempotency guard INSIDE the transaction. Room serializes transactions on a single
            // writer, so a duplicate concurrent confirm of the same draft sees the committed row and
            // returns early — no duplicate order/items (PRD §10/§17). Order numbering is also
            // allocated here so two confirms cannot mint the same sequence number.
            val existing = orderDao.byId(draftId)
            if (existing != null) {
                check(existing.status == OrderStatus.WAITING_PAYMENT) {
                    "Draft already confirmed (status=${existing.status})"
                }
                return@withTransaction
            }

            val orderNumber = nextOrderNumber(now)
            val order = OrderEntity(
                id = draftId,
                orderNumber = orderNumber,
                status = OrderStatus.WAITING_PAYMENT,
                customerName = customerName?.trim()?.takeIf { it.isNotBlank() },
                subtotal = totals.subtotal,
                serviceCharge = totals.serviceCharge,
                tax = totals.tax,
                total = totals.total,
                cogs = cogs,
                createdAt = now,
                updatedAt = now,
                completedAt = null,
            )
            orderDao.upsert(order)

            val items = cartLines.mapIndexed { index, line ->
                OrderItemEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = draftId,
                    productId = line.productId,
                    productName = line.productName,
                    unitPrice = line.unitPrice,
                    quantity = line.quantity,
                    note = line.note,
                    position = index,
                )
            }
            orderDao.insertOrderItems(items)
            val modifiers = items.flatMapIndexed { index, item ->
                cartLines[index].modifiers.map {
                    OrderItemModifierEntity(
                        id = UUID.randomUUID().toString(),
                        orderItemId = item.id,
                        optionName = it.optionName,
                        priceDelta = it.priceDelta,
                    )
                }
            }
            orderDao.insertModifiers(modifiers)
        }
        return draftId
    }

    /**
     * Confirms a cash payment: validates tender, records payment, moves the order to PAID,
     * and deducts inventory — atomically. [pinVerified] must already be true (UI gates it).
     */
    suspend fun payOrderCash(
        orderId: String,
        tendered: Long,
        pinVerified: Boolean,
        userLabel: String,
        now: Long,
    ): Long {
        check(pinVerified) { "Barista PIN verification required" }
        val order = orderDao.byId(orderId) ?: error("Order not found: $orderId")
        OrderFlow.requireTransition(order.status, OrderStatus.PAID)

        val change = Pricing.changeDue(order.total, tendered)

        db.withTransaction {
            orderDao.upsert(
                order.copy(status = OrderStatus.PAID, updatedAt = now),
            )
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                method = "CASH",
                amount = order.total,
                tendered = tendered,
                changeGiven = change,
                confirmed = true,
                confirmedAt = now,
            )
            paymentDao.insert(payment)
            deductStockForOrder(order, userLabel, now)
        }
        return change
    }

    /** Confirms a non-cash payment (QRIS/card/transfer): manual confirmation, no tender math. */
    suspend fun payOrderNonCash(
        orderId: String,
        method: String,
        pinVerified: Boolean,
        userLabel: String,
        now: Long,
    ) {
        check(pinVerified) { "Barista PIN verification required" }
        require(method in NON_CASH_METHODS) { "Invalid non-cash method: $method" }
        val order = orderDao.byId(orderId) ?: error("Order not found: $orderId")
        OrderFlow.requireTransition(order.status, OrderStatus.PAID)

        db.withTransaction {
            orderDao.upsert(order.copy(status = OrderStatus.PAID, updatedAt = now))
            paymentDao.insert(
                PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    method = method,
                    amount = order.total,
                    tendered = null,
                    changeGiven = null,
                    confirmed = true,
                    confirmedAt = now,
                ),
            )
            deductStockForOrder(order, userLabel, now)
        }
    }

    /** Transition helper for FULFILLED / ARCHIVED — no payment side effects. */
    suspend fun transition(orderId: String, to: OrderStatus, now: Long) {
        val order = orderDao.byId(orderId) ?: error("Order not found: $orderId")
        OrderFlow.requireTransition(order.status, to)
        orderDao.upsert(
            order.copy(
                status = to,
                updatedAt = now,
                completedAt = if (to == OrderStatus.ARCHIVED) now else order.completedAt,
            ),
        )
    }

    /** Cancels a WAITING_PAYMENT order (pre-payment). Requires a reason (PRD §9). */
    suspend fun cancel(orderId: String, reason: String, userLabel: String, now: Long) {
        require(reason.isNotBlank()) { "Cancellation requires a reason" }
        val order = orderDao.byId(orderId) ?: error("Order not found: $orderId")
        OrderFlow.requireTransition(order.status, OrderStatus.CANCELLED)
        db.withTransaction {
            orderDao.upsert(order.copy(status = OrderStatus.CANCELLED, updatedAt = now))
            auditService.record(
                action = "CANCEL",
                entityType = "order",
                entityId = orderId,
                delta = "order #${order.orderNumber}",
                userLabel = userLabel,
                reason = reason,
                now = now,
            )
        }
    }

    /** Voids a PAID order: refunds stock, flags order VOIDED. PIN + reason mandatory. */
    suspend fun void(
        orderId: String,
        reason: String,
        pinVerified: Boolean,
        userLabel: String,
        now: Long,
    ) {
        check(pinVerified) { "Owner/barista PIN verification required" }
        require(reason.isNotBlank()) { "Void requires a reason" }
        val order = orderDao.byId(orderId) ?: error("Order not found: $orderId")
        OrderFlow.requireTransition(order.status, OrderStatus.VOIDED)

        db.withTransaction {
            orderDao.upsert(order.copy(status = OrderStatus.VOIDED, updatedAt = now))
            restoreStockForOrder(order, userLabel, now)
            auditService.record(
                action = "VOID",
                entityType = "order",
                entityId = orderId,
                delta = "order #${order.orderNumber} total=${order.total}",
                userLabel = userLabel,
                reason = reason,
                now = now,
            )
        }
    }

    /** Startup recovery (PRD §9): flags WAITING_PAYMENT orders idle beyond the window. */
    suspend fun staleWaitingPayment(olderThan: Long): List<OrderEntity> =
        orderDao.waitingPaymentOlderThan(olderThan)

    /** Housekeeping: purges drafts abandoned beyond [olderThan]. Returns count removed. */
    suspend fun purgeAbandonedDrafts(olderThan: Long): Int =
        orderDao.deleteDraftsOlderThan(olderThan)

    private val paymentDao get() = db.paymentDao()

    private suspend fun nextOrderNumber(now: Long): Long {
        // PRD §9 Order Numbering: sequential per day, resets daily (#001, #002...)
        val dayStart = startOfDay(now)
        return orderDao.maxOrderNumberSince(dayStart) + 1
    }

    private fun startOfDay(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Computes cost of goods for the cart lines at order time (PRD §9 Reports). */
    private suspend fun computeCogs(cartLines: List<CartLine>): Long {
        val ingredientIds = cartLines
            .flatMap { ingredientDao.recipeForProductOnce(it.productId) }
            .map { it.ingredientId }
            .distinct()
        if (ingredientIds.isEmpty()) return 0L
        val ingredients = ingredientDao.byIds(ingredientIds)
        val costById = ingredients.associateBy { it.id }

        return cartLines.sumOf { line ->
            val recipe = ingredientDao.recipeForProductOnce(line.productId)
            val lineCost = recipe.sumOf { item ->
                val ingredient = costById[item.ingredientId] ?: return@sumOf 0L
                val perUnit = when {
                    ingredient.purchasePrice != null &&
                        ingredient.packSize != null &&
                        ingredient.packSize > 0 ->
                        Money.unitCost(ingredient.purchasePrice, ingredient.packSize)

                    else -> java.math.BigDecimal.valueOf(ingredient.costPerUnit.toLong())
                }
                Money.roundHalfUp(
                    perUnit.multiply(java.math.BigDecimal.valueOf(item.quantity))
                )
            }
            lineCost * line.quantity
        }
    }

    /** Deducts recipe ingredients for a paid order, recording the stock audit. */
    private suspend fun deductStockForOrder(order: OrderEntity, userLabel: String, now: Long) {
        val orderItems = orderDao.itemsForOrder(order.id)
        val productIds = orderItems.map { it.productId }.distinct()
        val recipes = productIds.associateWith { ingredientDao.recipeForProductOnce(it) }
        val ingredientIds = recipes.values.flatten().map { it.ingredientId }.distinct()
        if (ingredientIds.isEmpty()) return

        val ingredients = ingredientDao.byIds(ingredientIds).associateBy { it.id }
        val updated = mutableMapOf<String, Double>()

        orderItems.forEach { item ->
            recipes[item.productId]?.forEach { recipeItem ->
                val current = updated[recipeItem.ingredientId]
                    ?: (ingredients[recipeItem.ingredientId]?.currentStock ?: return@forEach)
                updated[recipeItem.ingredientId] = current - recipeItem.quantity * item.quantity
            }
        }
        updated.forEach { (id, newStock) ->
            ingredientDao.setStock(id, newStock, now)
            auditService.record(
                action = "STOCK_DEDUCT",
                entityType = "ingredient",
                entityId = id,
                delta = "$newStock (order #${order.orderNumber})",
                userLabel = userLabel,
                reason = "Payment for order #${order.orderNumber}",
                now = now,
            )
        }
    }

    /** Restores stock when a paid order is voided (PRD §9: voided orders refund stock). */
    private suspend fun restoreStockForOrder(order: OrderEntity, userLabel: String, now: Long) {
        val orderItems = orderDao.itemsForOrder(order.id)
        val productIds = orderItems.map { it.productId }.distinct()
        val recipes = productIds.associateWith { ingredientDao.recipeForProductOnce(it) }
        val ingredientIds = recipes.values.flatten().map { it.ingredientId }.distinct()
        if (ingredientIds.isEmpty()) return

        val ingredients = ingredientDao.byIds(ingredientIds).associateBy { it.id }
        val updated = mutableMapOf<String, Double>()

        orderItems.forEach { item ->
            recipes[item.productId]?.forEach { recipeItem ->
                val current = updated[recipeItem.ingredientId]
                    ?: (ingredients[recipeItem.ingredientId]?.currentStock ?: return@forEach)
                updated[recipeItem.ingredientId] = current + recipeItem.quantity * item.quantity
            }
        }
        updated.forEach { (id, newStock) ->
            ingredientDao.setStock(id, newStock, now)
            auditService.record(
                action = "STOCK_RESTORE",
                entityType = "ingredient",
                entityId = id,
                delta = "$newStock (void of order #${order.orderNumber})",
                userLabel = userLabel,
                reason = "Void of order #${order.orderNumber}",
                now = now,
            )
        }
    }

    companion object {
        val NON_CASH_METHODS = setOf("QRIS", "DEBIT", "CREDIT_CARD", "BANK_TRANSFER")
    }
}
