package com.dillu.quranlearner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = openQuranDb()

        setContent {
            CompositionLocalProvider(LocalQuranDb provides db) {
                App()
            }
        }
    }

    private fun openQuranDb(): QuranDb {
        // Copy the pre-populated DB from assets if it doesn't exist yet
        val dbFile = File(filesDir, "quran.db")
        if (!dbFile.exists()) {
            assets.open("pre_populated_quran.db").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        return QuranDb(connection)
    }
}