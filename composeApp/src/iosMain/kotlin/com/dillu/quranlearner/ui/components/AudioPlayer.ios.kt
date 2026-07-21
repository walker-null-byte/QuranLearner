package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.*
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.Foundation.NSURL
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.AVPlayerLooper
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification

actual class AudioPlayer {
    actual var onCompletion: (() -> Unit)? = null
    private var observer: Any? = null
    private var player: AVPlayer? = null
    private var looper: AVPlayerLooper? = null

    actual fun play(url: String, loop: Boolean) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        
        if (loop) {
            val qPlayer = AVQueuePlayer()
            looper = AVPlayerLooper(player = qPlayer, templateItem = playerItem)
            player = qPlayer
        } else {
            looper = null
            player = AVPlayer(playerItem = playerItem)
        }

        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = null,
            usingBlock = { _ ->
                onCompletion?.invoke()
            }
        )

        player?.play()
    }

    actual fun setLooping(loop: Boolean) {}

    actual fun pause() { player?.pause() }
    actual fun resume() { player?.play() }
    actual fun stop() { player?.pause() }
    actual fun release() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        player = null
    }
    actual fun seekTo(positionMs: Long) {
        player?.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 1000))
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
    return remember { AudioPlayer() }
}
