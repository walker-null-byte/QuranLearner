package com.dillu.quranlearner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors

val ALL_ACHIEVEMENTS = listOf(
    // Learning Milestones
    AchievementDef("first_seed", "First Seed", "Learned your very first Ayah.", 10, Icons.Default.Star),
    AchievementDef("the_opening", "The Opening", "Completed Surah Al-Fatiha.", 100, Icons.Default.MenuBook),
    AchievementDef("ayatul_kursi", "Throne Verse", "Learned Ayatul Kursi.", 50, Icons.Default.EmojiEvents),
    AchievementDef("three_quls", "The Protectors", "Completed Surahs 112, 113, and 114.", 200, Icons.Default.Shield),
    
    // Major Surahs
    AchievementDef("the_cave", "The Cave", "Completed Surah Al-Kahf.", 150, Icons.Default.ModeNight),
    AchievementDef("the_merciful", "The Merciful", "Completed Surah Ar-Rahman.", 150, Icons.Default.AutoAwesome),
    AchievementDef("the_heart", "The Heart", "Completed Surah Yaseen.", 300, Icons.Default.Favorite),
    AchievementDef("the_defender", "The Defender", "Completed Surah Al-Mulk.", 250, Icons.Default.Security),
    
    // Streaks and Habits
    AchievementDef("seven_days", "On Fire", "Reached a 7-day learning streak.", 100, Icons.Default.LocalFireDepartment),
    AchievementDef("thirty_days", "Habit Builder", "Reached a 30-day learning streak.", 500, Icons.Default.DoneAll),
    AchievementDef("iron_will", "Iron Will", "Reached a 100-day learning streak.", 2000, Icons.Default.FitnessCenter),
    AchievementDef("early_bird", "Early Bird", "Learned an Ayah between 4 AM and 6 AM.", 150, Icons.Default.WbTwilight),
    AchievementDef("the_friday_habit", "Friday Habit", "Learned from Surah Al-Kahf on a Friday.", 100, Icons.Default.Today),
    AchievementDef("marathon_learner", "Marathon", "Learned 50 Ayahs in a single day.", 200, Icons.Default.DirectionsRun),
    
    // Epic Milestones
    AchievementDef("juz_amma_master", "Juz Amma Master", "Completed the entire 30th Juz.", 1000, Icons.Default.MilitaryTech),
    AchievementDef("halfway_there", "Halfway There", "Learned 50% of the entire Quran.", 5000, Icons.Default.Map),
    AchievementDef("khatam_al_quran", "Khatam Al-Quran", "Completed all 114 Surahs.", 10000, Icons.Default.WorkspacePremium)
)

