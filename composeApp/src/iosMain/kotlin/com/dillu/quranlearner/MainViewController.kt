package com.dillu.quranlearner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import platform.Foundation.NSBundle
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

fun MainViewController() = ComposeUIViewController {
    val db = openQuranDb()
    CompositionLocalProvider(LocalQuranDb provides db) {
        App()
    }
}

private fun openQuranDb(): QuranDb {
    val dbPath = NSHomeDirectory() + "/Documents/quran.db"
    val fileManager = NSFileManager.defaultManager

    if (!fileManager.fileExistsAtPath(dbPath)) {
        // Copy from app bundle
        val bundlePath = NSBundle.mainBundle.pathForResource("pre_populated_quran", "db")
        if (bundlePath != null) {
            val data = NSData.dataWithContentsOfFile(bundlePath)
            data?.writeToFile(dbPath, atomically = true)
        }
    }

    val connection = BundledSQLiteDriver().open(dbPath)
    return QuranDb(connection)
}