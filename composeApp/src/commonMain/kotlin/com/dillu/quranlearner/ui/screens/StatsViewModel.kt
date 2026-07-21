package com.dillu.quranlearner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.db.QuranDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(private val db: QuranDb) : ViewModel() {
    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _unlockedAchievements = MutableStateFlow<List<String>>(emptyList())
    val unlockedAchievements: StateFlow<List<String>> = _unlockedAchievements.asStateFlow()

    private val _recentProgress = MutableStateFlow<List<DailyProgress>>(emptyList())
    val recentProgress: StateFlow<List<DailyProgress>> = _recentProgress.asStateFlow()

    private val _dailyGoal = MutableStateFlow(5)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _xp.value = db.getXp()
            _unlockedAchievements.value = db.getUnlockedAchievements()
            _recentProgress.value = db.getLast30DaysProgress()
            _dailyGoal.value = db.getSetting("daily_goal", "5").toIntOrNull() ?: 5
        }
    }

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            db.setSetting("daily_goal", newGoal.toString())
            _dailyGoal.value = newGoal
        }
    }
}