data class AchievementDef(val id: String, val title: String, val description: String, val xpReward: Int, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
) {
    val xp by viewModel.xp.collectAsState()
    val unlockedIds by viewModel.unlockedAchievements.collectAsState()
    val recentProgress by viewModel.recentProgress.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val noorType = LocalNoorTypography.current
    var showGoalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadStats() }

    val level = (xp / 100) + 1
    val nextLevelXp = level * 100
    val levelProgress = (xp % 100).toFloat() / 100f

    Scaffold(
        containerColor = NoorColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Stats & Achievements", style = noorType.headlineMd) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NoorColors.Surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Level Card ──
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.25f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Level $level",
                                style = noorType.headlineLg,
                                color = NoorColors.Primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "⭐ $xp / $nextLevelXp XP",
                                style = noorType.bodyLg,
                                color = NoorColors.Secondary,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { levelProgress },
                                modifier = Modifier.fillMaxWidth().height(10.dp),
                                color = NoorColors.Primary,
                                trackColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            // ── Recent Activity ──
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", style = noorType.headlineMd, color = NoorColors.OnSurface)
                        TextButton(onClick = { showGoalDialog = true }) {
                            Text("Goal: $dailyGoal", style = noorType.labelSm, color = NoorColors.Secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(14.dp), tint = NoorColors.Secondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ActivityHeatmap(recentProgress)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Achievements", style = noorType.headlineMd, color = NoorColors.OnSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            items(ALL_ACHIEVEMENTS) { achievement ->
                val isUnlocked = unlockedIds.contains(achievement.id)
                AchievementCard(achievement, isUnlocked)
            }

            // Bottom spacer
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showGoalDialog) {
            var newGoalStr by remember { mutableStateOf(dailyGoal.toString()) }
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                containerColor = NoorColors.SurfaceContainerHigh,
                titleContentColor = NoorColors.OnSurface,
                textContentColor = NoorColors.OnSurfaceVariant,
                title = { Text("Set Daily Goal", style = noorType.headlineMd) },
                text = {
                    OutlinedTextField(
                        value = newGoalStr,
                        onValueChange = { newGoalStr = it },
                        label = { Text("Ayahs per day") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NoorColors.Primary,
                            unfocusedBorderColor = NoorColors.OutlineVariant,
                            focusedLabelColor = NoorColors.Primary,
                            cursorColor = NoorColors.Primary,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        newGoalStr.toIntOrNull()?.let { 
                            if (it > 0) viewModel.updateDailyGoal(it)
                        }
                        showGoalDialog = false
                    }) {
                        Text("Save", color = NoorColors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("Cancel", color = NoorColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun ActivityHeatmap(progressList: List<DailyProgress>) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val noorType = LocalNoorTypography.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 6 downTo 0) {
            val date = today.minus(i, DateTimeUnit.DAY)
            val dateStr = date.toString()
            val progress = progressList.find { it.date == dateStr }
            val isGoalReached = progress != null && progress.ayahsLearned >= progress.goal
            val hasActivity = progress != null && progress.ayahsLearned > 0
            
            val bgColor = when {
                isGoalReached -> NoorColors.Primary
                hasActivity -> NoorColors.Primary.copy(alpha = 0.25f)
                else -> NoorColors.SurfaceContainerHigh
            }

            val borderColor = when {
                isGoalReached -> NoorColors.Primary.copy(alpha = 0.6f)
                hasActivity -> NoorColors.Primary.copy(alpha = 0.15f)
                else -> NoorColors.OutlineVariant.copy(alpha = 0.25f)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isGoalReached) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = NoorColors.OnPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date.dayOfWeek.name.take(3),
                    style = noorType.labelSm,
                    color = NoorColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: AchievementDef, isUnlocked: Boolean) {
    val noorType = LocalNoorTypography.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isUnlocked)
            NoorColors.SurfaceContainer.copy(alpha = 0.7f)
        else
            NoorColors.SurfaceContainer.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (isUnlocked) NoorColors.Primary.copy(alpha = 0.3f)
            else NoorColors.OutlineVariant.copy(alpha = 0.2f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isUnlocked)
                    NoorColors.Primary.copy(alpha = 0.15f)
                else
                    NoorColors.SurfaceContainerHighest.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) NoorColors.Primary.copy(alpha = 0.3f)
                    else NoorColors.OutlineVariant.copy(alpha = 0.15f),
                ),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isUnlocked) achievement.icon else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) NoorColors.Primary else NoorColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = achievement.title,
                style = noorType.labelSm,
                fontSize = 13.sp,
                color = if (isUnlocked) NoorColors.OnSurface else NoorColors.OnSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.description,
                style = noorType.labelSm,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
                color = if (isUnlocked) NoorColors.OnSurfaceVariant else NoorColors.OnSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isUnlocked)
                    NoorColors.Secondary.copy(alpha = 0.12f)
                else
                    NoorColors.SurfaceContainerHighest.copy(alpha = 0.3f),
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) NoorColors.Secondary.copy(alpha = 0.2f)
                    else Color.Transparent,
                ),
            ) {
                Text(
                    text = "+${achievement.xpReward} XP",
                    style = noorType.labelSm,
                    fontSize = 10.sp,
                    color = if (isUnlocked) NoorColors.Secondary else NoorColors.OnSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
