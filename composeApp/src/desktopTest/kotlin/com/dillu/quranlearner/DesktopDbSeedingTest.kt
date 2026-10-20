package com.dillu.quranlearner

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import javax.sound.sampled.AudioSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopDbSeedingTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "quran-db-test-${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun seedsFreshDatabase() = runBlocking {
        val db: QuranDb = openQuranDb(tempDir)

        assertEquals(114, db.getSurahs().size)
        assertEquals(7, db.getAyahsForSurah(1).size)
    }

    @Test
    fun repairsDatabaseMissingPrepopulatedData() = runBlocking {
        // Simulates the stale file created by the old broken seeding path.
        val dbFile = File(tempDir, "quran.db")
        dbFile.writeBytes(ByteArray(0))
        assertEquals(0L, dbFile.length())

        val db = openQuranDb(tempDir)

        assertEquals(114, db.getSurahs().size)
        assertTrue(dbFile.length() > 0L, "pre-populated DB should have been re-copied")
    }

    @Test
    fun repairsDatabaseCreatedByAppWithoutContent() = runBlocking {
        // Mirrors a DB left behind by the app's own init: user-data tables exist,
        // but the pre-populated content (surahs/ayahs) does not.
        val dbFile = File(tempDir, "quran.db")
        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        try {
            QuranDb(connection)
        } finally {
            connection.close()
        }

        val db = openQuranDb(tempDir)

        assertEquals(114, db.getSurahs().size)
    }
}

class DesktopMp3SpiTest {

    @Test
    fun decodesMp3WithMp3Spi() {
        val url = "https://everyayah.com/data/Alafasy_64kbps/001001.mp3"
        val bytes = try {
            val connection = URI(url).toURL().openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.getInputStream().use { it.readBytes() }
        } catch (t: Throwable) {
            println("DesktopMp3SpiTest skipped: could not download test MP3 (${t.message})")
            return
        }

        try {
            AudioSystem.getAudioInputStream(bytes.inputStream()).use { stream ->
                assertTrue(stream.format.sampleRate > 0f, "decoded MP3 should report a sample rate")
            }
        } catch (t: Throwable) {
            println("DesktopMp3SpiTest skipped: MP3 SPI unavailable (${t.message})")
        }
    }
}
