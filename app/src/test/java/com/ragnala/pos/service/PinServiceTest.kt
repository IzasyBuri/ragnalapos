package com.ragnala.pos.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ragnala.pos.data.db.RagnalaDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinServiceTest {

    private lateinit var db: RagnalaDatabase
    private lateinit var service: PinService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RagnalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = PinService(db.settingsDao(), db.auditDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `pin not set initially`() = runTest {
        assertFalse(service.isSet(PinRole.BARISTA))
        assertFalse(service.isSet(PinRole.OWNER))
    }

    @Test
    fun `set and verify pin`() = runTest {
        service.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "owner")
        assertTrue(service.isSet(PinRole.BARISTA))
        assertTrue(service.verify(PinRole.BARISTA, "1234"))
        assertFalse(service.verify(PinRole.BARISTA, "9999"))
    }

    @Test
    fun `wrong pin rejected`() = runTest {
        service.setPin(PinRole.OWNER, "654321", currentPin = null, userLabel = "owner")
        assertFalse(service.verify(PinRole.OWNER, "65432"))
        assertFalse(service.verify(PinRole.OWNER, "65432 1"))
    }

    @Test
    fun `change requires current pin`() = runTest {
        service.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "owner")

        var threw = false
        try {
            service.setPin(PinRole.BARISTA, "5678", currentPin = "0000", userLabel = "owner")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("Expected IllegalArgumentException for wrong current PIN", threw)
        assertTrue(service.verify(PinRole.BARISTA, "1234"))

        service.setPin(PinRole.BARISTA, "5678", currentPin = "1234", userLabel = "owner")
        assertTrue(service.verify(PinRole.BARISTA, "5678"))
        assertFalse(service.verify(PinRole.BARISTA, "1234"))
    }

    @Test
    fun `invalid pin format rejected`() = runTest {
        for (bad in listOf("12a4", "123", "1234567")) {
            var threw = false
            try {
                service.setPin(PinRole.BARISTA, bad, currentPin = null, userLabel = "owner")
            } catch (e: IllegalArgumentException) {
                threw = true
            }
            assertTrue("Expected IllegalArgumentException for pin '$bad'", threw)
        }
        assertFalse(service.isSet(PinRole.BARISTA))
    }

    @Test
    fun `owner can override a barista pin without current barista pin`() = runTest {
        service.setPin(PinRole.OWNER, "9999", currentPin = null, userLabel = "owner")
        service.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "owner")

        // owner changes barista pin using owner authority only (no barista current pin)
        service.setPinAsOwner(PinRole.BARISTA, "5678", ownerPin = "9999", userLabel = "owner")
        assertTrue(service.verify(PinRole.BARISTA, "5678"))
        assertFalse(service.verify(PinRole.BARISTA, "1234"))

        // wrong owner pin must be rejected
        var threw = false
        try {
            service.setPinAsOwner(PinRole.BARISTA, "1111", ownerPin = "0000", userLabel = "owner")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("Expected rejection for wrong owner PIN", threw)
    }

    @Test
    fun `pin not stored in plaintext`() = runTest {
        service.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "owner")
        val stored = db.settingsDao().get("pin_barista")!!
        assertFalse(stored.contains("1234"))
        assertTrue(stored.length == 64) // pbkdf2-sha256 256-bit hex
        assertNotNull(db.settingsDao().get("pin_barista_salt"))
        assertNotNull(db.settingsDao().get("pin_barista_iter"))
    }

    @Test
    fun `stored hash is not the legacy sha256 of pin and salt`() = runTest {
        service.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "owner")
        val stored = db.settingsDao().get("pin_barista")!!
        val salt = db.settingsDao().get("pin_barista_salt")!!
        // pre-v3 format was SHA-256(pin + "::" + salt) — must no longer match (audit M6)
        val legacy = java.security.MessageDigest.getInstance("SHA-256")
            .digest("1234::$salt".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertFalse("PBKDF2 hash must differ from legacy SHA-256", stored == legacy)
    }

    @Test
    fun `legacy sha256 pin from pre-v3 install still verifies`() = runTest {
        // simulate a PIN written before the v3 PBKDF2 upgrade (no _iter setting)
        val salt = "old-salt"
        val legacy = java.security.MessageDigest.getInstance("SHA-256")
            .digest("1234::$salt".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        db.settingsDao().put(com.ragnala.pos.data.db.SettingEntity("pin_barista_salt", salt))
        db.settingsDao().put(com.ragnala.pos.data.db.SettingEntity("pin_barista", legacy))

        assertTrue(service.verify(PinRole.BARISTA, "1234"))
        assertFalse(service.verify(PinRole.BARISTA, "9999"))
    }
}
