package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable

expect class NoorDownloader {
    fun download(urls: List<String>, onProgress: (Float) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit)
}

@Composable
expect fun rememberNoorDownloader(): NoorDownloader
