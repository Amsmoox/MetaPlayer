package com.metaplayer.iptv.data.repository

import android.content.Context
import android.util.Log
import com.metaplayer.iptv.data.api.ApiClient
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.parser.M3UParser
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PlaylistRepository(
    context: Context,
    private val parser: M3UParser = M3UParser()
) {
    private val appContext = context.applicationContext
    private val api = ApiClient.api
    private val CACHE_FILE_NAME = "playlist_cache.m3u"
    private val TAG = "PlaylistRepository"
    
    suspend fun loadPlaylistFromBackend(forceRefresh: Boolean = false): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
        
        Log.d(TAG, "=== Starting Playlist Load ===")
        Log.d(TAG, "Target File: ${cacheFile.absolutePath}")
        Log.d(TAG, "Parameters -> forceRefresh: $forceRefresh, File Exists: ${cacheFile.exists()}, File Size: ${cacheFile.length()} bytes")

        // 1. Try Cache First
        if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
            try {
                Log.d(TAG, "✅ Attempting to load from CACHE...")
                val channels = parser.parseFromFile(cacheFile)
                if (channels.isNotEmpty()) {
                    Log.d(TAG, "🚀 CACHE SUCCESS: Loaded ${channels.size} channels. SKIPPING NETWORK.")
                    return@withContext Result.success(channels)
                } else {
                    Log.w(TAG, "⚠️ CACHE WARNING: File is empty or has no valid channels.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ CACHE ERROR: Parsing failed. Deleting cache.", e)
                cacheFile.delete() 
            }
        } else {
            val reason = when {
                forceRefresh -> "Force refresh requested by user"
                !cacheFile.exists() -> "Cache file does not exist"
                cacheFile.length() == 0L -> "Cache file is 0 bytes"
                else -> "Unknown"
            }
            Log.d(TAG, "ℹ️ Skipping cache. Reason: $reason")
        }

        // 2. Fetch from Network
        Log.d(TAG, "🌐 NETWORK: Fetching latest playlist from API...")
        try {
            val macAddress = DeviceManager.getMacAddress(appContext)
            val response = api.getPlaylist(macAddress, refresh = forceRefresh)
            
            if (response.isSuccessful && response.body() != null) {
                val tempFile = File(appContext.filesDir, "${CACHE_FILE_NAME}.tmp")
                Log.d(TAG, "🌐 NETWORK: Downloading data to temporary file...")
                
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                        try { output.fd.sync() } catch (e: Exception) {} 
                    }
                }
                
                Log.d(TAG, "🌐 NETWORK: Download complete. Atomic swap to final cache file.")
                if (tempFile.renameTo(cacheFile)) {
                    Log.d(TAG, "✅ CACHE UPDATED: New size is ${cacheFile.length()} bytes.")
                } else {
                    tempFile.copyTo(cacheFile, overwrite = true)
                    tempFile.delete()
                }
                
                val channels = parser.parseFromFile(cacheFile)
                Log.d(TAG, "🚀 NETWORK SUCCESS: Loaded ${channels.size} channels.")
                Result.success(channels)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "❌ API ERROR: $errorBody")
                Result.failure(Exception("Failed to get playlist: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ NETWORK ERROR: ${e.message}")
            Result.failure(e)
        }
    }

    fun clearCache() {
        val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            cacheFile.delete()
            Log.d(TAG, "🗑️ CACHE CLEARED: Local playlist deleted.")
        }
    }
    
    suspend fun loadPlaylist(url: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = parser.parseFromUrl(url)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loadPlaylistFromString(content: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = parser.parseFromString(content)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
