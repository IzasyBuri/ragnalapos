package com.ragnala.pos.domain

import java.math.BigDecimal

/** One priced line in an order: product unit price + modifier deltas, times quantity. */
data class LineItem(
    val unitPrice: Long,
    val quantity: Int,
)

data class OrderTotals(
    val subtotal: Long,
    val serviceCharge: Long,
    val tax: Long,
    val total: Long,
)

/**
 * PRD §9 Settings: subtotal -> service charge on subtotal -> tax on (subtotal + SC).
 * Rounding: half-up, applied once at the total. No intermediate rounding.
 * [serviceCharge] and [tax] returned rounded for receipt display only; the total is
 * computed from the unrounded fractions per PRD.
 */
object Pricing {

    fun calculate(
        items: List<LineItem>,
        serviceChargePercent: Double,
        taxPercent: Double,
    ): OrderTotals {
        val subtotal = items.sumOf { Money.lineTotal(it.unitPrice, it.quantity) }

        val scPercent = BigDecimal.valueOf(serviceChargePercent)
        val taxPercentDec = BigDecimal.valueOf(taxPercent)
        val subtotalDec = BigDecimal.valueOf(subtotal.toLong())

        val scRaw = subtotalDec.multiply(scPercent).divide(BigDecimal(100))
        val taxRaw = subtotalDec.add(scRaw).multiply(taxPercentDec).divide(BigDecimal(100))

        val total = Money.roundHalfUp(subtotalDec.add(scRaw).add(taxRaw))
        val sc = Money.roundHalfUp(scRaw)
        val tax = Money.roundHalfUp(taxRaw)

        return OrderTotals(subtotal = subtotal, serviceCharge = sc, tax = tax, total = total)
    }

    /** Change due for a cash payment. Throws if tendered is less than the total. */
    fun changeDue(total: Long, tendered: Long): Long {
        require(tendered >= total) { "Tendered amount is less than the total" }
        return tendered - total
    }
}
