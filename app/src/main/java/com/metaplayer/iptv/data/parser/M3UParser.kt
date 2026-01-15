package com.metaplayer.iptv.data.parser

import com.metaplayer.iptv.data.model.Channel
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

class M3UParser {
    
    private val groupCache = mutableMapOf<String, String>()

    fun parseFromUrl(url: String): List<Channel> {
        return URL(url).openStream().use { parse(it) }
    }

    fun parseFromString(content: String): List<Channel> {
        return content.byteInputStream().use { parse(it) }
    }

    fun parseFromFile(file: File): List<Channel> {
        groupCache.clear()
        return file.inputStream().use { parse(it) }
    }

    private fun parse(inputStream: InputStream): List<Channel> {
        val channels = ArrayList<Channel>(50000) // Pre-allocate for better performance
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768) // Larger buffer
        
        var line: String?
        var currentInfo: TempInfo? = null
        
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            if (currentLine.isEmpty()) continue

            if (currentLine.startsWith("#EXTINF:")) {
                currentInfo = fastParseExtInf(currentLine)
            } else if (!currentLine.startsWith("#")) {
                if (currentInfo != null) {
                    channels.add(
                        Channel(
                            name = currentInfo.name,
                            url = currentLine.trim(),
                            logo = currentInfo.logo,
                            group = currentInfo.group,
                            tvgId = currentInfo.tvgId,
                            tvgName = currentInfo.tvgName
                        )
                    )
                    currentInfo = null
                }
            }
        }
        groupCache.clear() 
        return channels
    }

    /**
     * ULTRA FAST PARSER: Scans string directly without Regex or Map creation.
     */
    private fun fastParseExtInf(line: String): TempInfo {
        var tvgId: String? = null
        var tvgName: String? = null
        var logo: String? = null
        var group: String? = null

        // Extract attributes by manual scanning
        tvgId = extractAttribute(line, "tvg-id=\"")
        tvgName = extractAttribute(line, "tvg-name=\"")
        logo = extractAttribute(line, "tvg-logo=\"") ?: extractAttribute(line, "logo=\"")
        
        val rawGroup = extractAttribute(line, "group-title=\"") ?: "OTHER"
        group = groupCache.getOrPut(rawGroup) { rawGroup }

        // Channel Name is everything after the last comma
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
