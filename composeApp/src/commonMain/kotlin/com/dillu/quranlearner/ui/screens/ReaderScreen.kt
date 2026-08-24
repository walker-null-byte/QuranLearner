package com.dillu.quranlearner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dillu.quranlearner.ui.theme.arabicForQuranScript
import com.dillu.quranlearner.ui.theme.sanitizeQuranText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    surahNumber: Int,
    surahName: String,
    viewModel: ReaderViewModel,
    onPlayWholeSurah: () -> Unit,
    onBackClick: () -> Unit
) {
    val ayahs by viewModel.ayahs.collectAsState()
    val learnedAyahs by viewModel.learnedAyahs.collectAsState()
    val favoriteAyahs by viewModel.favoriteAyahs.collectAsState()
    val script by viewModel.currentScript.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val reciterFolder by viewModel.reciterFolder.collectAsState()
    val reciterName by viewModel.reciterName.collectAsState()
    val noorType = LocalNoorTypography.current

    var showMenu by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }

    val audioPlayer = rememberAudioPlayer()

    // Player state
    var activeAyahIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var durationTimeMs by remember { mutableStateOf(0L) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val activeAyah = if (activeAyahIndex in ayahs.indices) ayahs[activeAyahIndex] else null

    // Update player onCompletion callback to handle auto-advance reliably
    audioPlayer.onCompletion = {
        if (isLooping && activeAyahIndex in ayahs.indices) {
            playAyahAudio(audioPlayer, ayahs[activeAyahIndex], isLooping, reciterFolder)
        } else if (!isLooping && activeAyahIndex < ayahs.size - 1) {
            activeAyahIndex++
            playAyahAudio(audioPlayer, ayahs[activeAyahIndex], isLooping, reciterFolder)
            isPlaying = true
            scope.launch {
                listState.animateScrollToItem(activeAyahIndex + if (surahNumber != 1 && surahNumber != 9) 1 else 0)
            }
        } else {
            isPlaying = false
            audioProgress = 0f
            activeAyahIndex = -1
        }
    }

    // Poll audio progress only for UI updates
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            audioProgress = audioPlayer.progress
            currentTimeMs = audioPlayer.currentMs
            durationTimeMs = audioPlayer.durationMs
            delay(50)
        }
    }

    LaunchedEffect(surahNumber) {
        viewModel.loadSurah(surahNumber)
    }

    fun onAyahTap(index: Int) {
        if (activeAyahIndex == index && isPlaying) {
            // Pause
            audioPlayer.pause()
            isPlaying = false
        } else {
            activeAyahIndex = index
            playAyahAudio(audioPlayer, ayahs[index], isLooping, reciterFolder)
            isPlaying = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NoorColors.Background)) {
        Scaffold(
            topBar = {
                if (!isFocusMode) {
                    TopAppBar(
                        title = { Text(surahName, style = noorType.headlineMd) },
                        navigationIcon = {
                            IconButton(onClick = { audioPlayer.stop(); onBackClick() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (ayahs.isNotEmpty()) {
                                IconButton(onClick = {
                                    audioPlayer.stop()
                                    isPlaying = false
                                    onPlayWholeSurah()
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Whole Surah")
                                }
                            }
                            IconButton(onClick = { isFocusMode = true }) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = "Focus Mode")
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Toggle Script ($script)") },
                                    onClick = { viewModel.toggleScript(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showTranslation) "Hide Translation" else "Show Translation") },
                                    onClick = { viewModel.toggleTranslation(); showMenu = false }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NoorColors.Surface.copy(alpha = 0.8f)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (isFocusMode) {
                    SmallFloatingActionButton(
                        onClick = { isFocusMode = false },
                        containerColor = NoorColors.SecondaryContainer
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Exit Focus Mode")
                    }
                }
            },
            containerColor = NoorColors.Background
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFocusMode) PaddingValues(0.dp) else padding),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = if (activeAyah != null) 200.dp else 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Bismillah
                if (surahNumber != 1 && surahNumber != 9) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = noorType.arabicForQuranScript(script).copy(
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Rtl,
                                )
                            )
                        }
                    }
                }

                items(ayahs.size) { index ->
                    val ayah = ayahs[index]
                    val isLearned = learnedAyahs.contains(ayah.ayahNumber)
                    val isActive = index == activeAyahIndex

                    AyahCard(
                        ayah = ayah,
                        script = script,
                        showTranslation = showTranslation,
                        isLearned = isLearned,
                        isFavorite = favoriteAyahs.contains(ayah.ayahNumber),
                        isActive = isActive,
                        onTap = { onAyahTap(index) },
                        onMarkLearned = { viewModel.markAyahAsLearned(ayah) },
                        onToggleFavorite = { viewModel.toggleFavorite(ayah) },
                        noorType = noorType
                    )
                }
            }
        }

        // ── Docked Bottom Player ──
        AnimatedVisibility(
            visible = activeAyah != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeAyah?.let { ayah ->
                AyahAudioDockedPlayer(
                    ayah = ayah,
                    titleLine = surahName,
                    isPlaying = isPlaying,
                    isLooping = isLooping,
                    progress = audioProgress,
                    currentMs = currentTimeMs,
                    durationMs = durationTimeMs,
                    hasPrevious = activeAyahIndex > 0,
                    hasNext = activeAyahIndex < ayahs.size - 1,
                    onPlayPause = {
                        if (isPlaying) {
                            audioPlayer.pause()
                            isPlaying = false
                        } else {
                            if (audioProgress > 0.01f) {
                                audioPlayer.resume()
                            } else {
                                playAyahAudio(audioPlayer, ayah, isLooping, reciterFolder)
                            }
                            isPlaying = true
                        }
                    },
                    onPrevious = {
                        if (activeAyahIndex > 0) {
                            activeAyahIndex--
                            playAyahAudio(audioPlayer, ayahs[activeAyahIndex], isLooping, reciterFolder)
                            isPlaying = true
                            scope.launch {
                                listState.animateScrollToItem(activeAyahIndex + if (surahNumber != 1 && surahNumber != 9) 1 else 0)
                            }
                        }
                    },
                    onNext = {
                        if (activeAyahIndex < ayahs.size - 1) {
                            activeAyahIndex++
                            playAyahAudio(audioPlayer, ayahs[activeAyahIndex], isLooping, reciterFolder)
                            isPlaying = true
                            scope.launch {
                                listState.animateScrollToItem(activeAyahIndex + if (surahNumber != 1 && surahNumber != 9) 1 else 0)
                            }
                        }
                    },
                    onLoopToggle = {
                        isLooping = !isLooping
                        audioPlayer.setLooping(isLooping)
                    },
                    noorType = noorType,
                    reciterName = reciterName,
                )
            }
        }
    }
}

