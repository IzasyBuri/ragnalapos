package com.ragnala.pos.service

import com.ragnala.pos.data.db.SettingEntity
import com.ragnala.pos.data.db.SettingsDao
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Typed access to the settings table (PRD §9 Settings).
 * Values are cached in memory; writes hit the DB immediately.
 * All money percentages are stored as decimal percent (e.g. 11.0 = 11%).
 */
class SettingsService(private val dao: SettingsDao) {

    private val cache = MutableStateFlow<Map<String, String>>(emptyMap())

    suspend fun loadAll() {
        // settings table is small; simplest is a full refresh per load
        cache.value = dao.getAll()
    }

    private suspend fun getString(key: String, default: String): String =
        cache.value[key] ?: dao.get(key) ?: default.also { put(key, it) }

    suspend fun getLong(key: String, default: Long): Long =
        getString(key, default.toString()).toLongOrNull() ?: default

    suspend fun getDouble(key: String, default: Double): Double =
        getString(key, default.toString()).toDoubleOrNull() ?: default

    suspend fun put(key: String, value: String) {
        dao.put(SettingEntity(key, value))
        cache.value = cache.value + (key to value)
    }

    suspend fun serviceChargePercent(): Double = getDouble("service_charge_percent", 5.0)
    suspend fun taxPercent(): Double = getDouble("tax_percent", 11.0)

    suspend fun setServiceChargePercent(value: Double) = put("service_charge_percent", value.toString())
    suspend fun setTaxPercent(value: Double) = put("tax_percent", value.toString())

    suspend fun storeName(): String = getString("store_name", "Ragnala Coffee & Botanee")
    suspend fun setStoreName(value: String) = put("store_name", value)

    suspend fun receiptHeader(): String = getString("receipt_header", "")
    suspend fun setReceiptHeader(value: String) = put("receipt_header", value)
    suspend fun receiptFooter(): String = getString("receipt_footer", "")
    suspend fun setReceiptFooter(value: String) = put("receipt_footer", value)

    /** Absolute file path of the receipt logo PNG (empty = none). */
    suspend fun logoPath(): String = getString("receipt_logo_path", "")
    suspend fun setLogoPath(value: String) = put("receipt_logo_path", value)

    /** Absolute file path of the QRIS payment image (empty = none). */
    suspend fun qrisImagePath(): String = getString("qris_image_path", "")
    suspend fun setQrisImagePath(value: String) = put("qris_image_path", value)

    suspend fun idleTimeoutMinutes(): Long = getLong("idle_timeout_minutes", 10)
    suspend fun recoveryWindowMinutes(): Long = getLong("recovery_window_minutes", 15)
    suspend fun baristaLockMinutes(): Long = getLong("barista_lock_minutes", 5)

    /**
     * Daily "disable Barista PIN" flag (PRD §9 owner convenience).
     * Stores today's date (yyyy-MM-dd) when disabled; empty = PIN required.
     * Expires automatically next day because the stored date no longer matches.
     */
    suspend fun baristaPinDisabledDate(): String = getString("barista_pin_disabled_date", "")
    suspend fun setBaristaPinDisabledDate(date: String) = put("barista_pin_disabled_date", date)

    /** Clears the daily disable flag (re-enables the PIN). */
    suspend fun clearBaristaPinDisabled() {
        dao.deleteKey("barista_pin_disabled_date")
        cache.value = cache.value - "barista_pin_disabled_date"
    }
}
