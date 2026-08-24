package com.dillu.quranlearner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dillu.quranlearner.ui.components.rememberAudioPlayer
import com.dillu.quranlearner.ui.components.rememberSurahNamePainter
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahPlayerScreen(
    surahNumber: Int,
    surahName: String,
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val ayahs by viewModel.ayahs.collectAsState()
    val reciterFolder by viewModel.reciterFolder.collectAsState()
    val reciterName by viewModel.reciterName.collectAsState()
    val noorType = LocalNoorTypography.current

    val audioPlayer = rememberAudioPlayer()

    var isPlaying by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var durationTimeMs by remember { mutableStateOf(0L) }

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

    LaunchedEffect(surahNumber) {
        viewModel.loadSurah(surahNumber)
    }

    val baseUrl = recitersMap[reciterFolder] ?: "https://server8.mp3quran.net/afs/"
    val surahNumStr = surahNumber.toString().padStart(3, '0')
    val fullSurahUrl = "$baseUrl$surahNumStr.mp3"

    // Auto-play when loaded
    LaunchedEffect(fullSurahUrl) {
        audioPlayer.play(fullSurahUrl, loop = false)
        isPlaying = true
    }

    audioPlayer.onCompletion = {
        isPlaying = false
        audioProgress = 0f
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            audioProgress = audioPlayer.progress
            currentTimeMs = audioPlayer.currentMs
            durationTimeMs = audioPlayer.durationMs
            delay(50)
        }
    }

    fun playPause() {
        if (isPlaying) {
            audioPlayer.pause()
            isPlaying = false
        } else {
            if (audioProgress > 0.01f && audioProgress < 0.99f) {
                audioPlayer.resume()
            } else {
                audioPlayer.play(fullSurahUrl, loop = false)
            }
            isPlaying = true
        }
    }

    fun seekForward() {
        val newTime = (currentTimeMs + 15000).coerceAtMost(durationTimeMs)
        audioPlayer.seekTo(newTime)
    }

    fun seekBackward() {
        val newTime = (currentTimeMs - 15000).coerceAtLeast(0)
        audioPlayer.seekTo(newTime)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", style = noorType.headlineMd) },
                navigationIcon = {
                    IconButton(onClick = { audioPlayer.stop(); onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NoorColors.Background)
            )
        },
        containerColor = NoorColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Album Art" — flexes to whatever vertical space is left, stays square
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.aspectRatio(1f),
                    shape = RoundedCornerShape(32.dp),
                    color = NoorColors.SurfaceContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.2f))
                ) {
                    val artPainter = rememberSurahNamePainter(surahNumber)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(28.dp)
                    ) {
                        if (artPainter != null) {
                            Image(
                                painter = artPainter,
                                contentDescription = "Surah $surahNumber calligraphy",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.TopStart).size(32.dp),
                                shape = CircleShape,
                                color = NoorColors.Primary.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        surahNumber.toString(),
                                        style = noorType.labelSm,
                                        color = NoorColors.Primary
                                    )
                                }
                            }
                        } else {
                            Text(
                                surahNumber.toString(),
                                style = noorType.displayLg,
                                color = NoorColors.Primary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Titles
            Text(surahName, style = noorType.headlineLg, color = NoorColors.OnSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(reciterName, style = noorType.bodyLg, color = NoorColors.Secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Full Surah Recitation",
                style = noorType.labelSm,
                color = NoorColors.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Slider(
                value = audioProgress,
                onValueChange = { newProgress ->
                    val newTimeMs = (newProgress * durationTimeMs).toLong()
                    audioPlayer.seekTo(newTimeMs)
                    audioProgress = newProgress
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NoorColors.Primary,
                    activeTrackColor = NoorColors.Primary,
                    inactiveTrackColor = NoorColors.SurfaceContainerHighest
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentTimeMs), style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                Text(formatTime(durationTimeMs), style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { seekBackward() }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind 15s", modifier = Modifier.size(36.dp), tint = NoorColors.OnSurface)
                }

                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = NoorColors.Primary,
                    onClick = { playPause() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NoorColors.OnPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { seekForward() }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward 15s", modifier = Modifier.size(36.dp), tint = NoorColors.OnSurface)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
