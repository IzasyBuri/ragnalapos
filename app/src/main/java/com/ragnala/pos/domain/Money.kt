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

    /** Exact integer product — no rounding, no floats. */
    fun lineTotal(unitPrice: Long, quantity: Int): Long = unitPrice * quantity
}
