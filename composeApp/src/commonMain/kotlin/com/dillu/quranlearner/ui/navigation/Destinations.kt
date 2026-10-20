package com.dillu.quranlearner.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Onboarding

@Serializable
data class Reader(val surahNumber: Int, val surahName: String)

@Serializable
data class SurahPlayer(val surahNumber: Int, val surahName: String)

@Serializable
object MainApp
