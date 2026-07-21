package com.dillu.quranlearner.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

object AudioCacheManager {
    var cache: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "quran_audio_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB limit
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }
}

actual class AudioPlayer(private val player: ExoPlayer) {
    actual var onCompletion: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompletion?.invoke()
                }
            }
        })
    }

    actual fun play(url: String, loop: Boolean) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.prepare()
        player.play()
    }

    actual fun setLooping(loop: Boolean) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    actual fun pause() { player.pause() }
    actual fun resume() { player.play() }
    actual fun stop() { player.stop() }
    actual fun release() { player.release() }
    actual fun seekTo(positionMs: Long) { player.seekTo(positionMs) }
    actual val isPlaying: Boolean get() = player.isPlaying

    actual val progress: Float
        get() {
            val dur = player.duration
            val pos = player.currentPosition
            return if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }
    actual val currentMs: Long get() = player.currentPosition.coerceAtLeast(0)
    actual val durationMs: Long get() = player.duration.coerceAtLeast(0)
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    val exoPlayer = remember {
        val cache = AudioCacheManager.getInstance(context.applicationContext)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
            .build()
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    return remember { AudioPlayer(exoPlayer) }
}
