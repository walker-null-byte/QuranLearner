package com.dillu.quranlearner.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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

    init {
        // Ensure these tables exist (the pre-populated DB may not have them)
        exec("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        exec("CREATE TABLE IF NOT EXISTS progress (date TEXT PRIMARY KEY, ayahsLearned INTEGER NOT NULL DEFAULT 0, goal INTEGER NOT NULL DEFAULT 5)")
        exec("CREATE TABLE IF NOT EXISTS ayah_progress (surahNumber INTEGER, ayahNumber INTEGER, learnedAt TEXT, PRIMARY KEY(surahNumber, ayahNumber))")
        exec("CREATE TABLE IF NOT EXISTS achievements (id TEXT PRIMARY KEY, unlockedAt TEXT)")
        exec("CREATE TABLE IF NOT EXISTS ayah_reviews (surahNumber INTEGER, ayahNumber INTEGER, nextReviewDate TEXT, interval INTEGER, easeFactor REAL, PRIMARY KEY(surahNumber, ayahNumber))")
        exec("CREATE TABLE IF NOT EXISTS favorites (surahNumber INTEGER, ayahNumber INTEGER, addedAt TEXT, PRIMARY KEY(surahNumber, ayahNumber))")
    }

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
            val stmt = conn.prepare("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")
            try {
                stmt.bindText(1, key)
                stmt.bindText(2, value)
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
            val stmt = conn.prepare("INSERT OR REPLACE INTO progress (date, ayahsLearned, goal) VALUES (?, ?, ?)")
            try {
                stmt.bindText(1, progress.date)
                stmt.bindInt(2, progress.ayahsLearned)
                stmt.bindInt(3, progress.goal)
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

    suspend fun getStreak(todayDate: kotlinx.datetime.LocalDate): Int {
        val progressList = getLast30DaysProgress()
        if (progressList.isEmpty()) return 0

        var streak = 0
        var currentDate = todayDate

        val hasToday = progressList.any { it.date == todayDate.toString() && it.ayahsLearned > 0 }
        val hasYesterday = progressList.any {
            it.date == todayDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY).toString() && it.ayahsLearned > 0
        }

        if (!hasToday && !hasYesterday) return 0
        if (!hasToday) currentDate = todayDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY)

        while (true) {
            val dateStr = currentDate.toString()
            val progress = progressList.find { it.date == dateStr }
            if (progress != null && progress.ayahsLearned > 0) {
                streak++
                currentDate = currentDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
            } else break
        }
        return streak
    }

    // ── Ayah Progress & XP ──

    suspend fun markAyahLearned(surahNumber: Int, ayahNumber: Int, date: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val stmt = conn.prepare("INSERT OR IGNORE INTO ayah_progress (surahNumber, ayahNumber, learnedAt) VALUES (?, ?, ?)")
            try {
                stmt.bindInt(1, surahNumber)
                stmt.bindInt(2, ayahNumber)
                stmt.bindText(3, date)
                stmt.step()
            } finally {
                stmt.close()
            }
            
            // Add to ayah_reviews
            val nextReview = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
            val stmt2 = conn.prepare("INSERT OR IGNORE INTO ayah_reviews (surahNumber, ayahNumber, nextReviewDate, interval, easeFactor) VALUES (?, ?, ?, ?, ?)")
            try {
                stmt2.bindInt(1, surahNumber)
                stmt2.bindInt(2, ayahNumber)
                stmt2.bindText(3, nextReview)
                stmt2.bindInt(4, 1) // 1 day interval
                stmt2.bindDouble(5, 2.5) // Default ease factor
                stmt2.step()
            } finally {
                stmt2.close()
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
            val stmt = conn.prepare("UPDATE ayah_reviews SET nextReviewDate = ?, interval = ?, easeFactor = ? WHERE surahNumber = ? AND ayahNumber = ?")
            try {
                stmt.bindText(1, item.nextReviewDate)
                stmt.bindInt(2, item.interval)
                stmt.bindDouble(3, item.easeFactor.toDouble())
                stmt.bindInt(4, item.ayah.surahNumber)
                stmt.bindInt(5, item.ayah.ayahNumber)
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

            val stmt = conn.prepare("INSERT INTO achievements (id, unlockedAt) VALUES (?, ?)")
            try {
                stmt.bindText(1, id)
                stmt.bindText(2, date)
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
                false // no longer a favorite
            } else {
                val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
                val insStmt = conn.prepare("INSERT INTO favorites (surahNumber, ayahNumber, addedAt) VALUES (?, ?, ?)")
                try {
                    insStmt.bindInt(1, surahNumber)
                    insStmt.bindInt(2, ayahNumber)
                    insStmt.bindText(3, now)
                    insStmt.step()
                } finally {
                    insStmt.close()
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
