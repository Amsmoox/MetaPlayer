package com.metaplayer.iptv.data.repository

import android.util.Log
import android.util.Xml
import com.metaplayer.iptv.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

class EpgRepository {

    private val epgCacheById = mutableMapOf<String, MutableList<EpgProgram>>()
    private val epgCacheByName = mutableMapOf<String, MutableList<EpgProgram>>()

    suspend fun fetchEpg(url: String) = withContext(Dispatchers.IO) {
        Log.d("EPG", "Fetching EPG from: $url")
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            var inputStream: InputStream = connection.getInputStream()

            if (url.endsWith(".gz") || url.contains("gz=1")) {
                inputStream = GZIPInputStream(inputStream)
            }

            parseXmlTv(inputStream)
            inputStream.close()
            Log.d("EPG", "EPG Loaded: ${epgCacheById.size} channels cached")
        } catch (e: Exception) {
            Log.e("EPG", "Failed to fetch EPG", e)
        }
    }

    private fun parseXmlTv(inputStream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        
        var currentChannelId: String? = null
        var startTime: LocalDateTime? = null
        var stopTime: LocalDateTime? = null
        var title: String? = null
        var desc: String? = null
        
        // Temp storage for channel names mapping
        val channelIdToName = mutableMapOf<String, String>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "channel" -> {
                            val id = parser.getAttributeValue(null, "id")
                            // Some XMLs have <display-name> inside <channel>
                        }
                        "display-name" -> {
                            // This would help mapping IDs to Names if needed
                        }
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            startTime = parseDate(parser.getAttributeValue(null, "start"))
                            stopTime = parseDate(parser.getAttributeValue(null, "stop"))
                            title = null
                            desc = null
                        }
                        "title" -> title = safeNextText(parser)
                        "desc" -> desc = safeNextText(parser)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "programme" && currentChannelId != null && startTime != null && stopTime != null && title != null) {
                        val program = EpgProgram(currentChannelId, startTime, stopTime, title, desc)
                        
                        // Cache by ID
                        epgCacheById.getOrPut(currentChannelId) { mutableListOf() }.add(program)
                        
                        // Reset for next
                        currentChannelId = null
                        startTime = null
                        stopTime = null
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun safeNextText(parser: XmlPullParser): String? {
        var result: String? = null
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag() // Move to end tag
        }
        return result
    }

    private fun parseDate(dateStr: String?): LocalDateTime? {
        if (dateStr == null) return null
        return try {
            val cleanDate = dateStr.split(" ")[0]
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            LocalDateTime.parse(cleanDate.take(14), formatter)
        } catch (e: Exception) {
            null
        }
    }

    fun getProgramsForChannel(tvgId: String?, tvgName: String?): List<EpgProgram> {
        val now = LocalDateTime.now()
        
        // 1. Try matching by TVG-ID
        var programs = tvgId?.let { epgCacheById[it] }
        
        // 2. Fallback to matching by Name (Normalized)
        if (programs == null && tvgName != null) {
            programs = epgCacheById[tvgName] 
        }

        return programs?.filter { it.stop.isAfter(now) }?.sortedBy { it.start } ?: emptyList()
    }
}
