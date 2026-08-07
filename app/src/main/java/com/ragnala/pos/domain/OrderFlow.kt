package com.ragnala.pos.domain

/**
 * Canonical order state machine (PRD §9 Order Management, decision D13).
 * Single source of truth — UI and data layer both consult this.
 */
enum class OrderStatus {
    DRAFT,
    WAITING_PAYMENT,
    PAID,
    FULFILLED,      // drink made & handed over — binary "done" (replaces PREPARING/READY/COMPLETED)
    ARCHIVED,
    CANCELLED,
    VOIDED,
}

object OrderFlow {

    private val transitions: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.DRAFT to setOf(OrderStatus.WAITING_PAYMENT),
        OrderStatus.WAITING_PAYMENT to setOf(OrderStatus.PAID, OrderStatus.CANCELLED),
        // Payment is the pivot; fulfilled = drink made & handed over (binary "done").
        OrderStatus.PAID to setOf(OrderStatus.FULFILLED, OrderStatus.VOIDED),
        // Pay-later: a fulfilled (already-served) order can still be paid afterward.
        OrderStatus.FULFILLED to setOf(OrderStatus.PAID, OrderStatus.ARCHIVED),
        OrderStatus.ARCHIVED to emptySet(),
        OrderStatus.CANCELLED to emptySet(),
        OrderStatus.VOIDED to emptySet(),
    )

    fun canTransition(from: OrderStatus, to: OrderStatus): Boolean =
        transitions[from]?.contains(to) == true

    /** Payment confirmation is the only lawful way into PAID. */
    fun requireTransition(from: OrderStatus, to: OrderStatus) {
        check(canTransition(from, to)) {
            "Illegal order transition: $from -> $to"
        }
    }
}
