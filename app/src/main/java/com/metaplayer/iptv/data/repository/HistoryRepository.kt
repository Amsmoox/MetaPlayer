package com.metaplayer.iptv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("metaplayer_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxHistory = 30

    fun getHistory(): List<String> {
        val json = prefs.getString("watched_urls", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addToHistory(channelUrl: String) {
        val currentHistory = getHistory().toMutableList()
        
        // Remove if exists to move to top
        currentHistory.remove(channelUrl)
        
        // Add to top
        currentHistory.add(0, channelUrl)
        
        // Trim to max size
        val trimmedHistory = if (currentHistory.size > maxHistory) {
            currentHistory.take(maxHistory)
        } else {
            currentHistory
        }
        
        prefs.edit().putString("watched_urls", gson.toJson(trimmedHistory)).apply()
    }
}
