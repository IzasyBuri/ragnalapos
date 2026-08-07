package com.ragnala.pos.domain

/**
 * Inventory rules (PRD §9 Inventory, decision D10):
 * negative stock is allowed with a low-stock warning; auto-block is deferred to v2.
 * Low = current stock at or below minimum.
 */
object InventoryRules {

    data class Result(val newStock: Double, val isLow: Boolean)

    fun deduct(currentStock: Double, quantityUsed: Double, minStock: Double): Result {
        val newStock = currentStock - quantityUsed
        return Result(newStock = newStock, isLow = newStock <= minStock)
    }
}