// ── Ayah Card ──

@Composable
private fun AyahCard(
    ayah: Ayah,
    script: String,
    showTranslation: Boolean,
    isLearned: Boolean,
    isFavorite: Boolean,
    isActive: Boolean,
    onTap: () -> Unit,
    onMarkLearned: () -> Unit,
    onToggleFavorite: () -> Unit,
    noorType: com.dillu.quranlearner.ui.theme.NoorTypography
) {
    val verseKey = "${ayah.surahNumber}:${ayah.ayahNumber}"

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        color = NoorColors.SurfaceContainer.copy(alpha = if (isActive) 0.6f else 0.4f),
        border = BorderStroke(
            1.dp,
            if (isActive) NoorColors.OutlineVariant.copy(alpha = 0.4f)
            else NoorColors.OutlineVariant.copy(alpha = 0.15f)
        )
    ) {
        Box {
            // Active indicator — amber left bar
            if (isActive) {
                Surface(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart),
                    color = NoorColors.Secondary,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                ) {}
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Header: verse key + favorite + bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = verseKey,
                        style = noorType.labelSm,
                        color = if (isActive) NoorColors.Secondary else NoorColors.OnSurfaceVariant,
                        letterSpacing = 2.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Favorite heart
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = if (isFavorite) NoorColors.Error.copy(alpha = 0.12f) else NoorColors.SurfaceVariant.copy(alpha = 0.3f),
                            onClick = onToggleFavorite,
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                    tint = if (isFavorite) NoorColors.Error else NoorColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Learned check
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = if (isLearned) NoorColors.Primary.copy(alpha = 0.15f) else NoorColors.SurfaceVariant.copy(alpha = 0.3f),
                            onClick = { if (!isLearned) onMarkLearned() }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    if (isLearned) Icons.Default.CheckCircle else Icons.Default.Check,
                                    contentDescription = if (isLearned) "Learned" else "Mark Learned",
                                    tint = if (isLearned) NoorColors.Primary else NoorColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arabic text
                Text(
                    text = if (script == "Uthmani") ayah.textUthmani.sanitizeQuranText() else ayah.textIndoPak.sanitizeQuranText(stripStopMarks = true),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    style = noorType.arabicForQuranScript(script).copy(
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Rtl,
                    ),
                    color = NoorColors.OnSurface
                )

                // Translation
                if (showTranslation) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = ayah.translationEnglish.replace(Regex("<[^>]+>"), ""),
                        style = noorType.bodyLg,
                        color = NoorColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
