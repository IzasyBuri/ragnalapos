package com.ragnala.pos.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.data.db.PaymentEntity
import com.ragnala.pos.ui.customer.formatRupiah
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Offline receipt printing via the system PrintManager (PDF / save-to-file / any
 * installed print service). Fully local — no network, fits offline-first.
 *
 * Vintage "little slow bar" receipt: black ink on white, dotted price leaders,
 * small botanical motif dividers, discovery box with corner art, cozy footer line.
 * Monochrome — thermal-printer safe.
 */
fun printReceipt(
    context: Context,
    order: OrderEntity,
    items: List<OrderItemEntity>,
    payments: List<PaymentEntity>,
    storeName: String,
    logo: Bitmap? = null,
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

    val layout = buildReceiptLayout(order, items, payments, storeName)

    printManager.print(
        "Receipt #${order.orderNumber}",
        object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("receipt_${order.orderNumber}").apply {
                    setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                }.build()
                callback.onLayoutFinished(info, oldAttributes != newAttributes)
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback,
            ) {
                try {
                    val document = PdfDocument()
                    // A6 page (105x148mm @ 300dpi) — the spooler prints it as a tall receipt-like sheet.
                    val widthPx = (105f / 25.4f * 300f).toInt()
                    val heightPx = (148f / 25.4f * 300f).toInt()

                    val page = document.startPage(
                        PdfDocument.PageInfo.Builder(widthPx, heightPx, 0).create()
                    )
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    val contentW = widthPx.toFloat() - 120f
                    val left = (widthPx - contentW) / 2f
                    layout.drawReceipt(canvas, left, 60f, contentW, logo)
                    document.finishPage(page)

                    document.writeTo(FileOutputStream(destination.fileDescriptor))
                    document.close()
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        },
        PrintAttributes.Builder().apply {
            // A6 (105x148mm) is the closest standard size the print spooler accepts;
            // it renders as a tall receipt-like page instead of letterboxing on A4.
            setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        }.build(),
    )
}

/**
 * Builds the receipt's logical layout: line items + decorations.
 */
internal fun buildReceiptLayout(
    order: OrderEntity,
    items: List<OrderItemEntity>,
    payments: List<PaymentEntity>,
    storeName: String,
): ReceiptLayout {
    return ReceiptLayout(
        storeName = storeName,
        orderNumber = order.orderNumber,
        createdAt = order.createdAt,
        customerName = order.customerName,
        items = items,
        subtotal = order.subtotal,
        serviceCharge = order.serviceCharge,
        tax = order.tax,
        total = order.total,
        payments = payments,
    )
}

