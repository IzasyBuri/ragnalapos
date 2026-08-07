package com.ragnala.pos.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.ragnala.pos.data.db.RagnalaDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RagnalaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // create a v1 database
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS orders (" +
                    "id TEXT NOT NULL PRIMARY KEY, orderNumber INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, customerName TEXT, subtotal INTEGER NOT NULL, " +
                    "serviceCharge INTEGER NOT NULL, tax INTEGER NOT NULL, total INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, completedAt INTEGER" +
                    ")",
            )
            db.execSQL(
                "INSERT INTO orders (id, orderNumber, status, subtotal, serviceCharge, tax, total, createdAt, updatedAt) " +
                    "VALUES ('o1', 1, 'PAID', 10000, 500, 1100, 11600, 1000, 1000)",
            )
        }

        // run migration 1 -> 2
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, RagnalaDatabase.MIGRATION_1_2)
        migrated.query("SELECT cogs FROM orders WHERE id = 'o1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0)) // legacy rows: cogs unknown -> null
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
