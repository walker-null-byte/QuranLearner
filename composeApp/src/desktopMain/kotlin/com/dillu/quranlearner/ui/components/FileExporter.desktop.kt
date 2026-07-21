package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class FileExporter(private val onDirectoryPicked: (String) -> Unit) {
    actual fun pickDirectory() {
        // Stub for desktop
    }

    actual fun exportSurahs(
        surahNumbers: List<Int>,
        folderUriString: String,
        reciterFolder: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        onComplete()
    }
}

@Composable
actual fun rememberFileExporter(onDirectoryPicked: (String) -> Unit): FileExporter {
    return remember { FileExporter(onDirectoryPicked) }
}
