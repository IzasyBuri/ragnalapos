package com.ragnala.pos.ui.customer

import java.text.NumberFormat
import java.util.Locale

/** Shared currency formatting — integer rupiah (Money.kt rules). */
internal fun formatRupiah(amount: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
