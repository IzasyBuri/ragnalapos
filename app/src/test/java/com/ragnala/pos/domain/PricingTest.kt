package com.ragnala.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PricingTest {

    @Test
    fun `no tax no service charge`() {
        val t = Pricing.calculate(listOf(LineItem(25000, 2)), 0.0, 0.0)
        assertEquals(50000L, t.subtotal)
        assertEquals(0L, t.serviceCharge)
        assertEquals(0L, t.tax)
        assertEquals(50000L, t.total)
    }

    @Test
    fun `subtotal sums modifier-inflated unit prices`() {
        val t = Pricing.calculate(listOf(LineItem(25000, 1), LineItem(28000, 2)), 0.0, 0.0)
        assertEquals(81000L, t.subtotal)
        assertEquals(81000L, t.total)
    }

    @Test
    fun `service charge on subtotal tax on subtotal plus sc`() {
        // SC 5% of 100000 = 5000; tax 11% of 105000 = 11550; total 116550
        val t = Pricing.calculate(listOf(LineItem(100000, 1)), 5.0, 11.0)
        assertEquals(100000L, t.subtotal)
        assertEquals(5000L, t.serviceCharge)
        assertEquals(11550L, t.tax)
        assertEquals(116550L, t.total)
    }

    @Test
    fun `rounding applied once at the total`() {
        // 3 x 33333 = 99999. SC 5% = 4999.95, tax 11% of 104998.95 = 11549.8845
        // total = 116548.8345 -> half-up 116549. Display SC 5000, tax 11550.
        val t = Pricing.calculate(listOf(LineItem(33333, 3)), 5.0, 11.0)
        assertEquals(99999L, t.subtotal)
        assertEquals(5000L, t.serviceCharge)
        assertEquals(11550L, t.tax)
        assertEquals(116549L, t.total)
    }

    @Test
    fun `zero subtotal yields zero totals`() {
        val t = Pricing.calculate(emptyList(), 5.0, 11.0)
        assertEquals(0L, t.subtotal)
        assertEquals(0L, t.serviceCharge)
        assertEquals(0L, t.tax)
        assertEquals(0L, t.total)
    }

    @Test
    fun `change due`() {
        assertEquals(0L, Pricing.changeDue(116549, 116549))
        assertEquals(3451L, Pricing.changeDue(116549, 120000))
    }

    @Test
    fun `change due throws when tendered is short`() {
        assertThrows(IllegalArgumentException::class.java) {
            Pricing.changeDue(116549, 100000)
        }
    }
}
