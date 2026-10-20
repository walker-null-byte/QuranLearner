package com.dillu.quranlearner

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.db.SyncAchievementRow
import com.dillu.quranlearner.db.SyncFavoriteRow
import com.dillu.quranlearner.db.SyncProgressRow
import com.dillu.quranlearner.db.SyncReviewRow
import com.dillu.quranlearner.db.SyncSettingRow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncLayerTest {

    private fun withDb(block: suspend (QuranDb) -> Unit) = runBlocking {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            block(QuranDb(connection))
        } finally {
            connection.close()
        }
    }

    // ── Timestamp stamping ──

    @Test
    fun localMutationsAreTimestamped() = withDb { db ->
        db.setSetting("reciter_id", "alafasy")
        db.updateProgress(DailyProgress("2026-10-18", 3, 5))
        db.markAyahLearned(1, 1, "2026-10-18")
        db.unlockAchievement("first_ayah", "2026-10-18")
        db.toggleFavorite(1, 1)

        assertTrue(db.exportSettingsSince(0).first { it.key == "reciter_id" }.updatedAt > 0)
        assertTrue(db.exportProgressSince(0).first().updatedAt > 0)
        assertTrue(db.exportAyahProgressSince(0).first().updatedAt > 0)
        assertTrue(db.exportAchievementsSince(0).first().updatedAt > 0)
        assertTrue(db.exportFavoritesSince(0).first().updatedAt > 0)
    }

    @Test
    fun exportRespectsWatermark() = withDb { db ->
        db.setSetting("a", "1")
        db.setSetting("b", "2")
        val all = db.exportSettingsSince(0)
        assertEquals(setOf("a", "b"), all.map { it.key }.toSet())

        val watermark = all.maxOf { it.updatedAt }
        assertEquals(emptyList(), db.exportSettingsSince(watermark))

        db.setSetting("c", "3")
        assertEquals(listOf("c"), db.exportSettingsSince(watermark).map { it.key })
    }

    @Test
    fun deviceOnlySettingsAreExcluded() = withDb { db ->
        db.setSetting("downloaded_surahs_1", "true")
        db.setSetting("export_location", "/home/x")
        db.setSetting(QuranDb.SYNC_KEY_LAST_PUSHED, "42")
        db.setSetting("reciter_id", "alafasy")

        assertEquals(listOf("reciter_id"), db.exportSettingsSince(0).map { it.key })

        // …and server rows for those keys must not be applied either.
        db.applySettingsRows(
            listOf(
                SyncSettingRow(key = "downloaded_surahs_1", value = "false", updatedAt = Long.MAX_VALUE),
                SyncSettingRow(key = "export_location", value = "/other", updatedAt = Long.MAX_VALUE),
                SyncSettingRow(key = "sync.foo", value = "x", updatedAt = Long.MAX_VALUE),
            )
        )
        assertEquals("true", db.getSetting("downloaded_surahs_1"))
        assertEquals("/home/x", db.getSetting("export_location"))
        assertEquals("", db.getSetting("sync.foo"))
    }

    @Test
    fun oldDatabasesGetUpdatedAtBackfilled() = runBlocking {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            // Simulate a database created before cloud sync existed.
            exec(connection, "CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            exec(connection, "INSERT INTO settings (key, value) VALUES ('reciter_id', 'alafasy')")
            exec(connection, "CREATE TABLE favorites (surahNumber INTEGER, ayahNumber INTEGER, addedAt TEXT, PRIMARY KEY(surahNumber, ayahNumber))")
            exec(connection, "INSERT INTO favorites (surahNumber, ayahNumber, addedAt) VALUES (2, 255, '2026-01-01')")

            val db = QuranDb(connection)

            val setting = db.exportSettingsSince(0).first { it.key == "reciter_id" }
            assertTrue(setting.updatedAt > 0)
            assertEquals("alafasy", setting.value)
            assertEquals(1, db.exportFavoritesSince(0).size)
        } finally {
            connection.close()
        }
    }

    private fun exec(connection: SQLiteConnection, sql: String) {
        val stmt = connection.prepare(sql)
        try {
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    // ── Favorites: tombstones ──

    @Test
    fun unfavoriteLeavesTombstoneThatReaddCancels() = withDb { db ->
        assertTrue(db.toggleFavorite(2, 255))
        assertTrue(db.isFavorite(2, 255))
        assertEquals(0, db.exportFavoriteTombstones().size)

        assertFalse(db.toggleFavorite(2, 255))
        assertFalse(db.isFavorite(2, 255))
        assertEquals(0, db.exportFavoritesSince(0).size)
        val tombstone = db.exportFavoriteTombstones().single()
        assertEquals(2, tombstone.surahNumber)
        assertEquals(255, tombstone.ayahNumber)
        assertTrue(tombstone.deleted)
        assertTrue(tombstone.updatedAt > 0)

        // Re-adding the favorite cancels the pending deletion.
        assertTrue(db.toggleFavorite(2, 255))
        assertEquals(0, db.exportFavoriteTombstones().size)

        // …and clearing uploaded tombstones empties the table.
        assertFalse(db.toggleFavorite(2, 255))
        val pending = db.exportFavoriteTombstones()
        assertEquals(1, pending.size)
        db.clearFavoriteTombstones(pending)
        assertEquals(0, db.exportFavoriteTombstones().size)
    }

    // ── Last-write-wins apply ──

    @Test
    fun newerServerSettingWinsOlderIsIgnored() = withDb { db ->
        db.setSetting("quran_script", "uthmani")
        val localTs = db.exportSettingsSince(0).first { it.key == "quran_script" }.updatedAt

        db.applySettingsRows(listOf(SyncSettingRow(key = "quran_script", value = "indopak", updatedAt = localTs - 1)))
        assertEquals("uthmani", db.getSetting("quran_script"))

        db.applySettingsRows(listOf(SyncSettingRow(key = "quran_script", value = "indopak", updatedAt = localTs + 1)))
        assertEquals("indopak", db.getSetting("quran_script"))
    }

    @Test
    fun serverProgressAchievementsAndReviewsApply() = withDb { db ->
        db.applyProgressRows(listOf(SyncProgressRow(date = "2026-10-18", ayahsLearned = 7, goal = 10, updatedAt = 5_000)))
        assertEquals(DailyProgress("2026-10-18", 7, 10), db.getProgress("2026-10-18"))
        // Older server copy must not overwrite it.
        db.applyProgressRows(listOf(SyncProgressRow(date = "2026-10-18", ayahsLearned = 1, goal = 1, updatedAt = 4_000)))
        assertEquals(DailyProgress("2026-10-18", 7, 10), db.getProgress("2026-10-18"))

        db.applyAchievementRows(listOf(SyncAchievementRow(id = "streak_7", unlockedAt = "2026-10-18", updatedAt = 5_000)))
        assertTrue("streak_7" in db.getUnlockedAchievements())

        db.applyReviewRows(
            listOf(SyncReviewRow(surahNumber = 1, ayahNumber = 1, nextReviewDate = "2030-01-01", intervalDays = 3, easeFactor = 2.7, updatedAt = 5_000))
        )
        var review = db.exportReviewsSince(0).single()
        assertEquals("2030-01-01", review.nextReviewDate)
        assertEquals(3, review.intervalDays)
        assertEquals(2.7, review.easeFactor)

        db.applyReviewRows(
            listOf(SyncReviewRow(surahNumber = 1, ayahNumber = 1, nextReviewDate = "1999-01-01", intervalDays = 1, easeFactor = 1.3, updatedAt = 4_000))
        )
        review = db.exportReviewsSince(0).single()
        assertEquals("2030-01-01", review.nextReviewDate)
    }

    @Test
    fun serverFavoriteDeletionWinsUnlessLocalDeleteIsNewer() = withDb { db ->
        // Server deletion is newer than the local favorite -> remove it.
        db.toggleFavorite(3, 7)
        val localTs = db.exportFavoritesSince(0).single().updatedAt
        db.applyFavoriteRows(listOf(SyncFavoriteRow(surahNumber = 3, ayahNumber = 7, addedAt = "", updatedAt = localTs + 1, deleted = true)))
        assertFalse(db.isFavorite(3, 7))

        // Local deletion (tombstone) newer than a stale server row -> stays deleted.
        db.toggleFavorite(3, 7)
        db.toggleFavorite(3, 7) // deletes again, leaving a fresh tombstone
        val tombstoneTs = db.exportFavoriteTombstones().single().updatedAt
        db.applyFavoriteRows(
            listOf(SyncFavoriteRow(surahNumber = 3, ayahNumber = 7, addedAt = "2026-01-01", updatedAt = tombstoneTs - 1))
        )
        assertFalse(db.isFavorite(3, 7))

        // Server re-add newer than the tombstone -> favorite is back and the tombstone is cancelled.
        db.applyFavoriteRows(
            listOf(SyncFavoriteRow(surahNumber = 3, ayahNumber = 7, addedAt = "2026-10-18", updatedAt = tombstoneTs + 1))
        )
        assertTrue(db.isFavorite(3, 7))
        assertEquals(0, db.exportFavoriteTombstones().size)
    }

    // ── Sync bookkeeping state ──

    @Test
    fun syncStateRoundTrips() = withDb { db ->
        assertEquals("", db.getSyncStateString(QuranDb.SYNC_KEY_LAST_PULLED))
        assertEquals(0L, db.getSyncStateLong(QuranDb.SYNC_KEY_LAST_PUSHED))

        db.setSyncState(QuranDb.SYNC_KEY_LAST_PULLED, "2026-10-18T10:00:00Z")
        db.setSyncState(QuranDb.SYNC_KEY_LAST_PUSHED, 123456789L)
        db.setSyncState(QuranDb.SYNC_KEY_ACCOUNT, "user-1")

        assertEquals("2026-10-18T10:00:00Z", db.getSyncStateString(QuranDb.SYNC_KEY_LAST_PULLED))
        assertEquals(123456789L, db.getSyncStateLong(QuranDb.SYNC_KEY_LAST_PUSHED))
        assertEquals("user-1", db.getSyncStateString(QuranDb.SYNC_KEY_ACCOUNT))
    }
}
