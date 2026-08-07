package com.ragnala.pos.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All money is integer rupiah (Long). Floating-point math for money is forbidden
 * anywhere in the codebase (PRD §15). Fractions from tax/service-charge math are
 * carried in BigDecimal and rounded exactly once, half-up, at the total.
 */
object Money {
    fun roundHalfUp(amount: BigDecimal): Long =
        amount.setScale(0, RoundingMode.HALF_UP).longValueExact()

    /**
     * Exact per-unit cost from a pack price + pack size, carried in BigDecimal
     * (PRD §15: no floats for money). Not rounded here; round once at the total.
     */
    fun unitCost(purchasePrice: Long, packSize: Double): BigDecimal =
        BigDecimal.valueOf(purchasePrice)
            .divide(BigDecimal.valueOf(packSize), 8, RoundingMode.HALF_UP)

    /** Exact integer product — no rounding, no floats. */
    fun lineTotal(unitPrice: Long, quantity: Int): Long = unitPrice * quantity
}
