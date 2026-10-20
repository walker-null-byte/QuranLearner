package com.dillu.quranlearner

import com.dillu.quranlearner.db.computeStreak
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {

    private val today = LocalDate(2026, 1, 15)

    private fun daysBefore(days: Int): LocalDate = today.minus(days, DateTimeUnit.DAY)

    private fun dates(vararg days: LocalDate): Set<String> = days.map { it.toString() }.toSet()

    @Test
    fun emptySetIsZero() {
        assertEquals(0, computeStreak(emptySet(), today))
    }

    @Test
    fun onlyTodayIsOne() {
        assertEquals(1, computeStreak(dates(today), today))
    }

    @Test
    fun onlyYesterdayIsOne() {
        assertEquals(1, computeStreak(dates(daysBefore(1)), today))
    }

    @Test
    fun lastActivityTwoDaysAgoIsZero() {
        assertEquals(0, computeStreak(dates(daysBefore(2)), today))
    }

    @Test
    fun todayYesterdayThenGapIsTwo() {
        val active = dates(today, daysBefore(1), daysBefore(3))
        assertEquals(2, computeStreak(active, today))
    }

    @Test
    fun oneHundredTwentyConsecutiveDaysEndingToday() {
        val active = (0 until 120).map { daysBefore(it).toString() }.toSet()
        assertEquals(120, computeStreak(active, today))
    }
}