/** Draws the vintage botanical receipt on a single page. */
internal class ReceiptLayout(
    private val storeName: String,
    private val orderNumber: Long,
    private val createdAt: Long,
    private val customerName: String?,
    private val items: List<OrderItemEntity>,
    private val subtotal: Long,
    private val serviceCharge: Long,
    private val tax: Long,
    private val total: Long,
    private val payments: List<PaymentEntity>,
) {
    // Generous thermal-print sizing for a ~1000px-wide content column.
    private val baseSize = 34f
    private val baseStep = 54f

    private fun textPaint(italic: Boolean = false, fakeBold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = baseSize
            this.isFakeBoldText = fakeBold
            if (italic) typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            else if (fakeBold) typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val inkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    fun drawReceipt(canvas: Canvas, left: Float, top: Float, w: Float, logo: Bitmap? = null) {
        // Scale text to the column width so the receipt fills the sheet nicely.
        val scale = w / 1000f
        val s = baseSize * scale
        val step = baseStep * scale
        val x = left
        val right = left + w

        fun paint(italic: Boolean = false, bold: Boolean = false): Paint =
            textPaint(italic, bold).apply { textSize = s }

        var y = top

        // ---- Logo (if set) ----
        logo?.let {
            val logoW = w * 0.4f
            val logoH = it.height * (logoW / it.width)
            val lx = x + (w - logoW) / 2f
            canvas.drawBitmap(
                it, null,
                RectF(lx, y, lx + logoW, y + logoH),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            y += logoH + step * 0.6f
        }

        // ---- Store name + tagline (compact header) ----
        val storePaint = paint(bold = true).apply { textSize = s * 1.3f }
        centerText(canvas, storeName, x, right, y, storePaint)
        y += step * 0.95f
        val tagPaint = paint(italic = true).apply { textSize = s * 0.85f }
        centerText(canvas, "little slow bar", x, right, y, tagPaint)
        y += step * 0.8f

        // ---- Header: receipt number, date, customer ----
        val datePaint = paint()
        centerText(canvas, "Receipt #$orderNumber", x, right, y, datePaint)
        y += step * 0.9f
        val ts = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(createdAt))
        centerText(canvas, ts, x, right, y, datePaint)
        y += step * 0.9f
        customerName?.takeIf { it.isNotBlank() }?.let {
            centerText(canvas, "Customer: $it", x, right, y, datePaint)
            y += step
        }

        // ---- Motif divider ----
        drawMotifDivider(canvas, x, right, y, scale, small = true)
        y += step * 0.9f

        // ---- Title: TODAY'S HARVEST with flanking leaf marks ----
        val titlePaint = paint(bold = true).apply { textSize = s * 1.15f }
        val title = "TODAY'S HARVEST"
        val tw = titlePaint.measureText(title)
        val tx = x + (w - tw) / 2f
        canvas.drawText(title, tx, y, titlePaint)
        // flanking leaves
        drawSprout(canvas, tx - 34f * scale, y - 14f * scale, 16f * scale, 1f)
        drawSprout(canvas, tx + tw + 34f * scale, y - 14f * scale, 16f * scale, -1f)
        y += step * 1.25f

        // ---- Clear two-column item table ----
        val columnHeader = paint(bold = true).apply { textSize = s * 0.9f }
        columnRow(canvas, "Item", "Price", x, right, y, columnHeader, columnHeader)
        y += step * 0.85f
        drawThinRule(canvas, x, right, y, scale)
        y += step * 0.8f

        val namePaint = paint()
        val pricePaint = paint()
        items.forEach { item ->
            val label = "${item.quantity}x ${item.productName}"
            val price = formatRupiah(item.unitPrice * item.quantity)
            columnRow(canvas, label, price, x, right, y, namePaint, pricePaint)
            y += step * 1.05f
            item.note?.takeIf { it.isNotBlank() }?.let {
                val notePaint = paint(italic = true).apply { textSize = s * 0.82f }
                canvas.drawText(it, x + 20f * scale, y, notePaint)
                y += step
            }
        }

        // ---- Motif divider ----
        y += step * 0.3f
        drawMotifDivider(canvas, x, right, y, scale, small = true)
        y += step * 0.9f

        // ---- Totals: roomy two-column rows ----
        fun totalRow(label: String, amount: Long, bold: Boolean = false) {
            val lp = paint(bold = bold)
            val ap = paint(bold = bold)
            columnRow(canvas, label, formatRupiah(amount), x, right, y, lp, ap)
            y += step * 1.05f
        }
        totalRow("Subtotal", subtotal)
        totalRow("Service charge", serviceCharge)
        totalRow("Tax", tax)

        // ---- Total: strong hierarchy with breathing room ----
        y += step * 0.15f
        drawThinRule(canvas, x, right, y, scale)
        y += step * 0.9f
        val totalPaint = paint(bold = true).apply { textSize = s * 1.25f }
        columnRow(canvas, "TOTAL", formatRupiah(total), x, right, y, totalPaint, totalPaint)
        y += step * 0.95f
        drawThinRule(canvas, x, right, y, scale)
        y += step * 0.9f

        // ---- Payment rows ----
        payments.forEach { p ->
            if (p.method == "CASH") {
                totalRow("Cash", p.tendered ?: p.amount)
                p.changeGiven?.let { totalRow("Change", it) }
            } else {
                totalRow(p.method, p.amount)
            }
        }

        // ---- Discovery box: intentionally simple for thermal-paper clarity ----
        y += step * 0.5f
        val boxH = 104f * scale
        val box = RectF(x + 20f * scale, y, right - 20f * scale, y + boxH)
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
            pathEffect = DashPathEffect(floatArrayOf(10f * scale, 8f * scale), 0f)
        }
        canvas.drawRoundRect(box, 16f * scale, 16f * scale, dash)

        val boxPaint = paint(italic = true).apply {
            textSize = s * 0.9f
            textAlign = Paint.Align.CENTER
        }
        val discovery = "Today's little discovery: ${items.firstOrNull()?.productName ?: "coffee"}"
        val metrics = boxPaint.fontMetrics
        val centeredBaseline = box.centerY() - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(discovery, box.centerX(), centeredBaseline, boxPaint)
        y = box.bottom + step * 0.8f

        // ---- Footer ----
        val footPaint = paint(italic = true).apply { textSize = s * 0.92f }
        centerText(canvas, "Thank you for slowing down with us.", x, right, y, footPaint)
        y += step * 0.95f
        drawDotDivider(canvas, x, right, y, scale)
        y += step * 0.95f
        val closingPaint = paint(italic = true).apply { textSize = s * 0.92f }
        centerText(canvas, "Take a breath • Sip slowly • Grow gently", x, right, y, closingPaint)
    }

    // ---- Drawing helpers ----

    private fun centerText(c: Canvas, text: String, left: Float, right: Float, y: Float, p: Paint) {
        val tx = left + (right - left - p.measureText(text)) / 2f
        c.drawText(text, tx, y, p)
    }

    /** Clear two-column row: label left, amount right; no leader clutter. */
    private fun columnRow(
        c: Canvas,
        label: String,
        value: String,
        left: Float,
        right: Float,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint,
    ) {
        c.drawText(label, left, y, labelPaint)
        c.drawText(value, right - valuePaint.measureText(value), y, valuePaint)
    }

    private fun drawThinRule(c: Canvas, left: Float, right: Float, y: Float, scale: Float) {
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2.2f * scale
            style = Paint.Style.STROKE
        }
        c.drawLine(left, y, right, y, rule)
    }

    private fun dottedLine(c: Canvas, left: Float, y: Float, w: Float, scale: Float) {
        val r = 2.2f * scale
        var dx = left
        while (dx < left + w) {
            c.drawCircle(dx, y - 3f * scale, r, dot)
            dx += 10f * scale
        }
    }

    /** Small motif divider: leaf + dot + flower + dot + leaf. */
    private fun drawMotifDivider(c: Canvas, left: Float, right: Float, y: Float, scale: Float, small: Boolean) {
        val cx = left + (right - left) / 2f
        val gap = 30f * scale
        drawLeaf(c, cx - gap * 2f, y - 8f * scale, 12f * scale, 1f)
        c.drawCircle(cx - gap, y - 6f * scale, 3f * scale, dot)
        drawFlower(c, cx, y - 8f * scale, 11f * scale)
        c.drawCircle(cx + gap, y - 6f * scale, 3f * scale, dot)
        drawLeaf(c, cx + gap * 2f, y - 8f * scale, 12f * scale, -1f)
    }

    private fun drawDotDivider(c: Canvas, left: Float, right: Float, y: Float, scale: Float) {
        var dx = left + 30f * scale
        while (dx < right - 30f * scale) {
            c.drawCircle(dx, y - 3f * scale, 2.5f * scale, dot)
            dx += 14f * scale
        }
    }

    private fun drawLeaf(c: Canvas, x: Float, y: Float, size: Float, dir: Float) {
        val p = Path()
        p.moveTo(x, y)
        p.quadTo(x + size * dir, y - size * 0.6f, x + size * dir * 2f, y)
        p.quadTo(x + size * dir, y + size * 0.6f, x, y)
        c.drawPath(p, ink)
    }

    /** Small sprout: stem + two leaves. */
    private fun drawSprout(c: Canvas, x: Float, y: Float, size: Float, dir: Float) {
        val stem = Path()
        stem.moveTo(x, y + size * 0.8f)
        stem.lineTo(x, y - size * 0.2f)
        c.drawPath(stem, ink)
        drawLeaf(c, x, y - size * 0.2f, size * 0.55f, dir)
        drawLeaf(c, x, y - size * 0.2f, size * 0.55f, -dir)
    }

    /** Simple 5-petal flower (outline petals + solid center). */
    private fun drawFlower(c: Canvas, cx: Float, cy: Float, r: Float) {
        for (i in 0 until 5) {
            val a = Math.toRadians((i * 72).toDouble())
            val px = cx + (r * 0.6f * cos(a)).toFloat()
            val py = cy + (r * 0.6f * sin(a)).toFloat()
            c.drawCircle(px, py, r * 0.42f, ink)
        }
        c.drawCircle(cx, cy, r * 0.3f, inkFill)
    }

    /** Small mushroom: outlined cap + stem, dots. */
    private fun drawMushroom(c: Canvas, cx: Float, cy: Float, r: Float) {
        val stem = Path()
        stem.moveTo(cx - r * 0.25f, cy)
        stem.lineTo(cx - r * 0.25f, cy + r * 0.6f)
        stem.lineTo(cx + r * 0.25f, cy + r * 0.6f)
        stem.lineTo(cx + r * 0.25f, cy)
        c.drawPath(stem, ink)
        val cap = Path()
        cap.moveTo(cx - r, cy + r * 0.15f)
        cap.quadTo(cx - r, cy - r * 0.8f, cx, cy - r * 0.8f)
        cap.quadTo(cx + r, cy - r * 0.8f, cx + r, cy + r * 0.15f)
        cap.close()
        c.drawPath(cap, ink)
        c.drawCircle(cx - r * 0.4f, cy - r * 0.35f, r * 0.1f, inkFill)
        c.drawCircle(cx + r * 0.15f, cy - r * 0.5f, r * 0.1f, inkFill)
        c.drawCircle(cx + r * 0.45f, cy - r * 0.2f, r * 0.1f, inkFill)
    }
}
