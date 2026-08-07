package com.ragnala.pos.data.db

import androidx.room.TypeConverter
import com.ragnala.pos.domain.OrderStatus

class Converters {
    @TypeConverter
    fun orderStatusToString(status: OrderStatus): String = status.name

    @TypeConverter
    fun stringToOrderStatus(value: String): OrderStatus = when (value) {
        // Legacy statuses folded into the simplified binary lifecycle (PRD §9).
        "PREPARING", "READY", "COMPLETED" -> OrderStatus.FULFILLED
        else -> OrderStatus.valueOf(value)
    }
}
