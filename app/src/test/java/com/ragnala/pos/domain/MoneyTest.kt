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
}
