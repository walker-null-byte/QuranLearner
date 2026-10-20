package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer {
    actual var onCompletion: (() -> Unit)? = null
    private var observer: Any? = null
    private var player: AVPlayer? = null
    private var looping: Boolean = false

    actual fun play(url: String, loop: Boolean) {
        clearPlayer()
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        val newPlayer = AVPlayer(playerItem = playerItem)
        player = newPlayer
        looping = loop

        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = null,
            usingBlock = { _ ->
                if (looping) {
                    newPlayer.seekToTime(CMTimeMakeWithSeconds(0.0, 1000))
                    newPlayer.play()
                } else {
                    onCompletion?.invoke()
                }
            }
        )

        newPlayer.play()
    }

    actual fun setLooping(loop: Boolean) {
        looping = loop
    }

    actual fun pause() { player?.pause() }
    actual fun resume() { player?.play() }
    actual fun stop() { player?.pause() }

    actual fun release() = clearPlayer()

    private fun clearPlayer() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = null
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        player = null
        looping = false
    }

    actual fun seekTo(positionMs: Long) {
        player?.seekToTime(CMTimeMakeWithSeconds(positionMs.coerceAtLeast(0L) / 1000.0, 1000))
    }

    actual val isPlaying: Boolean get() = player?.timeControlStatus == 2L // AVPlayerTimeControlStatusPlaying

    actual val progress: Float
        get() {
            val p = player ?: return 0f
            val dur = CMTimeGetSeconds(p.currentItem?.duration ?: return 0f)
            val cur = CMTimeGetSeconds(p.currentTime())
            return if (dur > 0.0 && !dur.isNaN()) (cur / dur).toFloat().coerceIn(0f, 1f) else 0f
        }

    actual val currentMs: Long
        get() {
            val p = player ?: return 0L
            return (CMTimeGetSeconds(p.currentTime()) * 1000).toLong().coerceAtLeast(0)
        }

    actual val durationMs: Long
        get() {
            val p = player ?: return 0L
            val dur = CMTimeGetSeconds(p.currentItem?.duration ?: return 0L)
            return if (dur.isNaN()) 0L else (dur * 1000).toLong().coerceAtLeast(0)
        }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { AudioPlayer() }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
