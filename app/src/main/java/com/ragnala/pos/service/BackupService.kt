package com.ragnala.pos.service

import android.content.Context
import com.ragnala.pos.data.db.RagnalaDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup unit = single ZIP archive: SQLite DB snapshot + images directory
 * (PRD §9 Backup, decision D9). Snapshot via VACUUM INTO — never a raw copy of a
 * live DB (WAL makes raw copies unsafe). Restore validates schema version and is
 * destructive: confirmation happens in the UI layer before calling in.
 */
class BackupService(
    private val context: Context,
    private val db: RagnalaDatabase,
) {
    companion object {
        private const val DB_NAME = "ragnala.db"
        private const val IMAGES_DIR = "images"
        private const val SCHEMA_FILE = "schema_version.txt"
        private const val FILENAME_PATTERN = "ragnala_backup_%s.zip"
        private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }

    private val dbFile: File get() = context.getDatabasePath(DB_NAME)
    private val imagesDir: File get() = File(context.filesDir, IMAGES_DIR)
    private val backupDir: File get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")

    fun lastBackup(): File? = backupDir.listFiles { f -> f.extension == "zip" }
        ?.maxByOrNull { it.lastModified() }

    /** All backup archives, newest first. */
    fun listBackups(): List<File> = (backupDir.listFiles { f -> f.extension == "zip" } ?: emptyArray())
        .sortedByDescending { it.lastModified() }

    /** Creates a consistent snapshot archive. Returns the created file. */
    suspend fun createBackup(): File {
        val dbSnapshot = File.createTempFile("ragnala_snapshot", ".db", context.cacheDir)
        val schemaVersion = db.openHelper.readableDatabase.version

        try {
            // VACUUM INTO writes a consistent snapshot even with WAL active.
            // The statement executes only when the cursor is consumed — close() alone
            // is not enough (SQLite defers until first step).
            db.openHelper.writableDatabase.query(
                "VACUUM INTO ?",
                arrayOf(dbSnapshot.absolutePath),
            ).use { cursor ->
                cursor.moveToFirst()
            }

            backupDir.mkdirs()
            val archive = File(backupDir, FILENAME_PATTERN.format(LOCAL_NOW()))
            ZipOutputStream(FileOutputStream(archive).buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(DB_NAME))
                dbSnapshot.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(SCHEMA_FILE))
                zip.write(schemaVersion.toString().toByteArray())
                zip.closeEntry()

                if (imagesDir.exists()) {
                    imagesDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relative = file.relativeTo(imagesDir).path.replace('\\', '/')
                            zip.putNextEntry(ZipEntry("$IMAGES_DIR/$relative"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            return archive
        } finally {
            dbSnapshot.delete()
        }
    }

    /**
     * Restores from an archive. Destructive — the caller must have confirmed.
     * Validates schema: newer backups are rejected, older ones migrated.
     */
    suspend fun restore(archive: File) {
        require(archive.exists()) { "Backup archive not found: ${archive.name}" }

        // 1. Read + validate schema version before touching anything
        val schemaVersion = readSchemaVersion(archive)
        val currentVersion = db.openHelper.readableDatabase.version
        require(schemaVersion <= currentVersion) {
            "Backup schema v$schemaVersion is newer than app schema v$currentVersion"
        }

        // 2. Snapshot current state so the restore is recoverable (PRD §9)
        val safetyNet = createBackup()

        // 3. Extract to temp, then swap in
        val tempDb = File(context.cacheDir, "restore_${System.currentTimeMillis()}.db")
        val tempImages = File(context.cacheDir, "restore_images_${System.currentTimeMillis()}")

        try {
            extract(archive, tempDb, tempImages)
            db.close()

            // swap DB — atomic replace; delete-based renameTo fails under locks on Windows
            val newDbFile = context.getDatabasePath(DB_NAME)
            newDbFile.parentFile?.mkdirs()
            java.nio.file.Files.move(
                tempDb.toPath(),
                newDbFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            )
            // wipe WAL/SHM leftovers of the old connection
            listOf("$newDbFile-wal", "$newDbFile-shm").forEach { File(it).delete() }

            // swap images
            if (imagesDir.exists()) imagesDir.deleteRecursively()
            if (tempImages.exists()) tempImages.renameTo(imagesDir)

            // 4. Re-open + migrate older schema if needed
            db.openHelper.writableDatabase // triggers Room open + migration
        } catch (e: Exception) {
            // roll back to safety net if possible
            val restored = tryRestoreSafetyNet(safetyNet)
            throw IllegalStateException("Restore failed${if (restored) " (previous state restored)" else ""}: ${e.message}", e)
        }
    }

    private fun readSchemaVersion(archive: File): Int {
        ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == SCHEMA_FILE) {
                    return zip.bufferedReader().readText().trim().toIntOrNull()
                        ?: error("Invalid backup: missing schema version")
                }
                entry = zip.nextEntry
            }
        }
        error("Invalid backup archive: no $SCHEMA_FILE entry")
    }

    private fun extract(archive: File, targetDb: File, targetImages: File) {
        ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == DB_NAME -> zip.copyToFile(targetDb)
                    name.startsWith("$IMAGES_DIR/") -> {
                        val relative = name.removePrefix("$IMAGES_DIR/")
                        // Audit M11: zip-slip guard — never write outside the images dir.
                        val out = resolveInside(targetImages, relative)
                        out.parentFile?.mkdirs()
                        zip.copyToFile(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /** Rejects malicious entry names (absolute paths, `..`, symlink escapes) — zip-slip (M11). */
    private fun resolveInside(base: File, raw: String): File {
        val normalized = raw.replace('\\', '/').trimStart('/')
        require(normalized.isNotEmpty()) { "Empty image path in backup archive" }
        require(normalized != ".." && !normalized.startsWith("../")) { "Unsafe image path in backup: '$raw'" }

        val baseCanonical = base.canonicalFile
        val target = File(base, normalized).canonicalFile
        require(target.toPath().startsWith(baseCanonical.toPath())) { "Zip-slip blocked: '$raw'" }
        return target
    }

    private suspend fun tryRestoreSafetyNet(safetyNet: File?): Boolean {
        if (safetyNet == null || !safetyNet.exists()) return false
        return try {
            restore(safetyNet)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun ZipInputStream.copyToFile(target: File) {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out -> copyTo(out) }
    }

    private fun LOCAL_NOW(): String = TIMESTAMP.format(LocalDateTime.now())
}
