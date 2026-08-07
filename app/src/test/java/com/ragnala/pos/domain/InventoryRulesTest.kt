package com.ragnala.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryRulesTest {

    @Test
    fun `deducts stock`() {
        val r = InventoryRules.deduct(100.0, 20.0, 30.0)
        assertEquals(80.0, r.newStock, 0.0001)
        assertFalse(r.isLow)
    }

    @Test
    fun `low when at minimum`() {
        val r = InventoryRules.deduct(30.0, 0.0, 30.0)
        assertEquals(30.0, r.newStock, 0.0001)
        assertTrue(r.isLow)
    }

    @Test
    fun `negative stock allowed but flagged low`() {
        val r = InventoryRules.deduct(5.0, 10.0, 3.0)
        assertEquals(-5.0, r.newStock, 0.0001)
        assertTrue(r.isLow)
    }

    @Test
    fun `fractional quantities`() {
        val r = InventoryRules.deduct(500.0, 18.5, 50.0)
        assertEquals(481.5, r.newStock, 0.0001)
        assertFalse(r.isLow)
    }
}
