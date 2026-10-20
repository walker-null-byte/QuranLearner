package com.dillu.quranlearner.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Models
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class Surah(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

data class Ayah(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val textUthmani: String,
    val textIndoPak: String,
    val translationEnglish: String,
    val translationUrdu: String?,
    val juzNumber: Int,
    val pageNumber: Int,
    val verseKey: String
)

data class DailyProgress(
    val date: String,
    val ayahsLearned: Int,
    val goal: Int
)

data class ReviewItem(
    val ayah: Ayah,
    val nextReviewDate: String,
    val interval: Int,
    val easeFactor: Float
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Database
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

class QuranDb(private val conn: SQLiteConnection) {

    private val mutex = Mutex()

    companion object {
        /** Pull cursor: max server timestamp seen (ISO-8601 string). */
        const val SYNC_KEY_LAST_PULLED = "sync.last_pulled_at"

        /** Push watermark: epoch millis; local rows newer than this are pending upload. */
        const val SYNC_KEY_LAST_PUSHED = "sync.last_pushed_ms"

        /** Account the stored cursors belong to; cursors reset when it changes. */
        const val SYNC_KEY_ACCOUNT = "sync.account_id"

        /** Timestamp (epoch millis) of the last successful sync, for display. */
        const val SYNC_KEY_LAST_SUCCESS = "sync.last_success_ms"

        /**
         * Settings that stay device-local and are never synced: sync bookkeeping,
         * per-reciter download flags (files live on this device only) and the
         * export location (a per-device path/URI).
         */
        private val LOCAL_ONLY_SETTING_PREFIXES = listOf("sync.", "downloaded_surahs_")
        private const val LOCAL_ONLY_SETTING_EXACT = "export_location"

        internal fun isLocalOnlySetting(key: String): Boolean =
            LOCAL_ONLY_SETTING_PREFIXES.any { key.startsWith(it) } || key == LOCAL_ONLY_SETTING_EXACT
    }

    init {
        // Ensure these tables exist (the pre-populated DB may not have them)
        exec("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL, updatedAt INTEGER NOT NULL DEFAULT 0)")
        exec("CREATE TABLE IF NOT EXISTS progress (date TEXT PRIMARY KEY, ayahsLearned INTEGER NOT NULL DEFAULT 0, goal INTEGER NOT NULL DEFAULT 5, updatedAt INTEGER NOT NULL DEFAULT 0)")
        exec("CREATE TABLE IF NOT EXISTS ayah_progress (surahNumber INTEGER, ayahNumber INTEGER, learnedAt TEXT, updatedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(surahNumber, ayahNumber))")
        exec("CREATE TABLE IF NOT EXISTS achievements (id TEXT PRIMARY KEY, unlockedAt TEXT, updatedAt INTEGER NOT NULL DEFAULT 0)")
        exec("CREATE TABLE IF NOT EXISTS ayah_reviews (surahNumber INTEGER, ayahNumber INTEGER, nextReviewDate TEXT, interval INTEGER, easeFactor REAL, updatedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(surahNumber, ayahNumber))")
        exec("CREATE TABLE IF NOT EXISTS favorites (surahNumber INTEGER, ayahNumber INTEGER, addedAt TEXT, updatedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(surahNumber, ayahNumber))")
        exec("CREATE TABLE IF NOT EXISTS sync_tombstones (entity TEXT NOT NULL, itemKey TEXT NOT NULL, ts INTEGER NOT NULL, PRIMARY KEY(entity, itemKey))")

        // Databases created before cloud sync existed lack the updatedAt columns.
        // Add them and backfill so pre-existing data gets picked up by the first sync.
        val backfillMs = Clock.System.now().toEpochMilliseconds()
        for (table in listOf("settings", "progress", "ayah_progress", "achievements", "ayah_reviews", "favorites")) {
            val added = runCatching {
                exec("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                true
            }.getOrDefault(false)
            if (added) exec("UPDATE $table SET updatedAt = $backfillMs")
        }
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    // ── Surahs ──

    suspend fun getSurahs(): List<Surah> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT number, name, englishName, englishNameTranslation, numberOfAyahs, revelationType FROM surahs ORDER BY number ASC") { s ->
                buildList {
                    while (s.step()) {
                        add(Surah(
                            number = s.getInt(0),
                            name = s.getText(1),
                            englishName = s.getText(2),
                            englishNameTranslation = s.getText(3),
                            numberOfAyahs = s.getInt(4),
                            revelationType = s.getText(5)
                        ))
                    }
                }
            }
        }
    }

    // ── Ayahs ──

    suspend fun getAyahsForSurah(surahNumber: Int): List<Ayah> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare(
                "SELECT id, surahNumber, ayahNumber, textUthmani, textIndoPak, " +
                "translationEnglish, translationUrdu, juzNumber, pageNumber, verseKey " +
                "FROM ayahs WHERE surahNumber = ? ORDER BY ayahNumber ASC"
            )
            try {
                stmt.bindInt(1, surahNumber)
                buildList {
                    while (stmt.step()) {
                        add(Ayah(
                            id = stmt.getLong(0),
                            surahNumber = stmt.getInt(1),
                            ayahNumber = stmt.getInt(2),
                            textUthmani = stmt.getText(3),
                            textIndoPak = stmt.getText(4),
                            translationEnglish = stmt.getText(5).stripHtml(),
                            translationUrdu = if (stmt.isNull(6)) null else stmt.getText(6).stripHtml(),
                            juzNumber = stmt.getInt(7),
                            pageNumber = stmt.getInt(8),
                            verseKey = stmt.getText(9)
                        ))
                    }
                }
            } finally {
                stmt.close()
            }
        }
    }

    // ── Settings (stored in SQLite — no DataStore needed) ──

    suspend fun getSetting(key: String, default: String = ""): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT value FROM settings WHERE key = ?")
            try {
                stmt.bindText(1, key)
                if (stmt.step()) stmt.getText(0) else default
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun setSetting(key: String, value: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("INSERT OR REPLACE INTO settings (key, value, updatedAt) VALUES (?, ?, ?)")
            try {
                stmt.bindText(1, key)
                stmt.bindText(2, value)
                stmt.bindLong(3, nowMs())
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    // ── Progress ──

    suspend fun getProgress(date: String): DailyProgress? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT date, ayahsLearned, goal FROM progress WHERE date = ?")
            try {
                stmt.bindText(1, date)
                if (stmt.step()) {
                    DailyProgress(stmt.getText(0), stmt.getInt(1), stmt.getInt(2))
                } else null
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun updateProgress(progress: DailyProgress) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("INSERT OR REPLACE INTO progress (date, ayahsLearned, goal, updatedAt) VALUES (?, ?, ?, ?)")
            try {
                stmt.bindText(1, progress.date)
                stmt.bindInt(2, progress.ayahsLearned)
                stmt.bindInt(3, progress.goal)
                stmt.bindLong(4, nowMs())
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getLast30DaysProgress(): List<DailyProgress> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT date, ayahsLearned, goal FROM progress ORDER BY date DESC LIMIT 30") { s ->
                buildList {
                    while (s.step()) {
                        add(DailyProgress(s.getText(0), s.getInt(1), s.getInt(2)))
                    }
                }
            }
        }
    }

    private suspend fun getActivityDates(): Set<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT date FROM progress WHERE ayahsLearned > 0") { s ->
                buildSet {
                    while (s.step()) {
                        add(s.getText(0))
                    }
                }
            }
        }
    }

    suspend fun getStreak(todayDate: LocalDate): Int = computeStreak(getActivityDates(), todayDate)

    // ── Ayah Progress & XP ──

    suspend fun markAyahLearned(surahNumber: Int, ayahNumber: Int, date: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val ts = nowMs()
            exec("BEGIN")
            try {
                val stmt = conn.prepare("INSERT OR IGNORE INTO ayah_progress (surahNumber, ayahNumber, learnedAt, updatedAt) VALUES (?, ?, ?, ?)")
                try {
                    stmt.bindInt(1, surahNumber)
                    stmt.bindInt(2, ayahNumber)
                    stmt.bindText(3, date)
                    stmt.bindLong(4, ts)
                    stmt.step()
                } finally {
                    stmt.close()
                }

                // Add to ayah_reviews (due the same day it was learned)
                val stmt2 = conn.prepare("INSERT OR IGNORE INTO ayah_reviews (surahNumber, ayahNumber, nextReviewDate, interval, easeFactor, updatedAt) VALUES (?, ?, ?, ?, ?, ?)")
                try {
                    stmt2.bindInt(1, surahNumber)
                    stmt2.bindInt(2, ayahNumber)
                    stmt2.bindText(3, date)
                    stmt2.bindInt(4, 1) // 1 day interval
                    stmt2.bindDouble(5, 2.5) // Default ease factor
                    stmt2.bindLong(6, ts)
                    stmt2.step()
                } finally {
                    stmt2.close()
                }

                exec("COMMIT")
            } catch (e: Throwable) {
                runCatching { exec("ROLLBACK") }
                throw e
            }
        }
    }

    /**
     * Ayahs whose next review date is on or before [today], oldest due first (SQL `ORDER BY nextReviewDate ASC`).
     * Not randomized — order is deterministic. Capped at 50 for one session.
     */
    suspend fun getAyahsToReview(today: String): List<ReviewItem> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("""
                SELECT r.surahNumber, r.ayahNumber, r.nextReviewDate, r.interval, r.easeFactor,
                       a.id, a.textUthmani, a.textIndoPak, a.translationEnglish, a.translationUrdu, a.juzNumber, a.pageNumber, a.verseKey
                FROM ayah_reviews r
                JOIN ayahs a ON r.surahNumber = a.surahNumber AND r.ayahNumber = a.ayahNumber
                WHERE r.nextReviewDate <= ?
                ORDER BY r.nextReviewDate ASC
                LIMIT 50
            """.trimIndent())
            try {
                stmt.bindText(1, today)
                buildList {
                    while(stmt.step()) {
                        val sNum = stmt.getInt(0)
                        val aNum = stmt.getInt(1)
                        val nextD = stmt.getText(2)
                        val inv = stmt.getInt(3)
                        val ease = stmt.getDouble(4).toFloat()
                        val aId = stmt.getLong(5)
                        val tUth = stmt.getText(6)
                        val tIndo = stmt.getText(7)
                        val tEng = stmt.getText(8)
                        val tUrd = if (stmt.isNull(9)) null else stmt.getText(9)
                        val juz = stmt.getInt(10)
                        val page = stmt.getInt(11)
                        val vk = stmt.getText(12)
                        
                        val ayah = Ayah(aId, sNum, aNum, tUth, tIndo, tEng, tUrd, juz, page, vk)
                        add(ReviewItem(ayah, nextD, inv, ease))
                    }
                }
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun updateReviewItem(item: ReviewItem) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("UPDATE ayah_reviews SET nextReviewDate = ?, interval = ?, easeFactor = ?, updatedAt = ? WHERE surahNumber = ? AND ayahNumber = ?")
            try {
                stmt.bindText(1, item.nextReviewDate)
                stmt.bindInt(2, item.interval)
                stmt.bindDouble(3, item.easeFactor.toDouble())
                stmt.bindLong(4, nowMs())
                stmt.bindInt(5, item.ayah.surahNumber)
                stmt.bindInt(6, item.ayah.ayahNumber)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun isAyahLearned(surahNumber: Int, ayahNumber: Int): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT 1 FROM ayah_progress WHERE surahNumber = ? AND ayahNumber = ?")
            try {
                stmt.bindInt(1, surahNumber)
                stmt.bindInt(2, ayahNumber)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getLearnedAyahsCount(surahNumber: Int): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT COUNT(*) FROM ayah_progress WHERE surahNumber = ?")
            try {
                stmt.bindInt(1, surahNumber)
                if (stmt.step()) stmt.getInt(0) else 0
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getLearnedAyahsCountInRange(startSurah: Int, endSurah: Int): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT COUNT(*) FROM ayah_progress WHERE surahNumber BETWEEN ? AND ?")
            try {
                stmt.bindInt(1, startSurah)
                stmt.bindInt(2, endSurah)
                if (stmt.step()) stmt.getInt(0) else 0
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getTotalLearnedAyahsCount(): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT COUNT(*) FROM ayah_progress")
            try {
                if (stmt.step()) stmt.getInt(0) else 0
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getLearnedAyahs(surahNumber: Int): Set<Int> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT ayahNumber FROM ayah_progress WHERE surahNumber = ?")
            try {
                stmt.bindInt(1, surahNumber)
                buildSet {
                    while (stmt.step()) {
                        add(stmt.getInt(0))
                    }
                }
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getSurahProgressMap(): Map<Int, Int> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT surahNumber, COUNT(*) FROM ayah_progress GROUP BY surahNumber") { s ->
                buildMap {
                    while (s.step()) {
                        put(s.getInt(0), s.getInt(1))
                    }
                }
            }
        }
    }

    suspend fun getXp(): Int {
        return getSetting("xp", "0").toIntOrNull() ?: 0
    }

    suspend fun addXp(amount: Int) {
        val current = getXp()
        setSetting("xp", (current + amount).toString())
    }

    // ── Achievements ──

    suspend fun unlockAchievement(id: String, date: String): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val checkStmt = conn.prepare("SELECT 1 FROM achievements WHERE id = ?")
            val exists = try {
                checkStmt.bindText(1, id)
                checkStmt.step()
            } finally {
                checkStmt.close()
            }
            if (exists) return@withContext false

            val stmt = conn.prepare("INSERT INTO achievements (id, unlockedAt, updatedAt) VALUES (?, ?, ?)")
            try {
                stmt.bindText(1, id)
                stmt.bindText(2, date)
                stmt.bindLong(3, nowMs())
                stmt.step()
                true
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getUnlockedAchievements(): List<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT id FROM achievements") { s ->
                buildList {
                    while (s.step()) {
                        add(s.getText(0))
                    }
                }
            }
        }
    }

    // ── Favorites ──

    suspend fun toggleFavorite(surahNumber: Int, ayahNumber: Int): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val ts = nowMs()
            val checkStmt = conn.prepare("SELECT 1 FROM favorites WHERE surahNumber = ? AND ayahNumber = ?")
            val exists = try {
                checkStmt.bindInt(1, surahNumber)
                checkStmt.bindInt(2, ayahNumber)
                checkStmt.step()
            } finally {
                checkStmt.close()
            }
            if (exists) {
                val delStmt = conn.prepare("DELETE FROM favorites WHERE surahNumber = ? AND ayahNumber = ?")
                try {
                    delStmt.bindInt(1, surahNumber)
                    delStmt.bindInt(2, ayahNumber)
                    delStmt.step()
                } finally {
                    delStmt.close()
                }
                // Tombstone so the deletion is pushed to other devices on the next sync.
                val tombStmt = conn.prepare("INSERT OR REPLACE INTO sync_tombstones (entity, itemKey, ts) VALUES ('favorites', ?, ?)")
                try {
                    tombStmt.bindText(1, "$surahNumber:$ayahNumber")
                    tombStmt.bindLong(2, ts)
                    tombStmt.step()
                } finally {
                    tombStmt.close()
                }
                false // no longer a favorite
            } else {
                val now = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
                val insStmt = conn.prepare("INSERT INTO favorites (surahNumber, ayahNumber, addedAt, updatedAt) VALUES (?, ?, ?, ?)")
                try {
                    insStmt.bindInt(1, surahNumber)
                    insStmt.bindInt(2, ayahNumber)
                    insStmt.bindText(3, now)
                    insStmt.bindLong(4, ts)
                    insStmt.step()
                } finally {
                    insStmt.close()
                }
                // Re-adding cancels any pending tombstone for this ayah.
                val delTomb = conn.prepare("DELETE FROM sync_tombstones WHERE entity = 'favorites' AND itemKey = ?")
                try {
                    delTomb.bindText(1, "$surahNumber:$ayahNumber")
                    delTomb.step()
                } finally {
                    delTomb.close()
                }
                true // now a favorite
            }
        }
    }

    suspend fun isFavorite(surahNumber: Int, ayahNumber: Int): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT 1 FROM favorites WHERE surahNumber = ? AND ayahNumber = ?")
            try {
                stmt.bindInt(1, surahNumber)
                stmt.bindInt(2, ayahNumber)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getFavoriteAyahNumbers(surahNumber: Int): Set<Int> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT ayahNumber FROM favorites WHERE surahNumber = ?")
            try {
                stmt.bindInt(1, surahNumber)
                buildSet {
                    while (stmt.step()) {
                        add(stmt.getInt(0))
                    }
                }
            } finally {
                stmt.close()
            }
        }
    }

    suspend fun getAllFavoriteAyahs(): List<Ayah> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("""
                SELECT a.id, a.surahNumber, a.ayahNumber, a.textUthmani, a.textIndoPak,
                       a.translationEnglish, a.translationUrdu, a.juzNumber, a.pageNumber, a.verseKey
                FROM favorites f
                JOIN ayahs a ON f.surahNumber = a.surahNumber AND f.ayahNumber = a.ayahNumber
                ORDER BY f.addedAt DESC
            """.trimIndent()) { s ->
                buildList {
                    while (s.step()) {
                        add(Ayah(
                            id = s.getLong(0),
                            surahNumber = s.getInt(1),
                            ayahNumber = s.getInt(2),
                            textUthmani = s.getText(3),
                            textIndoPak = s.getText(4),
                            translationEnglish = s.getText(5).stripHtml(),
                            translationUrdu = if (s.isNull(6)) null else s.getText(6).stripHtml(),
                            juzNumber = s.getInt(7),
                            pageNumber = s.getInt(8),
                            verseKey = s.getText(9)
                        ))
                    }
                }
            }
        }
    }

    suspend fun getFavoriteCount(): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("SELECT COUNT(*) FROM favorites")
            try {
                if (stmt.step()) stmt.getInt(0) else 0
            } finally {
                stmt.close()
            }
        }
    }

    // ── Cloud sync support ──
    //
    // Every mutable row carries an `updatedAt` (epoch millis) stamped at write
    // time. SyncManager exports rows changed after a watermark, and applies
    // server rows that are newer than the local copy (last write wins).
    // Deleting a favorite leaves a tombstone so the deletion reaches the
    // other devices; re-adding cancels it.

    suspend fun getSyncStateString(key: String): String = getSetting(key, "")

    suspend fun setSyncState(key: String, value: String) = setSetting(key, value)

    suspend fun getSyncStateLong(key: String): Long = getSetting(key, "0").toLongOrNull() ?: 0L

    suspend fun setSyncState(key: String, value: Long) = setSetting(key, value.toString())

    // ── Sync export (local -> server) ──

    suspend fun exportSettingsSince(sinceMs: Long): List<SyncSettingRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT key, value, updatedAt FROM settings WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        val key = stmt.getText(0)
                        if (isLocalOnlySetting(key)) continue
                        add(SyncSettingRow(key = key, value = stmt.getText(1), updatedAt = stmt.getLong(2)))
                    }
                }
            }
        }
    }

    suspend fun exportProgressSince(sinceMs: Long): List<SyncProgressRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT date, ayahsLearned, goal, updatedAt FROM progress WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        add(SyncProgressRow(
                            date = stmt.getText(0),
                            ayahsLearned = stmt.getInt(1),
                            goal = stmt.getInt(2),
                            updatedAt = stmt.getLong(3),
                        ))
                    }
                }
            }
        }
    }

    suspend fun exportAyahProgressSince(sinceMs: Long): List<SyncAyahProgressRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT surahNumber, ayahNumber, learnedAt, updatedAt FROM ayah_progress WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        add(SyncAyahProgressRow(
                            surahNumber = stmt.getInt(0),
                            ayahNumber = stmt.getInt(1),
                            learnedAt = if (stmt.isNull(2)) "" else stmt.getText(2),
                            updatedAt = stmt.getLong(3),
                        ))
                    }
                }
            }
        }
    }

    suspend fun exportAchievementsSince(sinceMs: Long): List<SyncAchievementRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT id, unlockedAt, updatedAt FROM achievements WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        add(SyncAchievementRow(
                            id = stmt.getText(0),
                            unlockedAt = if (stmt.isNull(1)) "" else stmt.getText(1),
                            updatedAt = stmt.getLong(2),
                        ))
                    }
                }
            }
        }
    }

    suspend fun exportReviewsSince(sinceMs: Long): List<SyncReviewRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT surahNumber, ayahNumber, nextReviewDate, interval, easeFactor, updatedAt FROM ayah_reviews WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        add(SyncReviewRow(
                            surahNumber = stmt.getInt(0),
                            ayahNumber = stmt.getInt(1),
                            nextReviewDate = if (stmt.isNull(2)) "" else stmt.getText(2),
                            intervalDays = stmt.getInt(3),
                            easeFactor = stmt.getDouble(4),
                            updatedAt = stmt.getLong(5),
                        ))
                    }
                }
            }
        }
    }

    suspend fun exportFavoritesSince(sinceMs: Long): List<SyncFavoriteRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT surahNumber, ayahNumber, addedAt, updatedAt FROM favorites WHERE updatedAt > ?") { stmt ->
                stmt.bindLong(1, sinceMs)
                buildList {
                    while (stmt.step()) {
                        add(SyncFavoriteRow(
                            surahNumber = stmt.getInt(0),
                            ayahNumber = stmt.getInt(1),
                            addedAt = if (stmt.isNull(2)) "" else stmt.getText(2),
                            updatedAt = stmt.getLong(3),
                        ))
                    }
                }
            }
        }
    }

    /** Pending favorite deletions, as tombstone rows for upload. */
    suspend fun exportFavoriteTombstones(): List<SyncFavoriteRow> = mutex.withLock {
        withContext(Dispatchers.IO) {
            query("SELECT itemKey, ts FROM sync_tombstones WHERE entity = 'favorites'") { stmt ->
                buildList {
                    while (stmt.step()) {
                        val parts = stmt.getText(0).split(":")
                        if (parts.size != 2) continue
                        val surah = parts[0].toIntOrNull() ?: continue
                        val ayah = parts[1].toIntOrNull() ?: continue
                        add(SyncFavoriteRow(
                            surahNumber = surah,
                            ayahNumber = ayah,
                            addedAt = "",
                            updatedAt = stmt.getLong(1),
                            deleted = true,
                        ))
                    }
                }
            }
        }
    }

    /** Removes tombstones whose deletion was successfully uploaded (exact version only). */
    suspend fun clearFavoriteTombstones(rows: List<SyncFavoriteRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                // Matching on ts too: if the user deleted the same favorite again while the
                // sync was running, the newer tombstone must stay pending.
                deleteWhere("sync_tombstones", "entity = 'favorites' AND itemKey = ? AND ts = ?") {
                    it.bindText(1, "${row.surahNumber}:${row.ayahNumber}")
                    it.bindLong(2, row.updatedAt)
                }
            }
        }
    }

    // ── Sync apply (server -> local) ──

    suspend fun applySettingsRows(rows: List<SyncSettingRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (isLocalOnlySetting(row.key)) continue
                if (!isServerRowNewer("settings", "key = ?", row.updatedAt) { it.bindText(1, row.key) }) continue
                if (row.deleted) {
                    deleteWhere("settings", "key = ?") { it.bindText(1, row.key) }
                } else {
                    val stmt = conn.prepare("INSERT OR REPLACE INTO settings (key, value, updatedAt) VALUES (?, ?, ?)")
                    try {
                        stmt.bindText(1, row.key)
                        stmt.bindText(2, row.value)
                        stmt.bindLong(3, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    suspend fun applyProgressRows(rows: List<SyncProgressRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (!isServerRowNewer("progress", "date = ?", row.updatedAt) { it.bindText(1, row.date) }) continue
                if (row.deleted) {
                    deleteWhere("progress", "date = ?") { it.bindText(1, row.date) }
                } else {
                    val stmt = conn.prepare("INSERT OR REPLACE INTO progress (date, ayahsLearned, goal, updatedAt) VALUES (?, ?, ?, ?)")
                    try {
                        stmt.bindText(1, row.date)
                        stmt.bindInt(2, row.ayahsLearned)
                        stmt.bindInt(3, row.goal)
                        stmt.bindLong(4, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    suspend fun applyAyahProgressRows(rows: List<SyncAyahProgressRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (!isServerRowNewer("ayah_progress", "surahNumber = ? AND ayahNumber = ?", row.updatedAt) {
                        it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                    }
                ) continue
                if (row.deleted) {
                    deleteWhere("ayah_progress", "surahNumber = ? AND ayahNumber = ?") {
                        it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                    }
                } else {
                    val stmt = conn.prepare("INSERT OR REPLACE INTO ayah_progress (surahNumber, ayahNumber, learnedAt, updatedAt) VALUES (?, ?, ?, ?)")
                    try {
                        stmt.bindInt(1, row.surahNumber)
                        stmt.bindInt(2, row.ayahNumber)
                        stmt.bindText(3, row.learnedAt)
                        stmt.bindLong(4, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    suspend fun applyAchievementRows(rows: List<SyncAchievementRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (!isServerRowNewer("achievements", "id = ?", row.updatedAt) { it.bindText(1, row.id) }) continue
                if (row.deleted) {
                    deleteWhere("achievements", "id = ?") { it.bindText(1, row.id) }
                } else {
                    val stmt = conn.prepare("INSERT OR REPLACE INTO achievements (id, unlockedAt, updatedAt) VALUES (?, ?, ?)")
                    try {
                        stmt.bindText(1, row.id)
                        stmt.bindText(2, row.unlockedAt)
                        stmt.bindLong(3, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    suspend fun applyReviewRows(rows: List<SyncReviewRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (!isServerRowNewer("ayah_reviews", "surahNumber = ? AND ayahNumber = ?", row.updatedAt) {
                        it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                    }
                ) continue
                if (row.deleted) {
                    deleteWhere("ayah_reviews", "surahNumber = ? AND ayahNumber = ?") {
                        it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                    }
                } else {
                    val stmt = conn.prepare("INSERT OR REPLACE INTO ayah_reviews (surahNumber, ayahNumber, nextReviewDate, interval, easeFactor, updatedAt) VALUES (?, ?, ?, ?, ?, ?)")
                    try {
                        stmt.bindInt(1, row.surahNumber)
                        stmt.bindInt(2, row.ayahNumber)
                        stmt.bindText(3, row.nextReviewDate)
                        stmt.bindInt(4, row.intervalDays)
                        stmt.bindDouble(5, row.easeFactor)
                        stmt.bindLong(6, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    suspend fun applyFavoriteRows(rows: List<SyncFavoriteRow>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            for (row in rows) {
                if (row.deleted) {
                    if (!isServerRowNewer("favorites", "surahNumber = ? AND ayahNumber = ?", row.updatedAt) {
                            it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                        }
                    ) continue
                    deleteWhere("favorites", "surahNumber = ? AND ayahNumber = ?") {
                        it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                    }
                } else {
                    // A local tombstone newer than the server row means "deleted here"; keep it deleted.
                    val tombstoneTs = favoriteTombstoneTs(row.surahNumber, row.ayahNumber)
                    if (tombstoneTs >= row.updatedAt) continue
                    if (!isServerRowNewer("favorites", "surahNumber = ? AND ayahNumber = ?", row.updatedAt) {
                            it.bindInt(1, row.surahNumber); it.bindInt(2, row.ayahNumber)
                        }
                    ) continue
                    // Server re-added the favorite after the local deletion: cancel the pending tombstone,
                    // otherwise the deletion would be pushed after (and override) the newer row.
                    if (tombstoneTs >= 0) {
                        deleteWhere("sync_tombstones", "entity = 'favorites' AND itemKey = ?") {
                            it.bindText(1, "${row.surahNumber}:${row.ayahNumber}")
                        }
                    }
                    val stmt = conn.prepare("INSERT OR REPLACE INTO favorites (surahNumber, ayahNumber, addedAt, updatedAt) VALUES (?, ?, ?, ?)")
                    try {
                        stmt.bindInt(1, row.surahNumber)
                        stmt.bindInt(2, row.ayahNumber)
                        stmt.bindText(3, row.addedAt)
                        stmt.bindLong(4, row.updatedAt)
                        stmt.step()
                    } finally {
                        stmt.close()
                    }
                }
            }
        }
    }

    /** True when [table] has no row matching the filter, or its local copy is older than [serverTs]. */
    private fun isServerRowNewer(
        table: String,
        whereClause: String,
        serverTs: Long,
        bind: (SQLiteStatement) -> Unit,
    ): Boolean {
        val stmt = conn.prepare("SELECT updatedAt FROM $table WHERE $whereClause")
        return try {
            bind(stmt)
            !stmt.step() || stmt.getLong(0) < serverTs
        } finally {
            stmt.close()
        }
    }

    private fun deleteWhere(table: String, whereClause: String, bind: (SQLiteStatement) -> Unit) {
        val stmt = conn.prepare("DELETE FROM $table WHERE $whereClause")
        try {
            bind(stmt)
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    private fun favoriteTombstoneTs(surahNumber: Int, ayahNumber: Int): Long {
        val stmt = conn.prepare("SELECT ts FROM sync_tombstones WHERE entity = 'favorites' AND itemKey = ?")
        return try {
            stmt.bindText(1, "$surahNumber:$ayahNumber")
            if (stmt.step()) stmt.getLong(0) else -1L
        } finally {
            stmt.close()
        }
    }

    // ── Helpers ──

    private fun exec(sql: String) {
        val stmt = conn.prepare(sql)
        try { stmt.step() } finally { stmt.close() }
    }

    private inline fun <R> query(sql: String, block: (SQLiteStatement) -> R): R {
        val stmt = conn.prepare(sql)
        try { return block(stmt) } finally { stmt.close() }
    }
}

// Strips HTML tags (like <sup foot_note=195931>1</sup>) from translation text
private val htmlTagRegex = Regex("<[^>]+>")
private fun String.stripHtml(): String = htmlTagRegex.replace(this, "").trim()

/**
 * Consecutive active days counted backwards from [today].
 * A streak stays alive until a full day is missed: if [today] has no activity but yesterday does,
 * counting is anchored at yesterday. If neither today nor yesterday is active the streak is 0.
 */
internal fun computeStreak(activeDates: Set<String>, today: LocalDate): Int {
    if (activeDates.isEmpty()) return 0

    val hasToday = today.toString() in activeDates
    val yesterday = today.minus(1, DateTimeUnit.DAY)
    if (!hasToday && yesterday.toString() !in activeDates) return 0

    var cursor = if (hasToday) today else yesterday
    var streak = 0
    while (cursor.toString() in activeDates) {
        streak++
        cursor = cursor.minus(1, DateTimeUnit.DAY)
    }
    return streak
}
