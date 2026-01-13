package com.metaplayer.iptv.data.repository

import android.content.Context
import com.metaplayer.iptv.data.api.ApiClient
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.parser.M3UParser
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository for loading M3U playlists.
 * Downloads M3U from backend and parses locally (like IBO Player, SMART IPTV).
 */
class PlaylistRepository(
    private val context: Context,
    private val parser: M3UParser = M3UParser()
) {
    private val api = ApiClient.api
    
    /**
     * Load playlist from backend API (downloads M3U and parses locally).
     */
    suspend fun loadPlaylistFromBackend(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val macAddress = DeviceManager.getMacAddress(context)
            val response = api.getPlaylist(macAddress, refresh = false)
            
            if (response.isSuccessful && response.body() != null) {
                val m3uContent = response.body()!!.string()
                val channels = parser.parseFromString(m3uContent)
                Result.success(channels)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to get playlist: $errorBody"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP error: ${e.code()} - ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load playlist from direct URL (for backward compatibility).
     */
    suspend fun loadPlaylist(url: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = parser.parseFromUrl(url)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load playlist from M3U string content.
     */
    suspend fun loadPlaylistFromString(content: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = parser.parseFromString(content)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
