package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/** Root folder where desktop audio downloads are cached. Shared with [AudioPlayer]. */
internal fun desktopAudioDir(): File =
    File(File(System.getProperty("user.home"), ".quranlearner"), "audio")

/**
 * Deterministic URL → local file mapping (host + path, sanitized). Shared by the
 * downloader, the exporter and the audio player so a downloaded file is reused for
 * playback and export instead of being fetched again.
 */
internal fun desktopAudioFileFor(url: String): File {
    val withoutScheme = url.substringAfter("://", url)
    val safeName = buildString {
        for (ch in withoutScheme) {
            append(if (ch.isLetterOrDigit() || ch == '.' || ch == '-' || ch == '_') ch else '_')
        }
    }
    val fileName = if (safeName.length > 200) safeName.takeLast(200) else safeName
    return File(desktopAudioDir(), fileName)
}

/** Streams [url] into [target], writing to a temp file first so a failure never leaves a partial file. */
internal fun downloadToFile(url: String, target: File) {
    target.parentFile?.mkdirs()
    val temp = File(target.parentFile, target.name + ".part")
    try {
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.getInputStream().use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    } catch (t: Throwable) {
        temp.delete()
        throw t
    }
}

actual class NoorDownloader {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    actual fun download(urls: List<String>, onProgress: (Float) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                if (urls.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }
                desktopAudioDir().mkdirs()
                urls.forEachIndexed { index, url ->
                    val target = desktopAudioFileFor(url)
                    if (!target.exists() || target.length() == 0L) {
                        downloadToFile(url, target)
                    }
                    withContext(Dispatchers.Main) {
                        onProgress((index + 1).toFloat() / urls.size.toFloat())
                    }
                }
                withContext(Dispatchers.Main) { onComplete() }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    onError(t.message ?: "Download failed")
                }
            }
        }
    }
}

@Composable
actual fun rememberNoorDownloader(): NoorDownloader = remember { NoorDownloader() }
