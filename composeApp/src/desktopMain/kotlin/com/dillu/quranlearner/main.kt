package com.dillu.quranlearner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import java.io.File

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

private fun openQuranDb(): QuranDb {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".quranlearner")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    val dbFile = File(appDir, "quran.db")

    if (!dbFile.exists()) {
        // Copy from resources
        val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("files/pre_populated_quran.db")
        if (resourceStream != null) {
            resourceStream.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
    return QuranDb(connection)
}
