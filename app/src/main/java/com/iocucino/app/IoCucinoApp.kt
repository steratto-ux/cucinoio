package com.iocucino.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.iocucino.app.data.WikiPhotos
import okhttp3.OkHttpClient

/** Imposta il caricatore delle foto: Wikimedia chiede che ogni richiesta dichiari chi la fa. */
class IoCucinoApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", WikiPhotos.USER_AGENT)
                        .build()
                )
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("foto"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
