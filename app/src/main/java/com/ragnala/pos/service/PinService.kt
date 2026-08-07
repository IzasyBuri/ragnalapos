package com.ragnala.pos.service

import com.ragnala.pos.data.db.AuditDao
import com.ragnala.pos.data.db.SettingEntity
import com.ragnala.pos.data.db.SettingsDao
import java.security.MessageDigest
import java.security.spec.KeySpec
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PinRole { BARISTA, OWNER }

/**
 * PIN-based access control (PRD §9 Role Enforcement, decision D15).
 * PINs are never stored in plaintext: PBKDF2WithHmacSHA256 with a random per-PIN salt and
 * 120k iterations (OWASP-recommended for 4-6 digit PINs). Hashes written before the v3
 * upgrade used SHA-256; those are still verifiable via the legacy fallback (audit M6).
 */
class PinService(
    private val settingsDao: SettingsDao,
    private val auditDao: AuditDao,
) {
    companion object {
        private const val KEY_BARISTA = "pin_barista"
        private const val KEY_OWNER = "pin_owner"

        // Audit M6: PBKDF2WithHmacSHA256, 120,000 iterations, 256-bit output (64 hex chars).
        private const val ITERATIONS = 120_000

        fun hash(pin: String, salt: String, iterations: Int = ITERATIONS): String {
            val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), iterations, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val bytes = factory.generateSecret(spec).encoded
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** Legacy pre-v3 format: SHA-256(pin + "::" + salt). */
        private fun legacyHash(pin: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$pin::$salt".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /** Sets/updates a PIN. [currentPin] required when overwriting an existing PIN. */
    suspend fun setPin(role: PinRole, pin: String, currentPin: String?, userLabel: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) { "PIN must be 4-6 digits" }
        val key = keyFor(role)
        val existing = settingsDao.get(key)
        if (existing != null) {
            require(currentPin != null && verify(role, currentPin)) {
                "Current PIN required to change PIN"
            }
        }
        writePin(role, pin, userLabel)
    }

    /** Writes the PIN hash directly (no current-PIN gate). Used after an owner-authorised override. */
    private suspend fun writePin(role: PinRole, pin: String, userLabel: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) { "PIN must be 4-6 digits" }
        val key = keyFor(role)
        val salt = UUID.randomUUID().toString()
        settingsDao.put(SettingEntity("${key}_salt", salt))
        settingsDao.put(SettingEntity("${key}_iter", ITERATIONS.toString()))
        settingsDao.put(SettingEntity(key, hash(pin, salt)))
        auditDao.insert(
            com.ragnala.pos.data.db.AuditEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                action = "PIN_CHANGE",
                entityType = "role",
                entityId = role.name,
                delta = role.name,
                reason = null,
                userLabel = userLabel,
            ),
        )
    }

    suspend fun isSet(role: PinRole): Boolean =
        settingsDao.get(keyFor(role)) != null

    /**
     * Owner override: set a role's PIN using the OWNER PIN as authority.
     * Lets an owner reset a barista PIN without knowing the current barista PIN (PRD §9).
     */
    suspend fun setPinAsOwner(role: PinRole, newPin: String, ownerPin: String, userLabel: String) {
        require(verify(PinRole.OWNER, ownerPin)) { "Owner PIN required" }
        writePin(role, newPin, userLabel)
    }

    suspend fun verify(role: PinRole, pin: String): Boolean {
        val key = keyFor(role)
        val stored = settingsDao.get(key) ?: return false
        val salt = settingsDao.get("${key}_salt") ?: return false
        val iter = settingsDao.get("${key}_iter")?.toIntOrNull()
        val expected = if (iter != null) hash(pin, salt, iter) else legacyHash(pin, salt)
        return constantTimeEquals(expected, stored)
    }

    private fun keyFor(role: PinRole): String = when (role) {
        PinRole.BARISTA -> KEY_BARISTA
        PinRole.OWNER -> KEY_OWNER
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
