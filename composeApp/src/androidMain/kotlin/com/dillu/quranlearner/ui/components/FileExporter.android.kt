package com.dillu.quranlearner.ui.components

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.*

actual class FileExporter(
    private val context: Context,
    private val launchPicker: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    actual fun pickDirectory() {
        launchPicker()
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
                val cache = AudioCacheManager.getInstance(context)
                val upstreamFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .createDataSource()
                
                val recitersMap = mapOf(
                    "Alafasy_128kbps" to "https://server8.mp3quran.net/afs/",
                    "Husary_128kbps" to "https://server13.mp3quran.net/husr/",
                    "Minshawy_Murattal_128kbps" to "https://server10.mp3quran.net/minsh/",
                    "Abdul_Basit_Murattal_192kbps" to "https://server7.mp3quran.net/basit/",
                    "Abdurrahmaan_As-Sudais_192kbps" to "https://server11.mp3quran.net/sds/",
                    "Saood_ash-Shuraym_128kbps" to "https://server7.mp3quran.net/shrm/",
                    "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net" to "https://server10.mp3quran.net/ajm/",
                    "MauroAl-Muaiqly128kbps" to "https://server12.mp3quran.net/maher/"
                )
                val baseUrl = recitersMap[reciterFolder] ?: "https://server8.mp3quran.net/afs/"

                surahNumbers.forEachIndexed { index, surahNum ->
                    val surahNumStr = surahNum.toString().padStart(3, '0')
                    val url = "$baseUrl$surahNumStr.mp3"
                    val dataSpec = DataSpec.Builder().setUri(url).build()

                    val treeUri = Uri.parse(folderUriString)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    
                    // First check if file already exists (simplistic check to avoid duplicate creation)
                    // It's safer to always create a new one, it will auto-append (1).
                    val newDocUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        docUri,
                        "audio/mpeg",
                        "Surah_${surahNumStr}.mp3"
                    )
                    
                    if (newDocUri != null) {
                        context.contentResolver.openOutputStream(newDocUri)?.use { out ->
                            cacheDataSource.open(dataSpec)
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = cacheDataSource.read(buffer, 0, buffer.size)
                                if (read == androidx.media3.common.C.RESULT_END_OF_INPUT) break
                                out.write(buffer, 0, read)
                            }
                            cacheDataSource.close()
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        onProgress((index + 1).toFloat() / surahNumbers.size)
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Export failed")
                }
            }
        }
    }
}

@Composable
actual fun rememberFileExporter(onDirectoryPicked: (String) -> Unit): FileExporter {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            onDirectoryPicked(uri.toString())
        }
    }
    return remember { FileExporter(context) { launcher.launch(null) } }
}
