package com.ragnala.pos.ui.receipt

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
