package com.metaplayer.iptv.data.util

import com.metaplayer.iptv.data.model.ChannelCategory

/**
 * Detects channel category based on URL, group title, and channel name
 * Priority: URL patterns > Group-title > Channel name
 */
object CategoryDetector {
    
    /**
     * Detects category from URL, group, and name
     */
    fun detectCategory(url: String?, group: String?, name: String): ChannelCategory {
        val urlLower = url?.lowercase() ?: ""
        val groupLower = group?.lowercase() ?: ""
        val nameLower = name.lowercase()
        val searchText = "$groupLower $nameLower"
        
        // Priority 1: Check URL patterns (most reliable)
        return when {
            // Adult content check (before VOD checks)
            isAdult(urlLower, groupLower, nameLower) -> ChannelCategory.ADULT
            
            // URL-based detection
            urlLower.contains("/series/") -> ChannelCategory.SERIES
            urlLower.contains("/movie/") -> ChannelCategory.MOVIES
            hasVideoExtension(urlLower) -> {
                // VOD content with file extension - check if series or movie
                if (isSeriesPattern(nameLower, groupLower)) {
                    ChannelCategory.SERIES
                } else {
                    ChannelCategory.MOVIES
                }
            }
            
            // No video extension = LIVE TV
            !hasVideoExtension(urlLower) && urlLower.isNotBlank() -> ChannelCategory.LIVE_TV
            
            // Priority 2: Check group-title patterns
            groupLower.contains("vod") || groupLower.contains("movie") || groupLower.contains("film") -> {
                if (isSeriesPattern(nameLower, groupLower)) ChannelCategory.SERIES else ChannelCategory.MOVIES
            }
            groupLower.contains("series") || groupLower.contains("show") -> ChannelCategory.SERIES
            groupLower.startsWith("mu|") -> {
                // MU| prefix usually indicates VOD
                if (isSeriesPattern(nameLower, groupLower)) ChannelCategory.SERIES else ChannelCategory.MOVIES
            }
            
            // Priority 3: Fallback to name patterns
            isSeriesPattern(nameLower, groupLower) -> ChannelCategory.SERIES
            isMoviePattern(nameLower, groupLower) -> ChannelCategory.MOVIES
            isLiveTV(searchText) -> ChannelCategory.LIVE_TV
            
            else -> ChannelCategory.OTHER
        }
    }
    
    private fun hasVideoExtension(url: String): Boolean {
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".m4v", ".mpg", ".mpeg", ".ts"
        )
        return videoExtensions.any { url.endsWith(it) }
    }
    
    private fun isAdult(url: String, group: String, name: String): Boolean {
        val adultKeywords = listOf(
            "adult", "xxx", "porn", "18+", "18 plus", "erotic", "sex",
            "nsfw", "mature", "adults only"
        )
        return adultKeywords.any { 
            url.contains(it) || group.contains(it) || name.contains(it) 
        }
    }
    
    private fun isSeriesPattern(name: String, group: String): Boolean {
        // Check for episode patterns: S01 E01, S1 E1, Season 1 Episode 1, etc.
        val episodePatterns = listOf(
            "s\\d+\\s*e\\d+",           // S01 E01, S1 E1
            "season\\s*\\d+",           // Season 1
            "episode\\s*\\d+",          // Episode 1
            "ep\\s*\\d+",               // EP 01
            "saison\\s*\\d+",          // Saison 1 (French)
            "episodio\\s*\\d+"         // Episodio 1 (Spanish)
        )
        val combined = "$name $group"
        return episodePatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(combined)
        }
    }
    
    private fun isMoviePattern(name: String, group: String): Boolean {
        val movieKeywords = listOf(
            "movie", "film", "cinema", "feature"
        )
        val combined = "$name $group"
        return movieKeywords.any { combined.contains(it) } && !isSeriesPattern(name, group)
    }
    
    private fun isLiveTV(text: String): Boolean {
        val liveKeywords = listOf(
            "live",
            "tv",
            "television",
            "channel",
            "channels",
            "broadcast",
            "streaming",
            "iptv",
            "sport",
            "sports",
            "news",
            "music",
            "radio",
            "hd",
            "fhd",
            "4k",
            "uhd",
            "bein",
            "sky",
            "espn",
            "cnn",
            "bbc",
            "fox"
        )
        return liveKeywords.any { text.contains(it) }
    }
}
