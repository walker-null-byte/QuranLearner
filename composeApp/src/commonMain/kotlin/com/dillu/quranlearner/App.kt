package com.dillu.quranlearner

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.ui.navigation.*
import com.dillu.quranlearner.ui.screens.*
import com.dillu.quranlearner.ui.theme.NoorTheme
import com.dillu.quranlearner.ui.theme.NoorColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*

// Simple dependency passing — no DI framework needed
val LocalQuranDb = staticCompositionLocalOf<QuranDb> { error("QuranDb not provided") }

@Composable
fun App() {
    val db = LocalQuranDb.current
    var isOnboardingDone by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(db) {
        isOnboardingDone = db.getSetting("onboarding_complete", "false") == "true"
    }

    if (isOnboardingDone == null) {
        return // Show nothing while loading (fast)
    }

    val navController = rememberNavController()

    NoorTheme {
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingDone == true) MainApp else Onboarding
        ) {
            composable<Onboarding> {
                val vm = viewModel { OnboardingViewModel(db) }
                OnboardingScreen(
                    viewModel = vm,
                    onComplete = {
                        navController.navigate(MainApp) {
                            popUpTo(Onboarding) { inclusive = true }
                        }
                    }
                )
            }
            composable<MainApp> {
                var currentTab by remember { mutableStateOf("Quran") }
                Scaffold(
                    containerColor = NoorColors.Background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = NoorColors.Surface.copy(alpha = 0.95f),
                        ) {
                            NavigationBarItem(
                                selected = currentTab == "Quran",
                                onClick = { currentTab = "Quran" },
                                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Quran") },
                                label = { Text("Quran") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NoorColors.Primary,
                                    selectedTextColor = NoorColors.Primary,
                                    indicatorColor = NoorColors.Primary.copy(alpha = 0.12f),
                                ),
                            )
                            NavigationBarItem(
                                selected = currentTab == "Review",
                                onClick = { currentTab = "Review" },
                                icon = { Icon(Icons.Default.Repeat, contentDescription = "Review") },
                                label = { Text("Review") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NoorColors.Primary,
                                    selectedTextColor = NoorColors.Primary,
                                    indicatorColor = NoorColors.Primary.copy(alpha = 0.12f),
                                ),
                            )
                            NavigationBarItem(
                                selected = currentTab == "Stats",
                                onClick = { currentTab = "Stats" },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                                label = { Text("Stats") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NoorColors.Primary,
                                    selectedTextColor = NoorColors.Primary,
                                    indicatorColor = NoorColors.Primary.copy(alpha = 0.12f),
                                ),
                            )
                            NavigationBarItem(
                                selected = currentTab == "Settings",
                                onClick = { currentTab = "Settings" },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NoorColors.Primary,
                                    selectedTextColor = NoorColors.Primary,
                                    indicatorColor = NoorColors.Primary.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        when (currentTab) {
                            "Quran" -> {
                                val surahVm = viewModel { SurahListViewModel(db) }
                                val favVm = viewModel { FavoritesViewModel(db) }
                                SurahListScreen(
                                    viewModel = surahVm,
                                    favoritesViewModel = favVm,
                                    onSurahClick = { surah ->
                                        navController.navigate(Reader(surahNumber = surah.number, surahName = surah.englishName))
                                    },
                                    onSurahPlay = { surah ->
                                        navController.navigate(SurahPlayer(surahNumber = surah.number, surahName = surah.englishName))
                                    },
                                    onStatsClick = { currentTab = "Stats" }
                                )
                            }
                            "Review" -> {
                                val vm = viewModel { ReviewViewModel(db) }
                                ReviewScreen(viewModel = vm)
                            }
                            "Stats" -> {
                                val vm = viewModel { StatsViewModel(db) }
                                StatsScreen(viewModel = vm)
                            }
                            "Settings" -> {
                                val vm = viewModel { SettingsViewModel(db) }
                                SettingsScreen(viewModel = vm)
                            }
                        }
                    }
                }
            }
            composable<Reader> { backStackEntry ->
                val route: Reader = backStackEntry.toRoute()
                val vm = viewModel { ReaderViewModel(db) }
                ReaderScreen(
                    surahNumber = route.surahNumber,
                    surahName = route.surahName,
                    viewModel = vm,
                    onPlayWholeSurah = {
                        navController.navigate(SurahPlayer(surahNumber = route.surahNumber, surahName = route.surahName))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<SurahPlayer> { backStackEntry ->
                val route: SurahPlayer = backStackEntry.toRoute()
                val vm = viewModel { ReaderViewModel(db) }
                SurahPlayerScreen(
                    surahNumber = route.surahNumber,
                    surahName = route.surahName,
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
