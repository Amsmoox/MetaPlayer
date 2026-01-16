package com.metaplayer.iptv.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.metaplayer.iptv.data.api.ApiClient

/**
 * Professional ExoPlayer configuration optimized for IPTV streaming
 */
object PlayerConfig {
    
    fun createPlayer(context: Context): ExoPlayer {
        val trackSelector = createTrackSelector(context)
        val loadControl = createLoadControl()
        
        // Use our shared OkHttpClient with DNS-over-HTTPS for video playback
        val dataSourceFactory = OkHttpDataSource.Factory(ApiClient.okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }
    
    private fun createTrackSelector(context: Context): DefaultTrackSelector {
        val trackSelector = DefaultTrackSelector(context)
        val parameters = trackSelector.buildUponParameters()
            .setMaxVideoSizeSd()
            .setMaxVideoBitrate(Int.MAX_VALUE)
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowVideoNonSeamlessAdaptiveness(true)
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            .build()
        
        trackSelector.parameters = parameters
        return trackSelector
    }
    
    private fun createLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, 
                50000, 
                2500,  
                5000   
            )
            .setBackBuffer(
                10000, 
                true   
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
}
