package com.dillu.quranlearner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.cloud.Cloud
import com.dillu.quranlearner.db.QuranDb
import io.github.jan.supabase.auth.handleDeeplinks
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = openQuranDb()

        // Also creates the Supabase client (when configured) so an OAuth
        // deeplink arriving on a cold start is handled right away.
        handleAuthDeeplink(intent)

        setContent {
            CompositionLocalProvider(LocalQuranDb provides db) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeeplink(intent)
    }

    /** Completes the browser-based Google sign-in fallback (quranlearner://login-callback). */
    private fun handleAuthDeeplink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == Cloud.OAUTH_SCHEME && data.host == Cloud.OAUTH_HOST) {
            Cloud.client?.handleDeeplinks(intent)
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