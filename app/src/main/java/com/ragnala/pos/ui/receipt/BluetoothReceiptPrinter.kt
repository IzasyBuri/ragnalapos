package com.ragnala.pos.ui.receipt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.data.db.PaymentEntity
import com.ragnala.pos.ui.customer.formatRupiah
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ESC/POS Bluetooth thermal receipt printing over SPP (RFCOMM).
 * Works with standard 58mm/80mm thermal printers (e.g. most mobile Bluetooth receipt printers).
 * Fully offline — no network needed.
 *
 * Audit M10: text is written as UTF-8 (not US_ASCII) so customer/product names with non-ASCII
 * characters (e.g. Indonesian diacritics) print correctly on modern thermal printers.
 */
object BluetoothReceiptPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** Paired Bluetooth devices (printer candidates). */
    fun pairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Prints [order] to the printer at [device].
     * @throws IOException if connect/write fails.
     */
    @Throws(IOException::class)
    fun print(
        device: BluetoothDevice,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        payments: List<PaymentEntity>,
        storeName: String,
        logo: Bitmap? = null,
    ) {
        val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        try {
            socket.connect()
            val out: OutputStream = socket.outputStream

            // ESC/POS initialization
            out.write(byteArrayOf(0x1B, 0x40)) // ESC @

            // Logo as raster image (if any), centered.
            logo?.let { writeImage(out, it) }

            // Header — centered, bold, with generous spacing
            writeCenteredBold(out, storeName)
            writeCentered(out, "little slow bar")
            writeBlank(out)
            writeCentered(out, "Receipt #${order.orderNumber}")
            val ts = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(order.createdAt))
            writeCentered(out, ts)
            order.customerName?.takeIf { it.isNotBlank() }?.let { writeCentered(out, "Customer: $it") }

            writeBlank(out)
            writeDivider(out, '=')
            writeCenteredBold(out, "TODAY'S HARVEST")
            writeDivider(out, '=')

            // Items — clear two-column table
            writeBoldLine(out, alignedRow("Item", "Price"))
            writeDivider(out, '-')
            items.forEach { line ->
                writeLine(out, alignedRow("${line.quantity}x ${line.productName}", formatRupiah(line.unitPrice * line.quantity)))
                line.note?.takeIf { it.isNotBlank() }?.let { writeLine(out, "  $it") }
            }

            writeBlank(out)
            writeDivider(out, '-')

            // Totals — aligned, with strong total hierarchy
            writeLine(out, alignedRow("Subtotal", formatRupiah(order.subtotal)))
            writeLine(out, alignedRow("Service charge", formatRupiah(order.serviceCharge)))
            writeLine(out, alignedRow("Tax", formatRupiah(order.tax)))
            writeDivider(out, '-')
            writeBoldLine(out, alignedRow("TOTAL", formatRupiah(order.total)))
            writeDivider(out, '-')
            writeBlank(out)

            // Payment
            payments.forEach { p ->
                if (p.method == "CASH") {
                    writeLine(out, alignedRow("Cash", formatRupiah(p.tendered ?: p.amount)))
                    p.changeGiven?.let { writeLine(out, alignedRow("Change", formatRupiah(it))) }
                } else {
                    writeLine(out, alignedRow(p.method, formatRupiah(p.amount)))
                }
            }

            writeBlank(out)
            writeDivider(out, '.')
            val discoveryProducts = distinctDiscoveryProducts(items)
            writeCentered(out, discoveryHeading(discoveryProducts.size))
            discoveryProducts.ifEmpty { listOf("Coffee") }.forEach { productName ->
                writeCenteredBold(out, productName)
            }
            writeDivider(out, '.')
            writeBlank(out)
            writeCentered(out, "Take a breath - Sip slowly - Grow gently")

            // Feed + cut
            out.write(byteArrayOf(0x1B, 0x64, 0x04)) // ESC d 4 (feed 4 lines)
            out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (partial cut)
            out.flush()
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    // ---- ESC/POS helpers ----

    private fun writeLine(out: OutputStream, text: String) {
        out.write((text + "\n").toByteArray(Charsets.UTF_8))
    }

    private fun writeBoldLine(out: OutputStream, text: String) {
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // ESC E 1 (bold on)
        out.write((text + "\n").toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // bold off
    }

    private fun writeCentered(out: OutputStream, text: String) {
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1 (center)
        out.write((text + "\n").toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x61, 0x00)) // left
    }

    private fun writeCenteredBold(out: OutputStream, text: String) {
        out.write(byteArrayOf(0x1B, 0x61, 0x01, 0x1B, 0x45, 0x01)) // center + bold
        out.write((text + "\n").toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x00, 0x1B, 0x61, 0x00)) // reset
    }

    private fun writeBlank(out: OutputStream) {
        out.write('\n'.code)
    }

    /** Fits a label/value pair into a 32-character 58mm-safe line. */
    internal fun alignedRow(label: String, value: String, columns: Int = 32): String {
        val safeValue = if (value.length >= columns) value.takeLast(columns - 1) else value
        val maxLabel = (columns - safeValue.length - 1).coerceAtLeast(1)
        val safeLabel = if (label.length > maxLabel) label.take(maxLabel - 1) + "~" else label
        val spaces = (columns - safeLabel.length - safeValue.length).coerceAtLeast(1)
        return safeLabel + " ".repeat(spaces) + safeValue
    }

    private fun writeDivider(out: OutputStream, char: Char = '-') {
        out.write((char.toString().repeat(32) + "\n").toByteArray(Charsets.UTF_8))
    }

    /** Prints a bitmap as a 1-bit raster image (ESC *), scaled to 48mm-wide 58mm printers. */
    private fun writeImage(out: OutputStream, bitmap: Bitmap) {
        // Scale to fit ~380px wide (58mm at 8 dots/mm)
        val targetWidth = 380
        val scale = targetWidth.toFloat() / bitmap.width
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, (bitmap.height * scale).toInt(), true)

        val widthPx = scaled.width
        val heightPx = scaled.height
        val bytesPerLine = (widthPx + 7) / 8

        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00) // GS v 0 (raster mode)
        val xL = (widthPx % 256).toByte()
        val xH = (widthPx / 256).toByte()
        val yL = (heightPx % 256).toByte()
        val yH = (heightPx / 256).toByte()

        out.write(header)
        out.write(byteArrayOf(xL, xH, yL, yH))

        val lineData = ByteArray(bytesPerLine)
        for (y in 0 until heightPx) {
            lineData.fill(0)
            for (x in 0 until widthPx) {
                val pixel = scaled.getPixel(x, y)
                // luminance threshold — dark pixels print
                val lum = (0.299 * ((pixel shr 16) and 0xFF) +
                        0.587 * ((pixel shr 8) and 0xFF) +
                        0.114 * (pixel and 0xFF))
                if (lum < 128) {
                    lineData[x / 8] = (lineData[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
            out.write(lineData)
        }
    }
}
