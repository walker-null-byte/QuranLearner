package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.db.Ayah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ReaderViewModel(private val db: QuranDb) : ViewModel() {

    private val _ayahs = MutableStateFlow<List<Ayah>>(emptyList())
    val ayahs: StateFlow<List<Ayah>> = _ayahs.asStateFlow()

    private val _learnedAyahs = MutableStateFlow<Set<Int>>(emptySet())
    val learnedAyahs: StateFlow<Set<Int>> = _learnedAyahs.asStateFlow()

    private val _favoriteAyahs = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteAyahs: StateFlow<Set<Int>> = _favoriteAyahs.asStateFlow()

    private val _currentScript = MutableStateFlow("Uthmani")
    val currentScript: StateFlow<String> = _currentScript.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private val _showTransliteration = MutableStateFlow(false)
    val showTransliteration: StateFlow<Boolean> = _showTransliteration.asStateFlow()

    private val _reciterFolder = MutableStateFlow("Alafasy_128kbps")
    val reciterFolder: StateFlow<String> = _reciterFolder.asStateFlow()

    private val _reciterName = MutableStateFlow("Mishary Rashid Al-Afasy")
    val reciterName: StateFlow<String> = _reciterName.asStateFlow()

    init {
        viewModelScope.launch {
            _currentScript.value = db.getSetting("quran_script", "Uthmani")
            _showTranslation.value = db.getSetting("show_translation", "true") == "true"
            _showTransliteration.value = db.getSetting("show_transliteration", "false") == "true"

            val reciterId = db.getSetting("reciter_id", "alafasy")
            val reciter = AVAILABLE_RECITERS.find { it.id == reciterId } ?: AVAILABLE_RECITERS.first()
            val useDataSaver = db.getSetting("use_data_saver", "false") == "true"
            
            val folder = if (useDataSaver) {
                reciter.folder.replace("_128kbps", "_64kbps").replace("_192kbps", "_64kbps")
            } else {
                reciter.folder
            }
            
            _reciterFolder.value = folder
            _reciterName.value = reciter.name
        }
    }

    fun loadSurah(surahNumber: Int) {
        viewModelScope.launch {
            _ayahs.value = db.getAyahsForSurah(surahNumber)
            _learnedAyahs.value = db.getLearnedAyahs(surahNumber)
            _favoriteAyahs.value = db.getFavoriteAyahNumbers(surahNumber)
        }
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

    fun toggleTransliteration() {
        viewModelScope.launch {
            val newVal = !_showTransliteration.value
            db.setSetting("show_transliteration", newVal.toString())
            _showTransliteration.value = newVal
        }
    }

    fun markAyahAsLearned(ayah: Ayah) {
        viewModelScope.launch {
            if (_learnedAyahs.value.contains(ayah.ayahNumber)) return@launch

            val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val today = localDate.toString()
            
            // Mark it in DB
            db.markAyahLearned(ayah.surahNumber, ayah.ayahNumber, today)
            _learnedAyahs.value = _learnedAyahs.value + ayah.ayahNumber

            // Award XP
            db.addXp(10) // 10 XP per ayah

            // Check Achievements
            if (_learnedAyahs.value.size == 1 && db.getLearnedAyahsCount(ayah.surahNumber) == 1) {
                if (db.unlockAchievement("first_seed", today)) db.addXp(10)
            }
            if (ayah.surahNumber == 2 && ayah.ayahNumber == 255) {
                if (db.unlockAchievement("ayatul_kursi", today)) db.addXp(50)
            }
            if (ayah.surahNumber == 1 && _learnedAyahs.value.size == _ayahs.value.size) {
                if (db.unlockAchievement("the_opening", today)) db.addXp(100)
            }
            if (ayah.surahNumber == 18 && _learnedAyahs.value.size == 110) {
                if (db.unlockAchievement("the_cave", today)) db.addXp(150)
            }
            if (ayah.surahNumber == 55 && _learnedAyahs.value.size == 78) {
                if (db.unlockAchievement("the_merciful", today)) db.addXp(150)
            }
            
            if (ayah.surahNumber == 36 && _learnedAyahs.value.size == 83) {
                if (db.unlockAchievement("the_heart", today)) db.addXp(300)
            }
            if (ayah.surahNumber == 67 && _learnedAyahs.value.size == 30) {
                if (db.unlockAchievement("the_defender", today)) db.addXp(250)
            }
            
            // 3 Quls (Surahs 112, 113, 114)
            if (ayah.surahNumber in 112..114) {
                if (db.getLearnedAyahsCount(112) == 4 && db.getLearnedAyahsCount(113) == 5 && db.getLearnedAyahsCount(114) == 6) {
                    if (db.unlockAchievement("three_quls", today)) db.addXp(200)
                }
            }
            
            // Juz Amma (78-114) Total Ayahs: 564
            if (ayah.surahNumber in 78..114) {
                if (db.getLearnedAyahsCountInRange(78, 114) == 564) {
                    if (db.unlockAchievement("juz_amma_master", today)) db.addXp(1000)
                }
            }
            
            // Global Quran Progress (Total: 6236)
            val totalLearned = db.getTotalLearnedAyahsCount()
            if (totalLearned >= 3118) {
                if (db.unlockAchievement("halfway_there", today)) db.addXp(5000)
            }
            if (totalLearned == 6236) {
                if (db.unlockAchievement("khatam_al_quran", today)) db.addXp(10000)
            }

            // Update Daily Progress
            val goalStr = db.getSetting("daily_goal", "5")
            val goal = goalStr.toIntOrNull() ?: 5
            val current = db.getProgress(today) ?: com.dillu.quranlearner.db.DailyProgress(today, 0, goal)
            val newProgress = current.copy(ayahsLearned = current.ayahsLearned + 1)
            db.updateProgress(newProgress)
            
            // Marathon Learner
            if (newProgress.ayahsLearned >= 50) {
                if (db.unlockAchievement("marathon_learner", today)) db.addXp(200)
            }
            
            // Time/Date based Achievements
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            if (now.hour in 4..5) {
                if (db.unlockAchievement("early_bird", today)) db.addXp(150)
            }
            if (now.dayOfWeek.value == 5 && ayah.surahNumber == 18) { // Friday
                if (db.unlockAchievement("the_friday_habit", today)) db.addXp(100)
            }

            // Check Streak Achievements
            val currentStreak = db.getStreak(localDate)
            if (currentStreak >= 7) {
                if (db.unlockAchievement("seven_days", today)) db.addXp(100)
            }
            if (currentStreak >= 30) {
                if (db.unlockAchievement("thirty_days", today)) db.addXp(500)
            }
            if (currentStreak >= 100) {
                if (db.unlockAchievement("iron_will", today)) db.addXp(2000)
            }
        }
    }
    fun toggleFavorite(ayah: Ayah) {
        viewModelScope.launch {
            val isFav = db.toggleFavorite(ayah.surahNumber, ayah.ayahNumber)
            _favoriteAyahs.value = if (isFav) {
                _favoriteAyahs.value + ayah.ayahNumber
            } else {
                _favoriteAyahs.value - ayah.ayahNumber
            }
        }
    }
}
