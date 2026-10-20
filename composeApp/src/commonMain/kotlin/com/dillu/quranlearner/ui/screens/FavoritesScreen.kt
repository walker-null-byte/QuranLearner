package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.Ayah
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(private val db: QuranDb) : ViewModel() {
    private val _favorites = MutableStateFlow<List<Ayah>>(emptyList())
    val favorites: StateFlow<List<Ayah>> = _favorites.asStateFlow()

    private val _script = MutableStateFlow("Uthmani")
    val script: StateFlow<String> = _script.asStateFlow()

    private val _surahNames = MutableStateFlow<Map<Int, String>>(emptyMap())
    val surahNames: StateFlow<Map<Int, String>> = _surahNames.asStateFlow()

    private val _reciterFolder = MutableStateFlow("Alafasy_128kbps")
    val reciterFolder: StateFlow<String> = _reciterFolder.asStateFlow()

    private val _arabicFontSize = MutableStateFlow(36f)
    val arabicFontSize: StateFlow<Float> = _arabicFontSize.asStateFlow()

    private val _translationFontSize = MutableStateFlow(18f)
    val translationFontSize: StateFlow<Float> = _translationFontSize.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _favorites.value = db.getAllFavoriteAyahs()
            _script.value = db.getSetting("quran_script", "Uthmani")

            val reciterId = db.getSetting("reciter_id", "alafasy")
            val useDataSaver = db.getSetting("use_data_saver", "false") == "true"
            _reciterFolder.value = reciterFolderFor(reciterId, useDataSaver)

            _arabicFontSize.value =
                (db.getSetting("arabic_font_size", "36").toFloatOrNull() ?: 36f).coerceIn(24f, 56f)
            _translationFontSize.value =
                (db.getSetting("translation_font_size", "18").toFloatOrNull() ?: 18f).coerceIn(12f, 28f)

            val surahs = db.getSurahs()
            _surahNames.value = surahs.associate { it.number to it.englishName }
        }
    }

    fun removeFavorite(ayah: Ayah) {
        viewModelScope.launch {
            db.toggleFavorite(ayah.surahNumber, ayah.ayahNumber)
            _favorites.value = _favorites.value.filter {
                !(it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber)
            }
        }
    }
}
