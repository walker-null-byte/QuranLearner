package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val db: QuranDb) : ViewModel() {

    private val _selectedReciterId = MutableStateFlow("alafasy")
    val selectedReciterId: StateFlow<String> = _selectedReciterId.asStateFlow()

    private val _arabicFontSize = MutableStateFlow(36f)
    val arabicFontSize: StateFlow<Float> = _arabicFontSize.asStateFlow()

    private val _translationFontSize = MutableStateFlow(18f)
    val translationFontSize: StateFlow<Float> = _translationFontSize.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(false)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(20)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _useDataSaverAudio = MutableStateFlow(false)
    val useDataSaverAudio: StateFlow<Boolean> = _useDataSaverAudio.asStateFlow()

    private val _exportLocation = MutableStateFlow("Default (Internal App Storage)")
    val exportLocation: StateFlow<String> = _exportLocation.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            _selectedReciterId.value = db.getSetting("reciter_id", "alafasy")
            _arabicFontSize.value = db.getSetting("arabic_font_size", "36").toFloatOrNull() ?: 36f
            _translationFontSize.value = db.getSetting("translation_font_size", "18").toFloatOrNull() ?: 18f
            _reminderEnabled.value = db.getSetting("reminder_enabled", "false") == "true"
            _reminderHour.value = db.getSetting("reminder_hour", "20").toIntOrNull() ?: 20
            _reminderMinute.value = db.getSetting("reminder_minute", "0").toIntOrNull() ?: 0
            _useDataSaverAudio.value = db.getSetting("use_data_saver", "false") == "true"
            val exportLoc = db.getSetting("export_location", "")
            if (exportLoc.isNotEmpty()) {
                _exportLocation.value = exportLoc
            }
        }
    }

    fun updateReciter(reciterId: String) {
        viewModelScope.launch {
            db.setSetting("reciter_id", reciterId)
            _selectedReciterId.value = reciterId
        }
    }

    fun updateArabicFontSize(size: Float) {
        _arabicFontSize.value = size
        viewModelScope.launch {
            db.setSetting("arabic_font_size", size.toInt().toString())
        }
    }

    fun updateTranslationFontSize(size: Float) {
        _translationFontSize.value = size
        viewModelScope.launch {
            db.setSetting("translation_font_size", size.toInt().toString())
        }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            db.setSetting("reminder_enabled", enabled.toString())
            _reminderEnabled.value = enabled
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            db.setSetting("reminder_hour", hour.toString())
            db.setSetting("reminder_minute", minute.toString())
            _reminderHour.value = hour
            _reminderMinute.value = minute
        }
    }

    fun updateDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            db.setSetting("use_data_saver", enabled.toString())
            _useDataSaverAudio.value = enabled
        }
    }

    fun updateExportLocation(uri: String) {
        viewModelScope.launch {
            _exportLocation.value = uri
            db.setSetting("export_location", uri)
        }
    }

    suspend fun getDownloadedSurahs(): List<Int> {
        return db.getSetting("downloaded_surahs", "").split(",").mapNotNull { it.toIntOrNull() }
    }
}
