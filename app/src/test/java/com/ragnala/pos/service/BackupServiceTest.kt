package com.ragnala.pos.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.data.db.RagnalaDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class BackupServiceTest {

    private lateinit var context: Context
    private lateinit var db: RagnalaDatabase
    private lateinit var service: BackupService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // fresh DB file per test
        context.deleteDatabase("ragnala.db")
        db = Room.databaseBuilder(context, RagnalaDatabase::class.java, "ragnala.db")
            .allowMainThreadQueries()
            .build()
        service = BackupService(context, db)
    }

    @After
    fun teardown() {
        db.close()
        context.deleteDatabase("ragnala.db")
    }

    @Test
    fun `backup creates zip with db and schema version`() = runTest {
        db.categoryDao().upsert(
            CategoryEntity(
                id = "c1", name = "Coffee", position = 1,
                createdAt = 1000L, updatedAt = 1000L,
            ),
        )
        db.ingredientDao().upsert(
            IngredientEntity(
                id = "i1", name = "Espresso", unit = "shot", currentStock = 50.0,
                minStock = 10.0, costPerUnit = 3000, createdAt = 1000L, updatedAt = 1000L,
            ),
        )

        val backup = service.createBackup()
        assertTrue(backup.exists())
        assertTrue(backup.name.startsWith("ragnala_backup_"))
        assertTrue(backup.name.endsWith(".zip"))

        // verify contents
        val entries = mutableSetOf<String>()
        java.util.zip.ZipInputStream(backup.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertTrue("db entry missing", entries.contains("ragnala.db"))
        assertTrue("schema entry missing", entries.contains("schema_version.txt"))
    }

    @Test
    fun `restore round trip preserves data`() = runTest {
        db.categoryDao().upsert(
            CategoryEntity(
                id = "c1", name = "Coffee", position = 1,
                createdAt = 1000L, updatedAt = 1000L,
            ),
        )
        val backup = service.createBackup()

        // mutate the live DB
        db.categoryDao().upsert(
            CategoryEntity(
                id = "c2", name = "Pastry", position = 2,
                createdAt = 2000L, updatedAt = 2000L,
            ),
        )
        assertNotNull(db.categoryDao().byId("c2")) // c2 exists pre-restore

        service.restore(backup)

        // restore closed + re-opened the DB internally; fetch a fresh handle for assertions
        val freshDb = Room.databaseBuilder(context, RagnalaDatabase::class.java, "ragnala.db")
            .allowMainThreadQueries()
            .build()

        // c2 should be gone (snapshot was taken before c2 existed)
        val restored = freshDb.categoryDao().byId("c1")
        assertNotNull("c1 must survive restore", restored)
        assertEquals("Coffee", restored!!.name)
        freshDb.close()
    }

    @Test
    fun `restore rejects newer schema`() = runTest {
        val backup = service.createBackup()
        // fake a newer schema version in the archive
        val entries = java.util.zip.ZipInputStream(backup.inputStream().buffered()).use { zip ->
            val map = mutableMapOf<String, ByteArray>()
            var entry = zip.nextEntry
            while (entry != null) {
                map[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
            map
        }
        val forged = File(context.cacheDir, "forged_backup.zip")
        java.util.zip.ZipOutputStream(forged.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                val content = if (name == "schema_version.txt") "999".toByteArray() else bytes
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }

        var threw = false
        try {
            service.restore(forged)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("Expected IllegalArgumentException for newer schema", threw)
        forged.delete()
    }

    @Test
    fun `restore rejects zip-slip image entry`() = runTest {
        val malicious = File(context.cacheDir, "evil_backup.zip")
        java.util.zip.ZipOutputStream(malicious.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("schema_version.txt"))
            zip.write("0".toByteArray()) // 0 <= current version, passes schema check
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("images/../../../evil.png")) // traversal
            zip.write("x".toByteArray())
            zip.closeEntry()
        }
        val escaped = File(context.cacheDir, "evil.png")

        var threw = false
        try {
            service.restore(malicious)
        } catch (_: Exception) {
            threw = true
        }

        assertTrue("Restore must reject a zip-slip archive", threw)
        assertTrue("No file may be written outside the images dir", !escaped.exists())
        malicious.delete()
    }

    @Test
    fun `lastBackup finds most recent archive`() = runTest {
        service.createBackup()
        val last = service.lastBackup()
        assertNotNull(last)
        assertTrue(last!!.exists())
    }
}
