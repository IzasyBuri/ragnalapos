package com.ragnala.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `rounds half up`() {
        assertEquals(1L, Money.roundHalfUp(BigDecimal("0.5")))
        assertEquals(2L, Money.roundHalfUp(BigDecimal("1.5")))
        assertEquals(1L, Money.roundHalfUp(BigDecimal("1.4999")))
        assertEquals(0L, Money.roundHalfUp(BigDecimal("0.499")))
    }

    @Test
    fun `line total is exact integer product`() {
        assertEquals(75000L, Money.lineTotal(25000, 3))
        assertEquals(0L, Money.lineTotal(12345, 0))
    }

    @Test
    fun `unit cost from pack price and size is exact`() {
        assertEquals(newBigDecimal("100.00000000"), Money.unitCost(33000, 330.0))
        assertEquals(newBigDecimal("26.66666667"), Money.unitCost(20000, 750.0))
        assertEquals(newBigDecimal("45.00000000"), Money.unitCost(45000, 1000.0))
    }

    @Test
    fun `derived per-unit cost rounds half up`() {
        assertEquals(100L, Money.roundHalfUp(Money.unitCost(33000, 330.0)))
        assertEquals(27L, Money.roundHalfUp(Money.unitCost(20000, 750.0)))
    }

    private fun newBigDecimal(s: String) = BigDecimal(s)
}
