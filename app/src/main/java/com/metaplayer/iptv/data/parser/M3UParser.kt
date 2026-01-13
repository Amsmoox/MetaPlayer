package com.metaplayer.iptv.data.parser

import com.metaplayer.iptv.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

class M3UParser {
    
    fun parseFromUrl(url: String): List<Channel> {
        val inputStream = URL(url).openStream()
        return parse(inputStream)
    }

    /**
     * Re-adding the missing parseFromString method.
     */
    fun parseFromString(content: String): List<Channel> {
        return content.byteInputStream().use { parse(it) }
    }

    private fun parse(inputStream: InputStream): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        
        var line: String?
        var currentChannel: ChannelBuilder? = null
        
        while (reader.readLine().also { line = it } != null) {
            line?.let { currentLine ->
                when {
                    currentLine.startsWith("#EXTINF:") -> {
                        currentChannel = parseExtInf(currentLine)
                    }
                    currentLine.isNotBlank() && !currentLine.startsWith("#") -> {
                        currentChannel?.let { builder ->
                            channels.add(builder.copy(url = currentLine.trim()).build())
                            currentChannel = null
                        }
                    }
                    else -> {
                        // Ignore other lines (header, comments, etc.)
                    }
                }
            }
        }
        reader.close()
        return channels
    }

    private fun parseExtInf(line: String): ChannelBuilder {
        val builder = ChannelBuilder()
        val attributes = mutableMapOf<String, String>()
        
        // Match attributes like tvg-id="...", group-title="..." case-insensitively
        val attributeRegex = """([\w-]+)="([^"]*)"""".toRegex()
        attributeRegex.findAll(line).forEach { matchResult ->
            val (key, value) = matchResult.destructured
            attributes[key.lowercase()] = value // Store as lowercase
        }
        
        val nameStart = line.lastIndexOf(',')
        val channelName = if (nameStart != -1) line.substring(nameStart + 1).trim() else ""
        
        builder.name = channelName
        builder.tvgId = attributes["tvg-id"]
        builder.tvgName = attributes["tvg-name"] ?: channelName
        builder.logo = attributes["tvg-logo"] ?: attributes["logo"]
        builder.group = attributes["group-title"]
        
        return builder
    }

    private data class ChannelBuilder(
        var name: String = "",
        var url: String = "",
        var logo: String? = null,
        var group: String? = null,
        var tvgId: String? = null,
        var tvgName: String? = null
    ) {
        fun build() = Channel(
            name = name,
            url = url,
            logo = logo,
            group = group,
            tvgId = if (tvgId.isNullOrBlank()) null else tvgId,
            tvgName = tvgName ?: name
        )
    }
}
