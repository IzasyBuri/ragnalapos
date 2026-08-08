package com.ragnala.pos.ui.receipt

import com.ragnala.pos.data.db.OrderItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothReceiptPrinterTest {

    @Test
    fun `aligned row fits 58mm columns and right aligns value`() {
        val row = BluetoothReceiptPrinter.alignedRow(
            label = "Service charge",
            value = "Rp1.400,00",
        )

        assertEquals(32, row.length)
        assertTrue(row.startsWith("Service charge"))
        assertTrue(row.endsWith("Rp1.400,00"))
    }

    @Test
    fun `discovery keeps distinct products in original order`() {
        val items = listOf(
            OrderItemEntity("1", "o", "a", "Avocado Toast", 1L, 1, null, 0),
            OrderItemEntity("2", "o", "b", "Berry Obsidian", 1L, 3, null, 1),
            OrderItemEntity("3", "o", "a", "Avocado Toast", 1L, 1, null, 2),
            OrderItemEntity("4", "o", "c", "Americano", 1L, 1, null, 3),
        )

        assertEquals(listOf("Avocado Toast", "Berry Obsidian", "Americano"), distinctDiscoveryProducts(items))
        assertEquals("Today's little discoveries:", discoveryHeading(3))
        assertEquals("Today's little discovery:", discoveryHeading(1))
    }

    @Test
    fun `aligned row truncates a long label without losing value`() {
        val row = BluetoothReceiptPrinter.alignedRow(
            label = "An exceptionally long product name that cannot fit",
            value = "Rp28.000,00",
        )

        assertEquals(32, row.length)
        assertTrue(row.contains("~"))
        assertTrue(row.endsWith("Rp28.000,00"))
    }
}
