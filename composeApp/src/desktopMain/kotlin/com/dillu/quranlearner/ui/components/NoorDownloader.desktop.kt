package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class NoorDownloader {
    actual fun download(urls: List<String>, onProgress: (Float) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        // Stub for desktop
        onComplete()
    }
}

@Composable
actual fun rememberNoorDownloader(): NoorDownloader = remember { NoorDownloader() }
