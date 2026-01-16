package com.metaplayer.iptv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.metaplayer.iptv.data.api.ApiClient

class MetaPlayerApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Configure Coil to use our shared OkHttpClient with DNS-over-HTTPS.
     * This fixes "Unable to resolve host" errors for movie thumbnails (tmdb.org, etc).
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { ApiClient.okHttpClient }
            .respectCacheHeaders(false) // Sometimes IPTV servers have weird cache headers
            .build()
    }
}
