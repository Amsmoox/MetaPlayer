package com.metaplayer.iptv.data.repository

import android.util.Log
import android.util.Xml
import com.metaplayer.iptv.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

class EpgRepository {

    private val _epgLoaded = MutableStateFlow(false)
    val epgLoaded = _epgLoaded.asStateFlow()

    private val epgCacheById = mutableMapOf<String, MutableList<EpgProgram>>()

    suspend fun fetchEpg(url: String) = withContext(Dispatchers.IO) {
        Log.d("EPG_DEBUG", "Starting EPG fetch from: $url")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 30000
                readTimeout = 30000
                // Use a professional User-Agent to avoid server blocks
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e("EPG_DEBUG", "Server returned error code: $responseCode")
                return@withContext
            }

            var inputStream: InputStream = connection.inputStream

            // Detection for Gzip by extension or response header
            if (url.endsWith(".gz") || url.contains("gz=1") || connection.contentEncoding?.contains("gzip") == true) {
                Log.d("EPG_DEBUG", "Decompressing GZIP EPG...")
                inputStream = GZIPInputStream(inputStream)
            }

            parseXmlTv(inputStream)
            inputStream.close()
            
            _epgLoaded.value = true
            Log.d("EPG_DEBUG", "Successfully loaded EPG for ${epgCacheById.size} channels")
        } catch (e: Exception) {
            Log.e("EPG_DEBUG", "Failed to fetch/parse EPG: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseXmlTv(inputStream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var startTime: LocalDateTime? = null
        var stopTime: LocalDateTime? = null
        var title: String? = null
        var desc: String? = null

        epgCacheById.clear()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            startTime = parseDate(parser.getAttributeValue(null, "start"))
                            stopTime = parseDate(parser.getAttributeValue(null, "stop"))
                        }
                        "title" -> title = parser.nextText()
                        "desc" -> desc = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "programme" && currentChannelId != null && title != null && startTime != null && stopTime != null) {
                        val program = EpgProgram(currentChannelId, startTime, stopTime, title, desc)
                        epgCacheById.getOrPut(currentChannelId) { mutableListOf() }.add(program)
                        
                        // Clear title for next check
                        title = null
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseDate(dateStr: String?): LocalDateTime? {
        if (dateStr == null) return null
        return try {
            // Standard XMLTV format: 20240520120000 +0200
            val cleanDate = dateStr.split(" ")[0]
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            LocalDateTime.parse(cleanDate.take(14), formatter)
        } catch (e: Exception) {
            null
        }
    }

    fun getProgramsForChannel(tvgId: String?, tvgName: String?): List<EpgProgram> {
        val now = LocalDateTime.now()
        
        // 1. Try matching by TVG-ID (Exact match from XML "channel" attribute)
        var programs = tvgId?.let { epgCacheById[it] }
        
        // 2. Fallback to matching by Channel Name if ID is missing (Common in ipprotv)
        if (programs == null && tvgName != null) {
            programs = epgCacheById[tvgName]
        }

        return programs?.filter { it.stop.isAfter(now) }?.sortedBy { it.start } ?: emptyList()
    }
}
