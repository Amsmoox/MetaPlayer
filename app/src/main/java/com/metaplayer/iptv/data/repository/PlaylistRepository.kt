package com.metaplayer.iptv.data.repository

import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(private val parser: M3UParser = M3UParser()) {
    
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
