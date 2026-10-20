package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dillu.quranlearner.ui.screens.fullSurahServerFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual class FileExporter(private val onDirectoryPicked: (String) -> Unit) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    actual fun pickDirectory() {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.dialogTitle = "Choose export folder"
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.absolutePath?.let(onDirectoryPicked)
            }
        }
    }

    actual fun exportSurahs(
        surahNumbers: List<Int>,
        folderUriString: String,
        reciterFolder: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                if (folderUriString.isBlank()) {
                    withContext(Dispatchers.Main) { onError("Export folder is not set") }
                    return@launch
                }
                val targetDir = File(folderUriString)
                if (!targetDir.isDirectory) {
                    withContext(Dispatchers.Main) { onError("Export folder is not available") }
                    return@launch
                }
                if (surahNumbers.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }

                val baseUrl = fullSurahServerFor(reciterFolder)
                surahNumbers.forEachIndexed { index, surahNum ->
                    val surahNumStr = surahNum.toString().padStart(3, '0')
                    val url = "$baseUrl$surahNumStr.mp3"
                    val source = desktopAudioFileFor(url)
                    if (!source.exists() || source.length() == 0L) {
                        downloadToFile(url, source)
                    }
                    source.copyTo(File(targetDir, "Surah_$surahNumStr.mp3"), overwrite = true)
                    withContext(Dispatchers.Main) {
                        onProgress((index + 1).toFloat() / surahNumbers.size.toFloat())
                    }
                }
                withContext(Dispatchers.Main) { onComplete() }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) { onError(t.message ?: "Export failed") }
            }
        }
    }
}

@Composable
actual fun rememberFileExporter(onDirectoryPicked: (String) -> Unit): FileExporter {
    return remember { FileExporter(onDirectoryPicked) }
}
