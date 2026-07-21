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

    fun loadFavorites() {
        viewModelScope.launch {
            _favorites.value = db.getAllFavoriteAyahs()
            _script.value = db.getSetting("quran_script", "Uthmani")
            
            val reciterId = db.getSetting("reciter_id", "alafasy")
            val reciters = listOf(
                ReciterOption("alafasy", "Mishary Rashid Al-Afasy", "Alafasy_128kbps"),
                ReciterOption("husary", "Mahmoud Khalil Al-Husary", "Husary_128kbps"),
                ReciterOption("minshawi_murattal", "Mohamed Siddiq Al-Minshawi", "Minshawy_Murattal_128kbps"),
                ReciterOption("abdulbasit_murattal", "Abdul Basit (Murattal)", "Abdul_Basit_Murattal_192kbps"),
                ReciterOption("sudais", "Abdur-Rahman As-Sudais", "Abdurrahmaan_As-Sudais_192kbps"),
                ReciterOption("shuraim", "Saud Ash-Shuraim", "Saood_ash-Shuraym_128kbps"),
                ReciterOption("ajamy", "Ahmed Al-Ajamy", "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net"),
                ReciterOption("maher", "Maher Al-Muaiqly", "MauroAl-Muaiqly128kbps")
            )
            val selected = reciters.find { it.id == reciterId } ?: reciters.first()
            val useDataSaver = db.getSetting("use_data_saver", "false") == "true"
            val folder = if (useDataSaver) {
                selected.folder.replace("_128kbps", "_64kbps").replace("_192kbps", "_64kbps")
            } else {
                selected.folder
            }
            _reciterFolder.value = folder

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
