package com.dillu.quranlearner.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Onboarding

@Serializable
object SurahList

@Serializable
data class Reader(val surahNumber: Int, val surahName: String, val autoPlay: Boolean = false)

@Serializable
data class SurahPlayer(val surahNumber: Int, val surahName: String)

@Serializable
object Stats

@Serializable
object MainApp
