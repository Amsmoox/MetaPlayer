package com.metaplayer.iptv.data.repository

import android.content.Context
import android.util.Log
import com.metaplayer.iptv.data.api.ApiClient
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.parser.M3UParser
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class PlaylistRepository(
    context: Context,
    private val parser: M3UParser = M3UParser()
) {
    private val appContext = context.applicationContext
    private val api = ApiClient.api
    private val CACHE_FILE_NAME = "playlist_cache.m3u"
    private val TAG = "PlaylistRepository"
    
    // Reuse the global OkHttpClient from ApiClient which has DNS-over-HTTPS configured
    private val httpClient = ApiClient.okHttpClient
    
    suspend fun loadPlaylistStreaming(
        forceRefresh: Boolean,
        onProgress: (Float) -> Unit,
        onChannelsUpdated: (List<Channel>) -> Unit
    ): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
        
        if (forceRefresh) {
            clearCache()
        }

        // 1. If not forcing refresh and cache exists, parse cache streaming-style
        if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val channels = cacheFile.inputStream().use { stream ->
                    parser.parseStreaming(stream, cacheFile.length(), onProgress, onChannelsUpdated)
                }
                if (channels.isNotEmpty()) return@withContext Result.success(channels)
            } catch (e: Exception) {
                cacheFile.delete() 
            }
        }

        // 2. Fetch from Network
        try {
            val macAddress = DeviceManager.getMacAddress(appContext)
            val response = api.getPlaylistUrl(macAddress, refresh = forceRefresh)
            
            if (response.isSuccessful && response.body() != null) {
                val m3uUrl = response.body()!!.m3u_url
                Log.d(TAG, "🌐 FETCHING M3U FROM: $m3uUrl")
                
                val request = Request.Builder()
                    .url(m3uUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .build()
                
                val downloadResponse = httpClient.newCall(request).execute()
                if (!downloadResponse.isSuccessful) return@withContext Result.failure(Exception("Download Error: ${downloadResponse.code}"))

                val body = downloadResponse.body ?: return@withContext Result.failure(Exception("Empty body"))
                val contentLength = body.contentLength()

                // We write to a temporary file while parsing to update the cache
                val tempFile = File(appContext.filesDir, "${CACHE_FILE_NAME}.tmp")
                
                // We use a custom stream that both writes to file and provides data to parser
                val channels = body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val teeInputStream = object : java.io.InputStream() {
                            override fun read(): Int {
                                val b = input.read()
                                if (b != -1) output.write(b)
                                return b
                            }
                            override fun read(b: ByteArray, off: Int, len: Int): Int {
                                val n = input.read(b, off, len)
                                if (n != -1) output.write(b, off, n)
                                return n
                            }
                        }
                        parser.parseStreaming(teeInputStream, contentLength, onProgress, onChannelsUpdated)
                    }
                }
                
                tempFile.renameTo(cacheFile)
                Result.success(channels)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ DNS ERROR: Cannot resolve hostname - ${e.message}")
            Result.failure(Exception("Network Error: Cannot reach the IPTV server. Please check your internet connection or try using a VPN. Error: ${e.message}"))
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ TIMEOUT ERROR: ${e.message}")
            Result.failure(Exception("Network Error: Connection timeout. The server is taking too long to respond. Please try again."))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "❌ IO ERROR: ${e.message}")
            Result.failure(Exception("Network Error: ${e.message}. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR: ${e.message}", e)
            Result.failure(Exception("Error loading playlist: ${e.message}"))
        }
    }

    fun clearCache() {
        val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) cacheFile.delete()
    }
}
