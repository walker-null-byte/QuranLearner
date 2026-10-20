package com.dillu.quranlearner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import java.io.File
import java.io.InputStream

// Where the pre-populated DB may be packed. The second entry is the real Compose
// Multiplatform path; the first is kept for older/other packagings.
private val PREPOPULATED_DB_RESOURCES = listOf(
    "files/pre_populated_quran.db",
    "composeResources/quranlearner.composeapp.generated.resources/files/pre_populated_quran.db",
)

fun main() = application {
    val db = openQuranDb()
    Window(
        onCloseRequest = ::exitApplication,
        title = "QuranLearner",
    ) {
        CompositionLocalProvider(LocalQuranDb provides db) {
            App()
        }
    }
}

internal fun openQuranDb(appDir: File = File(System.getProperty("user.home"), ".quranlearner")): QuranDb {
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    val dbFile = File(appDir, "quran.db")

    ensurePrepopulated(dbFile)

    val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
    return QuranDb(connection)
}

/**
 * Makes sure [dbFile] holds the pre-populated data. A DB left behind by the old
 * broken seeding path is an empty file without the `surahs` table, so it is detected
 * and re-copied from the bundled resource. Failures are logged, never fatal.
 */
private fun ensurePrepopulated(dbFile: File) {
    try {
        if (dbFile.exists() && hasPrepopulatedData(dbFile)) return
        if (dbFile.exists()) {
            dbFile.delete()
        }
        copyPrepopulatedResource(dbFile)
    } catch (t: Throwable) {
        System.err.println("openQuranDb: failed to prepare database: ${t.message}")
    }
}

private fun hasPrepopulatedData(dbFile: File): Boolean {
    return try {
        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        try {
            val stmt = connection.prepare("SELECT COUNT(*) FROM surahs")
            try {
                stmt.step() && stmt.getInt(0) > 0
            } finally {
                stmt.close()
            }
        } finally {
            connection.close()
        }
    } catch (t: Throwable) {
        false
    }
}

private fun copyPrepopulatedResource(dbFile: File) {
    val stream = openPrepopulatedResource()
    if (stream == null) {
        System.err.println("openQuranDb: pre-populated DB resource not found on classpath")
        return
    }
    stream.use { input ->
        dbFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

private fun openPrepopulatedResource(): InputStream? {
    val loader = Thread.currentThread().contextClassLoader
        ?: QuranDb::class.java.classLoader
    for (path in PREPOPULATED_DB_RESOURCES) {
        val stream = loader?.getResourceAsStream(path)
        if (stream != null) return stream
    }
    return null
}
