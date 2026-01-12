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
                    currentLine.startsWith("#EXTM3U") -> {
                        // M3U header, continue
                    }
                    currentLine.startsWith("#EXTINF:") -> {
                        // Parse channel info
                        currentChannel = parseExtInf(currentLine)
                    }
                    currentLine.isNotBlank() && !currentLine.startsWith("#") -> {
                        // This is the stream URL
                        currentChannel?.let { builder ->
                            channels.add(
                                builder.copy(url = currentLine.trim()).build()
                            )
                            currentChannel = null
                        }
                    }
                    else -> {
                        // Other lines, ignore
                    }
                }
            }
        }
        
        reader.close()
        return channels
    }

    private fun parseExtInf(line: String): ChannelBuilder {
        // Format: #EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Channel Name
        val builder = ChannelBuilder()
        
        // Extract attributes
        val attributes = mutableMapOf<String, String>()
        val attributeRegex = """(\w+(?:-\w+)*)="([^"]*)"""".toRegex()
        attributeRegex.findAll(line).forEach { matchResult ->
            val (key, value) = matchResult.destructured
            attributes[key] = value
        }
        
        // Extract channel name (after the last comma)
        val nameStart = line.lastIndexOf(',')
        val channelName = if (nameStart != -1) {
            line.substring(nameStart + 1).trim()
        } else {
            ""
        }
        
        builder.name = channelName
        builder.tvgId = attributes["tvg-id"]
        builder.tvgName = attributes["tvg-name"]
        builder.tvgLogo = attributes["tvg-logo"]
        builder.logo = attributes["tvg-logo"] ?: attributes["logo"]
        builder.group = attributes["group-title"]
        builder.tvgShift = attributes["tvg-shift"]
        builder.radio = attributes["radio"]?.toBoolean() ?: false
        builder.catchup = attributes["catchup"]
        
        return builder
    }

    private data class ChannelBuilder(
        var name: String = "",
        var url: String = "",
        var logo: String? = null,
        var group: String? = null,
        var tvgId: String? = null,
        var tvgName: String? = null,
        var tvgLogo: String? = null,
        var tvgShift: String? = null,
        var radio: Boolean = false,
        var catchup: String? = null
    ) {
        fun build() = Channel(
            name = name,
            url = url,
            logo = logo,
            group = group,
            tvgId = tvgId,
            tvgName = tvgName ?: name,
            tvgLogo = tvgLogo,
            tvgShift = tvgShift,
            radio = radio,
            catchup = catchup
        )
    }
}
