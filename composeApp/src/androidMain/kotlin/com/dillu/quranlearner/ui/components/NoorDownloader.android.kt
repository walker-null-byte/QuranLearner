package com.dillu.quranlearner.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.*

actual class NoorDownloader(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    actual fun download(urls: List<String>, onProgress: (Float) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                val cache = AudioCacheManager.getInstance(context)
                val upstreamFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                
                val cacheDataSource = cacheDataSourceFactory.createDataSource()

                urls.forEachIndexed { index, url ->
                    val dataSpec = DataSpec.Builder().setUri(url).build()
                    val cacheWriter = CacheWriter(
                        cacheDataSource,
                        dataSpec,
                        null,
                        null
                    )
                    cacheWriter.cache()
                    
                    withContext(Dispatchers.Main) {
                        onProgress((index + 1).toFloat() / urls.size)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Download failed")
                }
            }
        }
    }
}

@Composable
actual fun rememberNoorDownloader(): NoorDownloader {
    val context = LocalContext.current
    return remember { NoorDownloader(context) }
}
