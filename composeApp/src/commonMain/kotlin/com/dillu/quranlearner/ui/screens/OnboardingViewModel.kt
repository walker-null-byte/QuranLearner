package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val db: QuranDb) : ViewModel() {

    private val _selectedGoal = MutableStateFlow(5)
    val selectedGoal = _selectedGoal.asStateFlow()

    private val _selectedScript = MutableStateFlow("Uthmani")
    val selectedScript = _selectedScript.asStateFlow()

    fun setGoal(goal: Int) { _selectedGoal.value = goal }
    fun setScript(script: String) { _selectedScript.value = script }

    fun completeOnboarding() {
        viewModelScope.launch {
            db.setSetting("daily_goal", _selectedGoal.value.toString())
            db.setSetting("quran_script", _selectedScript.value)
            db.setSetting("onboarding_complete", "true")
        }
    }
}
