package com.ragnala.pos.ui.customer

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

/** Shared currency formatting — integer rupiah (Money.kt rules). */
internal fun formatRupiah(amount: Long): String {
    val absolute = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.absoluteValue)
    return if (amount < 0) "-Rp$absolute" else "Rp$absolute"
}

/** Formats a quantity, trimming trailing zeros (e.g. 330.0 -> "330", 0.5 -> "0.5"). */
internal fun trimAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
