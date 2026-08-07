package com.ragnala.pos.ui.customer

import java.text.NumberFormat
import java.util.Locale

/** Shared currency formatting — integer rupiah (Money.kt rules). */
internal fun formatRupiah(amount: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

/** Formats a quantity, trimming trailing zeros (e.g. 330.0 -> "330", 0.5 -> "0.5"). */
internal fun trimAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
