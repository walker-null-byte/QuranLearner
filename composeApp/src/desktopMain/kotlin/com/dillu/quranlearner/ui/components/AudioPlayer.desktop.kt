package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class AudioPlayer {
    actual fun play(url: String, loop: Boolean) {}
    actual fun setLooping(loop: Boolean) {}
    actual fun pause() {}
    actual fun resume() {}
    actual fun stop() {}
    actual fun release() {}
    actual fun seekTo(positionMs: Long) {}
    actual val isPlaying: Boolean = false
    actual val progress: Float = 0f
    actual val currentMs: Long = 0L
    actual val durationMs: Long = 0L
    actual var onCompletion: (() -> Unit)? = null
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer = remember { AudioPlayer() }
