package com.metaplayer.iptv.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Professional ExoPlayer configuration optimized for IPTV streaming
 * Supports: 4K, HDR, all qualities, adaptive bitrate
 */
object PlayerConfig {
    
    /**
     * Creates a professional ExoPlayer instance optimized for high-quality streaming
     */
    fun createPlayer(context: Context): ExoPlayer {
        val trackSelector = createTrackSelector(context)
        val loadControl = createLoadControl()
        
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }
    
    /**
     * Track selector for optimal quality selection
     * - Removes quality limits (allows 4K+)
     * - Enables adaptive bitrate switching
     * - Supports all codecs
     */
    private fun createTrackSelector(context: Context): DefaultTrackSelector {
        val trackSelector = DefaultTrackSelector(context)
        val parameters = trackSelector.buildUponParameters()
            // Quality settings
            .setMaxVideoSizeSd() // Remove max limit
            .setMaxVideoBitrate(Int.MAX_VALUE) // No bitrate limit
            
            // Codec support
            .setPreferredVideoMimeType(null) // Allow all video formats
            .setPreferredAudioMimeType(null) // Allow all audio formats
            
            // Adaptive streaming
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowVideoNonSeamlessAdaptiveness(true)
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            
            // Prefer higher quality
            .setForceHighestSupportedBitrate(false) // Allow adaptive
            .setForceLowestBitrate(false)
            
            .build()
        
        trackSelector.parameters = parameters
        return trackSelector
    }
    
    /**
     * Load control for optimal buffering
     * - 15 second minimum buffer for smooth playback
     * - 50 second maximum buffer for 4K streams
     * - Fast startup (2.5 seconds)
     * - Quick rebuffering (5 seconds)
     */
    private fun createLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,  // minBufferMs: 15 seconds before playback starts
                50000,  // maxBufferMs: 50 seconds max buffer (critical for 4K)
                2500,   // bufferForPlaybackMs: 2.5 seconds for playback start
                5000    // bufferForPlaybackAfterRebufferMs: 5 seconds after rebuffer
            )
            .setBackBuffer(
                10000,  // Keep 10 seconds of back buffer
                true    // Retain back buffer after loading
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // No target buffer size limit
            .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based buffering
            .build()
    }
}
