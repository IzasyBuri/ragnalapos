package com.ragnala.pos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderFlowTest {

    @Test
    fun `happy path transitions`() {
        assertTrue(OrderFlow.canTransition(OrderStatus.DRAFT, OrderStatus.WAITING_PAYMENT))
        assertTrue(OrderFlow.canTransition(OrderStatus.WAITING_PAYMENT, OrderStatus.PAID))
        assertTrue(OrderFlow.canTransition(OrderStatus.PAID, OrderStatus.FULFILLED))
        assertTrue(OrderFlow.canTransition(OrderStatus.FULFILLED, OrderStatus.ARCHIVED))
    }

    @Test
    fun `cancellation only before payment`() {
        assertTrue(OrderFlow.canTransition(OrderStatus.WAITING_PAYMENT, OrderStatus.CANCELLED))
        assertFalse(OrderFlow.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED))
        assertFalse(OrderFlow.canTransition(OrderStatus.FULFILLED, OrderStatus.CANCELLED))
    }

    @Test
    fun `void only from paid`() {
        assertTrue(OrderFlow.canTransition(OrderStatus.PAID, OrderStatus.VOIDED))
        assertFalse(OrderFlow.canTransition(OrderStatus.WAITING_PAYMENT, OrderStatus.VOIDED))
        assertFalse(OrderFlow.canTransition(OrderStatus.FULFILLED, OrderStatus.VOIDED))
    }

    @Test
    fun `terminal states are terminal`() {
        for (terminal in listOf(OrderStatus.ARCHIVED, OrderStatus.CANCELLED, OrderStatus.VOIDED)) {
            for (target in OrderStatus.entries) {
                assertFalse("$terminal -> $target must be illegal", OrderFlow.canTransition(terminal, target))
            }
        }
    }

    @Test
    fun `no skipping or backwards moves`() {
        assertFalse(OrderFlow.canTransition(OrderStatus.DRAFT, OrderStatus.PAID))
        assertFalse(OrderFlow.canTransition(OrderStatus.DRAFT, OrderStatus.FULFILLED))
        // Cannot mark fulfilled before paying
        assertFalse(OrderFlow.canTransition(OrderStatus.WAITING_PAYMENT, OrderStatus.FULFILLED))
        assertFalse(OrderFlow.canTransition(OrderStatus.ARCHIVED, OrderStatus.FULFILLED))
    }

    @Test
    fun `requireTransition throws on illegal move`() {
        assertThrows(IllegalStateException::class.java) {
            OrderFlow.requireTransition(OrderStatus.DRAFT, OrderStatus.PAID)
        }
    }

    @Test
    fun `pay later FULFILLED to PAID is allowed`() {
        // Pay-later flow: customer pays after receiving the drink
        assertTrue(OrderFlow.canTransition(OrderStatus.FULFILLED, OrderStatus.PAID))
    }
}
