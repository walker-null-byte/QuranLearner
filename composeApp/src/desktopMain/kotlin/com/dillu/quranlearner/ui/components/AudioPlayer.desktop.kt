package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URI
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

private const val READ_BUFFER_BYTES = 8192
private const val LINE_BUFFER_BYTES = 64 * 1024

/**
 * javax.sound.sampled based player. Audio is decoded by the mp3spi SPI (registered through
 * ServiceLoader) and written to a [SourceDataLine] on a daemon thread. URLs are resolved to
 * the local download cache first (see [desktopAudioFileFor]), otherwise streamed directly.
 *
 * All cross-thread state is @Volatile; each playback run carries a generation id so a slow
 * thread from a previous play/seek can never overwrite the state of the current one.
 */
actual class AudioPlayer {

    @Volatile private var worker: Thread? = null
    @Volatile private var currentUrl: String? = null
    @Volatile private var line: SourceDataLine? = null
    @Volatile private var generation = 0
    @Volatile private var playingFlag = false
    @Volatile private var pausedFlag = false
    @Volatile private var loopEnabled = false
    @Volatile private var stopRequested = false
    @Volatile private var pendingSeekMs = 0L
    @Volatile private var currentPositionMs = 0L
    @Volatile private var totalDurationMs = 0L
    @Volatile private var progressValue = 0f

    @Volatile actual var onCompletion: (() -> Unit)? = null

    actual fun play(url: String, loop: Boolean) {
        startPlayback(url, loop, 0L)
    }

    actual fun setLooping(loop: Boolean) {
        loopEnabled = loop
    }

    actual fun pause() {
        pausedFlag = true
        runCatching { line?.stop() }
    }

    actual fun resume() {
        pausedFlag = false
        runCatching { line?.start() }
    }

    actual fun stop() {
        stopInternal()
        currentPositionMs = 0L
        progressValue = 0f
    }

    actual fun release() {
        stopInternal()
        onCompletion = null
    }

    actual fun seekTo(positionMs: Long) {
        val url = currentUrl ?: return
        val max = if (totalDurationMs > 0L) totalDurationMs else Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, max)
        if (!playingFlag) {
            currentPositionMs = target
            return
        }
        val wasPaused = pausedFlag
        startPlayback(url, loopEnabled, target)
        if (wasPaused) pause()
    }

    actual val isPlaying: Boolean get() = playingFlag && !pausedFlag

    actual val progress: Float get() = progressValue

    actual val currentMs: Long get() = currentPositionMs

    actual val durationMs: Long get() = totalDurationMs

    private fun startPlayback(url: String, loop: Boolean, startAtMs: Long) {
        generation += 1
        val myGeneration = generation
        stopInternal()
        currentUrl = url
        loopEnabled = loop
        pendingSeekMs = startAtMs
        stopRequested = false
        pausedFlag = false
        playingFlag = true
        progressValue = 0f
        currentPositionMs = startAtMs
        totalDurationMs = 0L

        val thread = Thread({ runPlayback(myGeneration) }, "QuranAudioPlayer")
        thread.isDaemon = true
        worker = thread
        thread.start()
    }

    private fun stopInternal() {
        stopRequested = true
        playingFlag = false
        val activeLine = line
        line = null
        runCatching { activeLine?.stop() }
        runCatching { activeLine?.flush() }
        runCatching { activeLine?.close() }

        val activeWorker = worker
        worker = null
        if (activeWorker != null && activeWorker !== Thread.currentThread()) {
            runCatching { activeWorker.join(500) }
        }
    }

    private fun cancelled(myGeneration: Int): Boolean =
        stopRequested || myGeneration != generation

    private fun runPlayback(myGeneration: Int) {
        val url = currentUrl ?: return
        try {
            var firstPass = true
            while (!cancelled(myGeneration)) {
                var dataLine: SourceDataLine? = null
                var stream: AudioInputStream? = null
                try {
                    val (raw, contentLength) = openRawStream(url)
                    stream = AudioSystem.getAudioInputStream(BufferedInputStream(raw))
                    val format = stream.format
                    updateDuration(stream, format, contentLength)

                    val info = DataLine.Info(SourceDataLine::class.java, format)
                    dataLine = if (AudioSystem.isLineSupported(info)) {
                        AudioSystem.getLine(info) as SourceDataLine
                    } else {
                        AudioSystem.getSourceDataLine(format)
                    }
                    dataLine.open(format, LINE_BUFFER_BYTES)
                    line = dataLine
                    dataLine.start()
                    if (pausedFlag) dataLine.stop()

                    val startMs = if (firstPass) pendingSeekMs else 0L
                    firstPass = false
                    if (startMs > 0L) {
                        skipDecoded(stream, format, startMs, myGeneration)
                    }
                    var decodedBytes = msToBytes(startMs, format)
                    updateProgress(decodedBytes, format, myGeneration)

                    val buffer = ByteArray(READ_BUFFER_BYTES)
                    while (!cancelled(myGeneration)) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        dataLine.write(buffer, 0, read)
                        decodedBytes += read
                        updateProgress(decodedBytes, format, myGeneration)
                    }

                    if (!cancelled(myGeneration)) {
                        runCatching { dataLine.drain() }
                    }
                } finally {
                    runCatching { stream?.close() }
                    if (line === dataLine) line = null
                    runCatching { dataLine?.stop() }
                    runCatching { dataLine?.flush() }
                    runCatching { dataLine?.close() }
                }

                if (cancelled(myGeneration) || !loopEnabled) break
            }

            if (!cancelled(myGeneration)) {
                playingFlag = false
                currentPositionMs = totalDurationMs
                progressValue = if (totalDurationMs > 0L) 1f else 0f
                onCompletion?.invoke()
            }
        } catch (t: Throwable) {
            System.err.println("AudioPlayer: playback failed: ${t.message}")
            if (myGeneration == generation) playingFlag = false
        }
    }

    private fun openRawStream(url: String): Pair<InputStream, Long?> {
        val local = desktopAudioFileFor(url)
        if (local.exists() && local.length() > 0L) {
            return local.inputStream() to local.length()
        }
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        val length = connection.contentLengthLong.takeIf { it > 0L }
        return connection.getInputStream() to length
    }

    private fun updateDuration(stream: AudioInputStream, format: AudioFormat, contentLength: Long?) {
        val frames = stream.frameLength
        val rate = format.frameRate
        totalDurationMs = if (frames > 0L && rate > 0f) {
            (frames / rate * 1000.0).toLong()
        } else {
            estimateDurationMs(format, contentLength)
        }
    }

    private fun estimateDurationMs(format: AudioFormat, contentLength: Long?): Long {
        val nominalBitrate = (format.getProperty("mp3.bitrate.nominal.bps") as? Number)?.toDouble() ?: 0.0
        if (nominalBitrate <= 0.0 || contentLength == null || contentLength <= 0L) return 0L
        return (contentLength * 8.0 / nominalBitrate * 1000.0).toLong()
    }

    private fun skipDecoded(stream: AudioInputStream, format: AudioFormat, ms: Long, myGeneration: Int) {
        var remaining = msToBytes(ms, format)
        if (remaining <= 0L) return
        val buffer = ByteArray(READ_BUFFER_BYTES)
        while (remaining > 0L && !cancelled(myGeneration)) {
            val skipped = runCatching { stream.skip(remaining) }.getOrDefault(0L)
            if (skipped > 0L) {
                remaining -= skipped
                continue
            }
            val read = stream.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (read <= 0) break
            remaining -= read
        }
    }

    private fun updateProgress(decodedBytes: Long, format: AudioFormat, myGeneration: Int) {
        if (myGeneration != generation) return
        currentPositionMs = bytesToMs(decodedBytes, format)
        val total = totalDurationMs
        progressValue = if (total > 0L) {
            (currentPositionMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    private fun msToBytes(ms: Long, format: AudioFormat): Long {
        val frameSize = format.frameSize
        val frameRate = format.frameRate
        if (ms <= 0L || frameSize <= 0 || frameRate <= 0f) return 0L
        return (ms / 1000.0 * frameRate).toLong() * frameSize
    }

    private fun bytesToMs(bytes: Long, format: AudioFormat): Long {
        val frameSize = format.frameSize
        val frameRate = format.frameRate
        if (bytes <= 0L || frameSize <= 0 || frameRate <= 0f) return 0L
        val frames = bytes / frameSize
        return (frames / frameRate * 1000.0).toLong()
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
