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
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

    private fun currentDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

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
            _downloadedSurahs.value = downloadedSurahsForCurrentReciter()
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
            val reciterId = db.getSetting("reciter_id", "alafasy")
            val dataSaver = db.getSetting("use_data_saver", "false").toBoolean()
            val key = downloadedSurahsKey(reciterFolderFor(reciterId, dataSaver))
            val current = _downloadedSurahs.value.toMutableSet()
            current.add(surahNumber)
            _downloadedSurahs.value = current
            db.setSetting(key, current.joinToString(","))
        }
    }

    private suspend fun downloadedSurahsForCurrentReciter(): Set<Int> {
        val reciterId = db.getSetting("reciter_id", "alafasy")
        val dataSaver = db.getSetting("use_data_saver", "false").toBoolean()
        val key = downloadedSurahsKey(reciterFolderFor(reciterId, dataSaver))
        return db.getSetting(key, "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    private suspend fun refreshProgress() {
        _dailyProgress.value = db.getProgress(currentDate().toString())
    }

    private suspend fun calculateStreak() {
        _streak.value = db.getStreak(currentDate())
    }
}
