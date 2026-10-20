package com.dillu.quranlearner

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressionDbTest {

    // Derived from java.time to avoid kotlinx.datetime.Clock, which is unavailable at
    // runtime in desktopTest (compile 0.6.1 vs runtime 0.7.1 classpath mismatch).
    private fun today(): LocalDate {
        val now = java.time.LocalDate.now()
        return LocalDate(now.year, now.monthValue, now.dayOfMonth)
    }

    @Test
    fun streakCountsBeyondThirtyDays() = runBlocking {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val db = QuranDb(connection)
            val today = today()
            for (day in 0 until 100) {
                val date = today.minus(day, DateTimeUnit.DAY).toString()
                db.updateProgress(DailyProgress(date, 1, 5))
            }
            assertEquals(100, db.getStreak(today))
        } finally {
            connection.close()
        }
    }

    @Test
    fun markingSameAyahTwiceKeepsOneReview() = runBlocking {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val db = QuranDb(connection)
            seedAyah(connection, surah = 1, ayah = 1)
            val today = today().toString()

            db.markAyahLearned(1, 1, today)
            db.markAyahLearned(1, 1, today)

            assertEquals(1, db.getLearnedAyahsCount(1))
            assertEquals(1, db.getAyahsToReview(today).size)
        } finally {
            connection.close()
        }
    }

    @Test
    fun markingTwoAyahsKeepsTwoReviews() = runBlocking {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val db = QuranDb(connection)
            seedAyah(connection, surah = 1, ayah = 1)
            seedAyah(connection, surah = 1, ayah = 2)
            val today = today().toString()

            db.markAyahLearned(1, 1, today)
            db.markAyahLearned(1, 2, today)

            assertEquals(2, db.getLearnedAyahsCount(1))
            assertEquals(2, db.getAyahsToReview(today).size)
        } finally {
            connection.close()
        }
    }

    // The bundled driver starts empty; create a minimal ayahs table so review joins resolve.
    private fun seedAyah(connection: SQLiteConnection, surah: Int, ayah: Int) {
        val create = connection.prepare(
            "CREATE TABLE IF NOT EXISTS ayahs (" +
            "id INTEGER PRIMARY KEY, surahNumber INTEGER, ayahNumber INTEGER, " +
            "textUthmani TEXT, textIndoPak TEXT, translationEnglish TEXT, translationUrdu TEXT, " +
            "juzNumber INTEGER, pageNumber INTEGER, verseKey TEXT)"
        )
        try { create.step() } finally { create.close() }

        val insert = connection.prepare(
            "INSERT INTO ayahs (id, surahNumber, ayahNumber, textUthmani, textIndoPak, " +
            "translationEnglish, translationUrdu, juzNumber, pageNumber, verseKey) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        try {
            insert.bindLong(1, (surah * 1000 + ayah).toLong())
            insert.bindInt(2, surah)
            insert.bindInt(3, ayah)
            insert.bindText(4, "")
            insert.bindText(5, "")
            insert.bindText(6, "")
            insert.bindText(7, "")
            insert.bindInt(8, 1)
            insert.bindInt(9, 1)
            insert.bindText(10, "$surah:$ayah")
            insert.step()
        } finally {
            insert.close()
        }
    }
}
