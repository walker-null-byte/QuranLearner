package com.dillu.quranlearner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dillu.quranlearner.db.Ayah
import com.dillu.quranlearner.db.DailyProgress
import com.dillu.quranlearner.db.Surah
import com.dillu.quranlearner.ui.components.rememberAudioPlayer
import com.dillu.quranlearner.ui.components.rememberNoorDownloader
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors
import com.dillu.quranlearner.ui.theme.arabicForQuranScript
import com.dillu.quranlearner.ui.theme.sanitizeQuranText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    viewModel: SurahListViewModel,
    favoritesViewModel: FavoritesViewModel,
    onSurahClick: (Surah) -> Unit,
    onSurahPlay: (Surah) -> Unit,
    onStatsClick: () -> Unit
) {
    val filteredSurahs by viewModel.filteredSurahs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val progress by viewModel.dailyProgress.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val surahProgress by viewModel.surahProgress.collectAsState()
    val noorType = LocalNoorTypography.current

    // Favorites
    val favorites by favoritesViewModel.favorites.collectAsState()
    val favScript by favoritesViewModel.script.collectAsState()
    val surahNames by favoritesViewModel.surahNames.collectAsState()
    val reciterFolder by favoritesViewModel.reciterFolder.collectAsState()
    val pinnedSurahs by viewModel.pinnedSurahs.collectAsState()
    val downloadingSurahs by viewModel.downloadingSurahs.collectAsState()
    val downloadedSurahs by viewModel.downloadedSurahs.collectAsState()

    val downloader = rememberNoorDownloader()
    
    val recitersMap = remember {
        mapOf(
            "Alafasy_128kbps" to "https://server8.mp3quran.net/afs/",
            "Husary_128kbps" to "https://server13.mp3quran.net/husr/",
            "Minshawy_Murattal_128kbps" to "https://server10.mp3quran.net/minsh/",
            "Abdul_Basit_Murattal_192kbps" to "https://server7.mp3quran.net/basit/",
            "Abdurrahmaan_As-Sudais_192kbps" to "https://server11.mp3quran.net/sds/",
            "Saood_ash-Shuraym_128kbps" to "https://server7.mp3quran.net/shrm/",
            "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net" to "https://server10.mp3quran.net/ajm/",
            "MauroAl-Muaiqly128kbps" to "https://server12.mp3quran.net/maher/"
        )
    }

    var showFavorites by remember { mutableStateOf(false) }
    
    val audioPlayer = rememberAudioPlayer()
    var playingAyahKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
        favoritesViewModel.loadFavorites()
    }

    // Reload favorites when switching back from surah view
    LaunchedEffect(showFavorites) {
        if (showFavorites) favoritesViewModel.loadFavorites()
        else audioPlayer.stop()
    }
    
    // Stop playing if ayah completes
    LaunchedEffect(audioPlayer.isPlaying, audioPlayer.progress) {
        if (!audioPlayer.isPlaying && audioPlayer.progress > 0.9f) {
            playingAyahKey = null
        }
    }

    Scaffold(
        containerColor = NoorColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Noor", style = noorType.headlineMd) },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onStatsClick() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ $xp XP",
                            style = noorType.labelSm,
                            color = NoorColors.Secondary,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "🔥 $streak",
                            style = noorType.labelSm,
                            fontSize = 14.sp,
                            color = NoorColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NoorColors.Surface.copy(alpha = 0.8f),
                ),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Tab Toggle: All Surahs / Favorites ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showFavorites,
                    onClick = { showFavorites = false },
                    label = { Text("All Surahs", style = noorType.labelSm) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NoorColors.Primary.copy(alpha = 0.15f),
                        selectedLabelColor = NoorColors.Primary,
                        containerColor = Color.Transparent,
                        labelColor = NoorColors.OnSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = !showFavorites,
                        borderColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                        selectedBorderColor = NoorColors.Primary.copy(alpha = 0.3f),
                    ),
                )
                FilterChip(
                    selected = showFavorites,
                    onClick = { showFavorites = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Favorites${if (favorites.isNotEmpty()) " (${favorites.size})" else ""}",
                                style = noorType.labelSm,
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NoorColors.Error.copy(alpha = 0.12f),
                        selectedLabelColor = NoorColors.Error,
                        selectedLeadingIconColor = NoorColors.Error,
                        containerColor = Color.Transparent,
                        labelColor = NoorColors.OnSurfaceVariant,
                        iconColor = NoorColors.OnSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showFavorites,
                        borderColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                        selectedBorderColor = NoorColors.Error.copy(alpha = 0.25f),
                    ),
                )
            }

            if (showFavorites) {
                // ── Favorites View ──
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = NoorColors.OnSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No favorites yet", style = noorType.headlineMd, color = NoorColors.OnSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap the ♡ icon on any ayah\nin the reader to save it here.",
                                style = noorType.bodyMd,
                                color = NoorColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(favorites.size) { index ->
                            val ayah = favorites[index]
                            val isPlaying = playingAyahKey == "${ayah.surahNumber}:${ayah.ayahNumber}" && audioPlayer.isPlaying
                            FavoriteAyahInlineCard(
                                ayah = ayah,
                                script = favScript,
                                surahName = surahNames[ayah.surahNumber] ?: "Surah ${ayah.surahNumber}",
                                isPlaying = isPlaying,
                                onPlayClick = {
                                    val key = "${ayah.surahNumber}:${ayah.ayahNumber}"
                                    if (playingAyahKey == key && audioPlayer.isPlaying) {
                                        audioPlayer.pause()
                                        playingAyahKey = null
                                    } else {
                                        val surahNumStr = ayah.surahNumber.toString().padStart(3, '0')
                                        val ayahNumStr = ayah.ayahNumber.toString().padStart(3, '0')
                                        val url = "https://everyayah.com/data/$reciterFolder/$surahNumStr$ayahNumStr.mp3"
                                        audioPlayer.play(url)
                                        playingAyahKey = key
                                    }
                                },
                                onRemove = { favoritesViewModel.removeFavorite(ayah) },
                            )
                        }
                    }
                }
            } else {
                // ── Surahs View ──
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            "Search surahs…",
                            style = noorType.bodyMd,
                            color = NoorColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NoorColors.OnSurfaceVariant)
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = NoorColors.OnSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorColors.Primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = NoorColors.OutlineVariant.copy(alpha = 0.3f),
                        focusedContainerColor = NoorColors.SurfaceContainer.copy(alpha = 0.4f),
                        unfocusedContainerColor = NoorColors.SurfaceContainer.copy(alpha = 0.25f),
                        cursorColor = NoorColors.Primary,
                        focusedTextColor = NoorColors.OnSurface,
                        unfocusedTextColor = NoorColors.OnSurface,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )

                // Daily Goal Card
                progress?.let { DailyGoalCard(it) }

                if (filteredSurahs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (searchQuery.isNotEmpty()) {
                            Text("No surahs match \"$searchQuery\"", style = noorType.bodyMd, color = NoorColors.OnSurfaceVariant)
                        } else {
                            CircularProgressIndicator(color = NoorColors.Primary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredSurahs.size) { index ->
                            val surah = filteredSurahs[index]
                            val learnedCount = surahProgress[surah.number] ?: 0
                            SurahItem(
                                surah = surah,
                                learnedCount = learnedCount,
                                isPinned = pinnedSurahs.contains(surah.number),
                                isDownloading = downloadingSurahs.containsKey(surah.number),
                                downloadProgress = downloadingSurahs[surah.number] ?: 0f,
                                isDownloaded = downloadedSurahs.contains(surah.number),
                                onClick = { onSurahClick(surah) },
                                onPlayClick = { onSurahPlay(surah) },
                                onPinClick = { viewModel.togglePin(surah.number) },
                                onDownloadClick = {
                                    if (downloadedSurahs.contains(surah.number) || downloadingSurahs.contains(surah.number)) return@SurahItem
                                    viewModel.setDownloading(surah.number, true)
                                    
                                    val baseUrl = recitersMap[reciterFolder] ?: "https://server8.mp3quran.net/afs/"
                                    val surahNumStr = surah.number.toString().padStart(3, '0')
                                    val fullSurahUrl = "$baseUrl$surahNumStr.mp3"
                                    
                                    val ayahUrls = (1..surah.numberOfAyahs).map { ayahNum ->
                                        val ayahNumStr = ayahNum.toString().padStart(3, '0')
                                        "https://everyayah.com/data/$reciterFolder/$surahNumStr$ayahNumStr.mp3"
                                    }
                                    
                                    downloader.download(
                                        urls = listOf(fullSurahUrl) + ayahUrls,
                                        onProgress = { progress ->
                                            viewModel.setDownloading(surah.number, true, progress)
                                        },
                                        onComplete = {
                                            viewModel.setDownloading(surah.number, false)
                                            viewModel.markDownloaded(surah.number)
                                        },
                                        onError = {
                                            viewModel.setDownloading(surah.number, false)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Inline Favorite Card (compact, no back nav needed) ──

@Composable
private fun FavoriteAyahInlineCard(
    ayah: Ayah,
    script: String,
    surahName: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val noorType = LocalNoorTypography.current
    val verseKey = "${ayah.surahNumber}:${ayah.ayahNumber}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(surahName, style = noorType.labelSm, color = NoorColors.Secondary)
                    Text(verseKey, style = noorType.labelSm, color = NoorColors.OnSurfaceVariant, letterSpacing = 2.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = NoorColors.Primary.copy(alpha = 0.12f),
                        onClick = onPlayClick,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NoorColors.Primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = NoorColors.Error.copy(alpha = 0.12f),
                        onClick = onRemove,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Remove",
                                tint = NoorColors.Error,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (script == "Uthmani") ayah.textUthmani.sanitizeQuranText() else ayah.textIndoPak.sanitizeQuranText(stripStopMarks = true),
                modifier = Modifier.fillMaxWidth(),
                style = noorType.arabicForQuranScript(script).copy(
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl,
                ),
                color = NoorColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ayah.translationEnglish,
                style = noorType.bodyMd,
                color = NoorColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun DailyGoalCard(progress: DailyProgress) {
    val noorType = LocalNoorTypography.current
    val progressFraction = (progress.ayahsLearned.toFloat() / progress.goal.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, NoorColors.Tertiary.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Goal Progress", style = noorType.labelSm, fontSize = 13.sp, color = NoorColors.Tertiary)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = NoorColors.Tertiary,
                trackColor = NoorColors.OutlineVariant.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${progress.ayahsLearned} / ${progress.goal} ayahs learned today", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SurahItem(
    surah: Surah,
    learnedCount: Int,
    isPinned: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPinClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val noorType = LocalNoorTypography.current
    val completionPercentage = if (surah.numberOfAyahs > 0) learnedCount.toFloat() / surah.numberOfAyahs else 0f
    val isComplete = completionPercentage >= 1f
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { expanded = !expanded }
        ),
        shape = RoundedCornerShape(14.dp),
        color = NoorColors.SurfaceContainer.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (isComplete) NoorColors.Primary.copy(alpha = 0.2f)
            else NoorColors.OutlineVariant.copy(alpha = 0.15f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { completionPercentage },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isComplete) NoorColors.Primary else NoorColors.Secondary,
                    trackColor = NoorColors.OutlineVariant.copy(alpha = 0.15f),
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round,
                )
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = NoorColors.SurfaceContainerHigh.copy(alpha = 0.7f),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(surah.number.toString(), style = noorType.labelSm, fontSize = 13.sp, color = NoorColors.OnSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(surah.englishName, style = noorType.bodyMd, color = NoorColors.OnSurface)
                Text(surah.englishNameTranslation, style = noorType.labelSm, color = NoorColors.OnSurfaceVariant.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(surah.name, style = noorType.bodyMd, color = NoorColors.Primary)
                Text(
                    text = if (learnedCount > 0) "$learnedCount / ${surah.numberOfAyahs} Ayahs" else "${surah.numberOfAyahs} Ayahs",
                    style = noorType.labelSm,
                    fontSize = 10.sp,
                    color = if (isComplete) NoorColors.Primary else NoorColors.OnSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            if (isPinned) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = NoorColors.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                HorizontalDivider(color = NoorColors.OutlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { expanded = false; onPlayClick() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NoorColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", style = noorType.labelSm, color = NoorColors.Primary)
                    }
                    TextButton(onClick = { onDownloadClick() }) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(16.dp),
                                color = NoorColors.Secondary,
                                strokeWidth = 2.dp,
                                trackColor = NoorColors.OutlineVariant.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        } else {
                            Icon(
                                if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = null,
                                tint = NoorColors.Secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDownloading) "${(downloadProgress * 100).toInt()}%" else if (isDownloaded) "Downloaded" else "Download", style = noorType.labelSm, color = NoorColors.Secondary)
                    }
                    TextButton(onClick = { expanded = false; onPinClick() }) {
                        Icon(
                            if (isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                            contentDescription = null,
                            tint = NoorColors.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPinned) "Unpin" else "Pin", style = noorType.labelSm, color = NoorColors.Secondary)
                    }
                }
            }
        }
        }
    }
}
