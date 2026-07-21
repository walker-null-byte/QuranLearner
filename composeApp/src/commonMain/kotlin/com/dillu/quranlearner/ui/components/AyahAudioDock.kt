package com.dillu.quranlearner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dillu.quranlearner.db.Ayah
import com.dillu.quranlearner.ui.theme.NoorColors
import com.dillu.quranlearner.ui.theme.NoorTypography
import kotlinx.coroutines.delay
import kotlin.math.sin

fun playAyahAudio(audioPlayer: AudioPlayer, ayah: Ayah, loop: Boolean, reciterFolder: String = "Alafasy_128kbps") {
    val s = ayah.surahNumber.toString().padStart(3, '0')
    val a = ayah.ayahNumber.toString().padStart(3, '0')
    val url = "https://everyayah.com/data/$reciterFolder/$s$a.mp3"
    audioPlayer.play(url, loop = loop)
}

fun formatAyahAudioTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

@Composable
fun AyahAudioDockedPlayer(
    ayah: Ayah,
    titleLine: String,
    isPlaying: Boolean,
    isLooping: Boolean,
    progress: Float,
    currentMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLoopToggle: () -> Unit,
    noorType: NoorTypography,
    reciterName: String = "Mishary Rashid Al-Afasy",
    /** Tighter padding, no [navigationBarsPadding] — use when placed inside [Scaffold] bottomBar (parent applies insets). */
    compactForBottomBar: Boolean = false,
) {
    val horizontalPad = if (compactForBottomBar) 12.dp else 24.dp
    val verticalPad = if (compactForBottomBar) 6.dp else 24.dp
    val innerPad = if (compactForBottomBar) 12.dp else 20.dp
    val elevation = if (compactForBottomBar) 8.dp else 16.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad, vertical = verticalPad)
            .then(if (!compactForBottomBar) Modifier.navigationBarsPadding() else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = NoorColors.SurfaceContainerHighest.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.2f)),
        shadowElevation = elevation,
    ) {
        Column(modifier = Modifier.padding(innerPad)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = formatAyahAudioTime(currentMs),
                    style = noorType.labelSm,
                    color = NoorColors.OnSurfaceVariant.copy(alpha = 0.7f),
                )
                AyahAudioSquigglyProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = formatAyahAudioTime(durationMs),
                    style = noorType.labelSm,
                    color = NoorColors.OnSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleLine,
                        style = noorType.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                        color = NoorColors.OnSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = "Ayah ${ayah.ayahNumber} · $reciterName",
                        style = noorType.labelSm,
                        color = NoorColors.OnSurfaceVariant.copy(alpha = 0.8f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onLoopToggle,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (isLooping) NoorColors.Primary else NoorColors.OnSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        enabled = hasPrevious,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (hasPrevious) NoorColors.OnSurfaceVariant else NoorColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = NoorColors.Primary,
                        shadowElevation = 8.dp,
                        onClick = onPlayPause,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NoorColors.OnPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        enabled = hasNext,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (hasNext) NoorColors.OnSurfaceVariant else NoorColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahAudioSquigglyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val playedColor = NoorColors.Secondary
    val dotColor = NoorColors.Secondary
    val trackColor = NoorColors.OutlineVariant.copy(alpha = 0.3f)

    var phase by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.12f
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val amplitude = height / 3f
        val wavelength = 80f
        val strokeWidth = 8f
        val progressX = width * progress.coerceIn(0f, 1f)

        drawLine(
            color = trackColor,
            start = Offset(progressX, midY),
            end = Offset(width, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        if (progressX > 0f) {
            val path = Path()
            path.moveTo(0f, midY)
            var x = 0f
            while (x <= progressX) {
                val y = midY + amplitude * sin((x / wavelength) * 2 * Math.PI.toFloat() + phase)
                path.lineTo(x, y)
                x += 1.5f
            }
            drawPath(
                path = path,
                color = playedColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val dotY = midY + amplitude * sin((progressX / wavelength) * 2 * Math.PI.toFloat() + phase)
            drawCircle(
                color = dotColor,
                radius = 6f,
                center = Offset(progressX, dotY),
            )
        }
    }
}
