package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.db.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

class SurahListViewModel(private val db: QuranDb) : ViewModel() {

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pinnedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val pinnedSurahs: StateFlow<Set<Int>> = _pinnedSurahs.asStateFlow()

    private val _downloadingSurahs = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadingSurahs: StateFlow<Map<Int, Float>> = _downloadingSurahs.asStateFlow()

    private val _downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSurahs: StateFlow<Set<Int>> = _downloadedSurahs.asStateFlow()

    /** Surahs filtered by the current search query and sorted by pinned status. */
    val filteredSurahs: StateFlow<List<Surah>> = combine(_surahs, _searchQuery, _pinnedSurahs) { surahs, query, pinned ->
        val filtered = if (query.isBlank()) surahs
        else {
            val q = query.trim().lowercase()
            surahs.filter { surah ->
                surah.englishName.lowercase().contains(q)
                    || surah.englishNameTranslation.lowercase().contains(q)
                    || surah.name.contains(q)
                    || surah.number.toString() == q
            }
        }
        filtered.sortedWith(compareByDescending<Surah> { pinned.contains(it.number) }.thenBy { it.number })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyProgress = MutableStateFlow<DailyProgress?>(null)
    val dailyProgress: StateFlow<DailyProgress?> = _dailyProgress.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _surahProgress = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val surahProgress: StateFlow<Map<Int, Int>> = _surahProgress.asStateFlow()

    private val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    init {
        refreshData()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refreshData() {
        viewModelScope.launch {
            _surahs.value = db.getSurahs()
            _surahProgress.value = db.getSurahProgressMap()
            _xp.value = db.getXp()
            _pinnedSurahs.value = db.getSetting("pinned_surahs", "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
            _downloadedSurahs.value = db.getSetting("downloaded_surahs", "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
            refreshProgress()
            calculateStreak()
        }
    }

    fun togglePin(surahNumber: Int) {
        viewModelScope.launch {
            val current = _pinnedSurahs.value
            val next = if (current.contains(surahNumber)) current - surahNumber else current + surahNumber
            db.setSetting("pinned_surahs", next.joinToString(","))
            _pinnedSurahs.value = next
        }
    }

    fun setDownloading(surahNumber: Int, isDownloading: Boolean, progress: Float = 0f) {
        val current = _downloadingSurahs.value.toMutableMap()
        if (isDownloading) {
            current[surahNumber] = progress
        } else {
            current.remove(surahNumber)
        }
        _downloadingSurahs.value = current
    }

    fun markDownloaded(surahNumber: Int) {
        viewModelScope.launch {
            val current = _downloadedSurahs.value.toMutableSet()
            current.add(surahNumber)
            _downloadedSurahs.value = current
            db.setSetting("downloaded_surahs", current.joinToString(","))
        }
    }

    fun markAyahAsLearned() {
        viewModelScope.launch {
            val goalStr = db.getSetting("daily_goal", "5")
            val goal = goalStr.toIntOrNull() ?: 5
            val current = _dailyProgress.value ?: DailyProgress(today, 0, goal)
            val updated = current.copy(ayahsLearned = current.ayahsLearned + 1)
            db.updateProgress(updated)
            _dailyProgress.value = updated
            calculateStreak()
        }
    }

    private suspend fun refreshProgress() {
        _dailyProgress.value = db.getProgress(today)
    }

    private suspend fun calculateStreak() {
        val progressList = db.getLast30DaysProgress()
        if (progressList.isEmpty()) { _streak.value = 0; return }

        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        var streak = 0
        var currentDate = todayDate

        val hasToday = progressList.any { it.date == todayDate.toString() && it.ayahsLearned > 0 }
        val hasYesterday = progressList.any {
            it.date == todayDate.minus(1, DateTimeUnit.DAY).toString() && it.ayahsLearned > 0
        }

        if (!hasToday && !hasYesterday) { _streak.value = 0; return }
        if (!hasToday) currentDate = todayDate.minus(1, DateTimeUnit.DAY)

        while (true) {
            val dateStr = currentDate.toString()
            val progress = progressList.find { it.date == dateStr }
            if (progress != null && progress.ayahsLearned > 0) {
                streak++
                currentDate = currentDate.minus(1, DateTimeUnit.DAY)
            } else break
        }
        _streak.value = streak
    }
}
