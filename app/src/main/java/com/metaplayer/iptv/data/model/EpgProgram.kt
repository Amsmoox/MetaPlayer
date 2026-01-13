package com.metaplayer.iptv.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EpgProgram(
    val channelId: String,
    val start: LocalDateTime,
    val stop: LocalDateTime,
    val title: String,
    val description: String? = null
) {
    fun isCurrent(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(start) && now.isBefore(stop)
    }

    fun getFormattedTimeRange(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return "${start.format(formatter)} - ${stop.format(formatter)}"
    }
}
