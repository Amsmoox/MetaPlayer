package com.metaplayer.iptv.data.parser

import android.util.Log
import com.metaplayer.iptv.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class M3UParser {
    
    private val groupCache = mutableMapOf<String, String>()
    private val TAG = "M3UParser"

    fun parseStreaming(
        inputStream: InputStream,
        totalBytes: Long,
        onProgress: (Float) -> Unit,
        onChannelsUpdate: (List<Channel>) -> Unit
    ): List<Channel> {
        val channels = ArrayList<Channel>(50000)
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768)
        
        var line: String?
        var currentInfo: TempInfo? = null
        var bytesRead: Long = 0
        var lastUpdate = 0L
        
        Log.d(TAG, "Starting streaming parse...")
        
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            bytesRead += currentLine.length + 1
            
            if (currentLine.isEmpty()) continue

            if (currentLine.startsWith("#EXTINF:")) {
                currentInfo = fastParseExtInf(currentLine)
            } else if (!currentLine.startsWith("#")) {
                if (currentInfo != null) {
                    val channel = Channel(
                        name = currentInfo.name,
                        url = currentLine.trim(),
                        logo = currentInfo.logo,
                        group = currentInfo.group,
                        tvgId = currentInfo.tvgId,
                        tvgName = currentInfo.tvgName,
                        tvgLogo = currentInfo.logo // Sync tvgLogo with logo field
                    )
                    channels.add(channel)
                    
                    // Log first few channels to verify logos
                    if (channels.size <= 5) {
                        Log.d(TAG, "Parsed Channel: ${channel.name}, Logo: ${channel.logo}")
                    }
                    
                    currentInfo = null
                    
                    if (channels.size % 500 == 0) {
                        onChannelsUpdate(ArrayList(channels))
                    }
                }
            }
            
            if (totalBytes > 0 && bytesRead - lastUpdate > 102400) {
                onProgress(bytesRead.toFloat() / totalBytes.toFloat())
                lastUpdate = bytesRead
            }
        }
        
        Log.d(TAG, "Parse complete. Total channels: ${channels.size}")
        onProgress(1.0f)
        onChannelsUpdate(channels)
        groupCache.clear() 
        return channels
    }

    private fun fastParseExtInf(line: String): TempInfo {
        var tvgId: String? = null
        var tvgName: String? = null
        var logo: String? = null
        var group: String? = null

        tvgId = extractAttribute(line, "tvg-id=\"")
        tvgName = extractAttribute(line, "tvg-name=\"")
        logo = extractAttribute(line, "tvg-logo=\"") ?: extractAttribute(line, "logo=\"")
        
        val rawGroup = extractAttribute(line, "group-title=\"") ?: "OTHER"
        group = groupCache.getOrPut(rawGroup) { rawGroup }

        val commaIndex = line.lastIndexOf(',')
        val channelName = if (commaIndex != -1 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            ""
        }

        return TempInfo(
            name = channelName,
            tvgId = if (tvgId.isNullOrBlank()) null else tvgId,
            tvgName = tvgName ?: channelName,
            logo = logo,
            group = group
        )
    }

    private fun extractAttribute(line: String, key: String): String? {
        val start = line.indexOf(key)
        if (start == -1) return null
        val valueStart = start + key.length
        val end = line.indexOf('"', valueStart)
        if (end == -1) return null
        return line.substring(valueStart, end)
    }

    private data class TempInfo(
        val name: String,
        val tvgId: String?,
        val tvgName: String,
        val logo: String?,
        val group: String?
    )
}
