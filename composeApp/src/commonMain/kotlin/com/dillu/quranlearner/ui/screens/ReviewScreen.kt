package com.dillu.quranlearner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dillu.quranlearner.db.Ayah
import com.dillu.quranlearner.ui.components.AyahAudioDockedPlayer
import com.dillu.quranlearner.ui.components.playAyahAudio
import com.dillu.quranlearner.ui.components.rememberAudioPlayer
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors
import com.dillu.quranlearner.ui.theme.NoorTypography
import com.dillu.quranlearner.ui.theme.arabicForQuranScript
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val reviewItems by viewModel.reviewItems.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val script by viewModel.currentScript.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val surahNames by viewModel.surahEnglishNames.collectAsState()
    val noorType = LocalNoorTypography.current

    val audioPlayer = rememberAudioPlayer()

    var showMenu by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var durationTimeMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

    LaunchedEffect(currentIndex, reviewItems.size) {
        audioPlayer.stop()
        isPlaying = false
        audioProgress = 0f
        currentTimeMs = 0L
        durationTimeMs = 0L
    }

    LaunchedEffect(isPlaying) {
        var prevNativePlaying = audioPlayer.isPlaying
        while (isPlaying) {
            audioProgress = audioPlayer.progress
            currentTimeMs = audioPlayer.currentMs
            durationTimeMs = audioPlayer.durationMs
            val nowNativePlaying = audioPlayer.isPlaying
            val trackFinished =
                prevNativePlaying && !nowNativePlaying && audioProgress > 0.92f

            if (trackFinished) {
                val ayah = reviewItems.getOrNull(currentIndex)?.ayah
                when {
                    isLooping && ayah != null -> playAyahAudio(audioPlayer, ayah, isLooping)
                    else -> {
                        isPlaying = false
                        audioProgress = 0f
                    }
                }
            }
            prevNativePlaying = nowNativePlaying
            delay(80)
        }
    }

    val hasActiveCard = reviewItems.isNotEmpty() && currentIndex < reviewItems.size
    val activeAyah: Ayah? = if (hasActiveCard) reviewItems[currentIndex].ayah else null
    val dockTitle = activeAyah?.let { ayah ->
        surahNames[ayah.surahNumber] ?: "Surah ${ayah.surahNumber}"
    } ?: ""

    Scaffold(
        containerColor = NoorColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Review", style = noorType.headlineMd) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Toggle script ($script)") },
                            onClick = { viewModel.toggleScript(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showTranslation) "Hide translation" else "Show translation") },
                            onClick = { viewModel.toggleTranslation(); showMenu = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NoorColors.Surface.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            if (hasActiveCard && activeAyah != null) {
                val ayah = activeAyah
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoorColors.SurfaceContainerLowest)
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                audioPlayer.stop()
                                isPlaying = false
                                viewModel.submitReview(0)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoorColors.Error.copy(alpha = 0.12f),
                                contentColor = NoorColors.Error,
                            ),
                            border = BorderStroke(1.dp, NoorColors.Error.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Forgot", style = noorType.labelSm, maxLines = 1) }

                        Button(
                            onClick = {
                                audioPlayer.stop()
                                isPlaying = false
                                viewModel.submitReview(3)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoorColors.Secondary.copy(alpha = 0.12f),
                                contentColor = NoorColors.Secondary,
                            ),
                            border = BorderStroke(1.dp, NoorColors.Secondary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Hard", style = noorType.labelSm, maxLines = 1) }

                        Button(
                            onClick = {
                                audioPlayer.stop()
                                isPlaying = false
                                viewModel.submitReview(5)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoorColors.Primary.copy(alpha = 0.12f),
                                contentColor = NoorColors.Primary,
                            ),
                            border = BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Easy", style = noorType.labelSm, maxLines = 1) }
                    }
                    AyahAudioDockedPlayer(
                        ayah = ayah,
                        titleLine = dockTitle,
                        isPlaying = isPlaying,
                        isLooping = isLooping,
                        progress = audioProgress,
                        currentMs = currentTimeMs,
                        durationMs = durationTimeMs,
                        hasPrevious = currentIndex > 0,
                        hasNext = currentIndex < reviewItems.size - 1,
                        onPlayPause = {
                            if (isPlaying) {
                                audioPlayer.pause()
                                isPlaying = false
                            } else {
                                if (audioProgress > 0.01f) {
                                    audioPlayer.resume()
                                } else {
                                    playAyahAudio(audioPlayer, ayah, isLooping)
                                }
                                isPlaying = true
                            }
                        },
                        onPrevious = { viewModel.goToPreviousCard() },
                        onNext = { viewModel.goToNextCard() },
                        onLoopToggle = {
                            isLooping = !isLooping
                            audioPlayer.setLooping(isLooping)
                        },
                        noorType = noorType,
                        compactForBottomBar = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            currentIndex >= reviewItems.size -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    ReviewSessionComplete(
                        hadItems = reviewItems.isNotEmpty(),
                        noorType = noorType,
                    )
                }
            }
            else -> {
                val progress = (currentIndex + 1).toFloat() / reviewItems.size.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = NoorColors.Secondary,
                        trackColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${currentIndex + 1} / ${reviewItems.size}",
                        style = noorType.labelSm,
                        color = NoorColors.Secondary,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ReviewAyahCard(
                        ayah = reviewItems[currentIndex].ayah,
                        script = script,
                        showTranslation = showTranslation,
                        noorType = noorType,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewSessionComplete(hadItems: Boolean, noorType: NoorTypography) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!hadItems) {
            Text(
                text = "Nothing due today",
                style = noorType.headlineMd,
                color = NoorColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mark ayahs as learned from the reader to build your review queue.",
                style = noorType.bodyMd,
                color = NoorColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NoorColors.Primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Session complete",
                style = noorType.headlineMd,
                color = NoorColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Great work — come back tomorrow for the next set.",
                style = noorType.bodyMd,
                color = NoorColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReviewAyahCard(
    ayah: Ayah,
    script: String,
    showTranslation: Boolean,
    noorType: NoorTypography,
) {
    val verseKey = "${ayah.surahNumber}:${ayah.ayahNumber}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.35f)),
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart),
                color = NoorColors.Secondary,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            ) {}

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = verseKey,
                        style = noorType.labelSm,
                        color = NoorColors.Secondary,
                        letterSpacing = 2.sp,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = NoorColors.Primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "SRS",
                            style = noorType.labelSm,
                            color = NoorColors.Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (script == "Uthmani") ayah.textUthmani else ayah.textIndoPak,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    style = noorType.arabicForQuranScript(script).copy(
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Rtl,
                    ),
                    color = NoorColors.OnSurface,
                )
                if (showTranslation) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = ayah.translationEnglish.replace(Regex("<[^>]+>"), ""),
                        style = noorType.bodyLg,
                        color = NoorColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
