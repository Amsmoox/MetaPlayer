package com.metaplayer.iptv.data.repository

import android.content.Context
import android.content.SharedPreferences

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("metaplayer_favorites", Context.MODE_PRIVATE)

    fun isFavorite(channelUrl: String): Boolean {
        return prefs.getBoolean(channelUrl, false)
    }

    fun toggleFavorite(channelUrl: String) {
        val current = isFavorite(channelUrl)
        prefs.edit().putBoolean(channelUrl, !current).apply()
    }

    fun getFavorites(): Set<String> {
        return prefs.all.filterValues { it == true }.keys
    }
}
