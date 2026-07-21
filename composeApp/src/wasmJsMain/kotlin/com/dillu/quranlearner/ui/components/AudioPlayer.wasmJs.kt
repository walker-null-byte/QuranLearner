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
    actual val isPlaying: Boolean = false
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer = remember { AudioPlayer() }
