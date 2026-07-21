package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable

expect class FileExporter {
    fun pickDirectory()
    fun exportSurahs(
        surahNumbers: List<Int>,
        folderUriString: String,
        reciterFolder: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )
}

@Composable
expect fun rememberFileExporter(onDirectoryPicked: (String) -> Unit): FileExporter
