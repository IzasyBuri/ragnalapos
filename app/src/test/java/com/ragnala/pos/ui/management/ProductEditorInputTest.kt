package com.ragnala.pos.ui.management

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductEditorInputTest {

    @Test
    fun `rupiah parser accepts digits and Indonesian thousands grouping`() {
        assertEquals(25_000L, parseRupiahInput("25000"))
        assertEquals(25_000L, parseRupiahInput("25.000"))
        assertEquals(1_250_000L, parseRupiahInput("1.250.000"))
        assertEquals(18_000L, parseRupiahInput("18.000"))
    }

    @Test
    fun `rupiah parser rejects malformed or decimal values`() {
        assertNull(parseRupiahInput("2.5"))
        assertNull(parseRupiahInput("25,000"))
        assertNull(parseRupiahInput("-25000"))
        assertNull(parseRupiahInput("25.00"))
        assertNull(parseRupiahInput("18000."))
        assertNull(parseRupiahInput(".000"))
        assertNull(parseRupiahInput(""))
    }

    @Test
    fun `formatPriceInput produces Indonesian thousands grouping`() {
        assertEquals("18.000", formatPriceInput(18_000L))
        assertEquals("25.000", formatPriceInput(25_000L))
        assertEquals("1.250.000", formatPriceInput(1_250_000L))
        assertEquals("500", formatPriceInput(500L))
    }

    @Test
    fun `formatPriceInput round-trips with parseRupiahInput`() {
        val values = listOf(500L, 18_000L, 25_000L, 1_250_000L, 100_000L, 999L)
        values.forEach { v ->
            assertEquals(v, parseRupiahInput(formatPriceInput(v)))
        }
    }
}
