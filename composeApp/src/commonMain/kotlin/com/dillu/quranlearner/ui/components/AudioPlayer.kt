package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable

expect class AudioPlayer {
    fun play(url: String, loop: Boolean = false)
    fun setLooping(loop: Boolean)
    fun pause()
    fun resume()
    fun stop()
    fun release()
    fun seekTo(positionMs: Long)
    val isPlaying: Boolean
    val progress: Float      // 0f..1f
    val currentMs: Long
    val durationMs: Long
    var onCompletion: (() -> Unit)?
}

@Composable
expect fun rememberAudioPlayer(): AudioPlayer
