package com.metaplayer.iptv.data.model

enum class ChannelCategory(
    val displayName: String,
    val colorHex: String
) {
    LIVE_TV("Live TV", "#6366F1"),      // Indigo
    MOVIES("Movies", "#10B981"),        // Emerald
    SERIES("Series", "#8B5CF6"),        // Purple
    ADULT("Adult", "#EF4444"),          // Red
    OTHER("Other", "#6B7280")           // Gray
}
