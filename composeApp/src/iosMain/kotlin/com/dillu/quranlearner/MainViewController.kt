package com.dillu.quranlearner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLog
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

fun MainViewController() = ComposeUIViewController {
    val db = openQuranDb()
    CompositionLocalProvider(LocalQuranDb provides db) {
        App()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun openQuranDb(): QuranDb {
    val dbPath = NSHomeDirectory() + "/Documents/quran.db"
    val fileManager = NSFileManager.defaultManager

    try {
        if (!fileManager.fileExistsAtPath(dbPath) || !hasPrepopulatedData(dbPath)) {
            if (fileManager.fileExistsAtPath(dbPath)) {
                fileManager.removeItemAtPath(dbPath, error = null)
            }
            val bundlePath = findBundledDatabase()
            if (bundlePath != null) {
                NSData.dataWithContentsOfFile(bundlePath)?.writeToFile(dbPath, atomically = true)
            } else {
                NSLog("openQuranDb: pre-populated DB not found in app bundle")
            }
        }
    } catch (t: Throwable) {
        NSLog("openQuranDb: failed to prepare database: ${t.message}")
    }

    val connection = BundledSQLiteDriver().open(dbPath)
    return QuranDb(connection)
}

/** First bundle location that actually contains the pre-populated DB, or null. */
private fun findBundledDatabase(): String? {
    val bundle = NSBundle.mainBundle
    val candidates = listOfNotNull(
        bundle.pathForResource("pre_populated_quran", "db"),
        bundle.pathForResource("pre_populated_quran", "db", "compose-resources/files"),
        bundle.pathForResource("pre_populated_quran", "db", "compose-resources"),
        bundle.resourcePath?.let { "$it/compose-resources/files/pre_populated_quran.db" },
    )
    val fileManager = NSFileManager.defaultManager
    return candidates.firstOrNull { fileManager.fileExistsAtPath(it) }
}

private fun hasPrepopulatedData(dbPath: String): Boolean {
    return try {
        val connection = BundledSQLiteDriver().open(dbPath)
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
