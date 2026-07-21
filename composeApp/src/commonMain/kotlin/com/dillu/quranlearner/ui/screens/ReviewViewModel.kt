package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.db.ReviewItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

class ReviewViewModel(private val db: QuranDb) : ViewModel() {
    private val _reviewItems = MutableStateFlow<List<ReviewItem>>(emptyList())
    val reviewItems: StateFlow<List<ReviewItem>> = _reviewItems.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _surahEnglishNames = MutableStateFlow<Map<Int, String>>(emptyMap())
    val surahEnglishNames: StateFlow<Map<Int, String>> = _surahEnglishNames.asStateFlow()

    private val _currentScript = MutableStateFlow("Uthmani")
    val currentScript: StateFlow<String> = _currentScript.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    fun loadReviews() {
        viewModelScope.launch {
            refreshReaderPrefs()
            val surahs = db.getSurahs()
            _surahEnglishNames.value = surahs.associate { it.number to it.englishName }
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            _reviewItems.value = db.getAyahsToReview(today)
            _currentIndex.value = 0
        }
    }

    private suspend fun refreshReaderPrefs() {
        _currentScript.value = db.getSetting("quran_script", "Uthmani")
        _showTranslation.value = db.getSetting("show_translation", "true") == "true"
    }

    fun toggleScript() {
        viewModelScope.launch {
            val newScript = if (_currentScript.value == "Uthmani") "IndoPak" else "Uthmani"
            db.setSetting("quran_script", newScript)
            _currentScript.value = newScript
        }
    }

    fun toggleTranslation() {
        viewModelScope.launch {
            val newVal = !_showTranslation.value
            db.setSetting("show_translation", newVal.toString())
            _showTranslation.value = newVal
        }
    }

    fun goToPreviousCard() {
        if (_currentIndex.value > 0) _currentIndex.value = _currentIndex.value - 1
    }

    fun goToNextCard() {
        val list = _reviewItems.value
        if (_currentIndex.value < list.size - 1) {
            _currentIndex.value = _currentIndex.value + 1
        }
    }

    /**
     * SM-2–style update: adjusts [ReviewItem.easeFactor], [ReviewItem.interval], and [ReviewItem.nextReviewDate]
     * from today. [quality] is 0 (Forgot), 3 (Hard), or 5 (Easy). Forgot re-queues the same ayah at the end
     * of the in-memory list for another pass today; the `ayah_reviews` row is still updated.
     */
    fun submitReview(quality: Int) {
        val currentList = _reviewItems.value
        val index = _currentIndex.value
        if (index >= currentList.size) return

        val item = currentList[index]

        // SM-2 modified logic
        var ease = item.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        ease = max(1.3f, ease)

        val newInterval = if (quality < 3) {
            1
        } else if (item.interval == 0) {
            1
        } else if (item.interval == 1) {
            6
        } else {
            (item.interval * ease).toInt()
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val nextDate = today.plus(newInterval, DateTimeUnit.DAY).toString()

        val updatedItem = item.copy(
            nextReviewDate = nextDate,
            interval = newInterval,
            easeFactor = ease
        )

        viewModelScope.launch {
            db.updateReviewItem(updatedItem)

            // If failed (0), add to back of queue to review again today
            if (quality == 0) {
                _reviewItems.value = currentList + updatedItem
            }
            _currentIndex.value = index + 1
        }
    }
}
