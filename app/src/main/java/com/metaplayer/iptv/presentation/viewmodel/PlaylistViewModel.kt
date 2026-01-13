package com.metaplayer.iptv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.EpgProgram
import com.metaplayer.iptv.data.repository.DeviceRepository
import com.metaplayer.iptv.data.repository.EpgRepository
import com.metaplayer.iptv.data.repository.FavoritesRepository
import com.metaplayer.iptv.data.repository.HistoryRepository
import com.metaplayer.iptv.data.repository.PlaylistRepository
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val channels: List<Channel> = emptyList(),
    val favoriteUrls: Set<String> = emptySet(),
    val historyUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val macAddress: String = "",
    val deviceRegistered: Boolean = false,
    val m3uUrlFromBackend: String? = null,
    // NAVIGATION PERSISTENCE
    val lastSelectedGroup: String = "ALL CHANNELS",
    val lastSelectedChannel: Channel? = null
)

class PlaylistViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository(application)
    private val deviceRepository = DeviceRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val historyRepository = HistoryRepository(application)
    val epgRepository = EpgRepository()

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        val macAddress = DeviceManager.getMacAddress(application)
        _uiState.value = _uiState.value.copy(
            macAddress = macAddress,
            favoriteUrls = favoritesRepository.getFavorites(),
            historyUrls = historyRepository.getHistory()
        )
        loadM3UUrlFromBackend()
    }

    // UPDATE PERSISTENT STATE
    fun updateNavigationState(group: String, channel: Channel?) {
        _uiState.value = _uiState.value.copy(
            lastSelectedGroup = group,
            lastSelectedChannel = channel
        )
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState(macAddress = _uiState.value.macAddress)
        }
    }

    fun toggleFavorite(channel: Channel) {
        favoritesRepository.toggleFavorite(channel.url)
        _uiState.value = _uiState.value.copy(favoriteUrls = favoritesRepository.getFavorites())
    }

    fun addToHistory(channel: Channel) {
        historyRepository.addToHistory(channel.url)
        _uiState.value = _uiState.value.copy(historyUrls = historyRepository.getHistory())
    }

    fun loadM3UUrlFromBackend() {
        viewModelScope.launch {
            deviceRepository.getM3UUrl().fold(
                onSuccess = { m3uUrl ->
                    _uiState.value = _uiState.value.copy(m3uUrlFromBackend = m3uUrl, deviceRegistered = true)
                    if (m3uUrl.isNotBlank()) {
                        loadPlaylistFromBackend()
                        epgRepository.fetchEpg(constructEpgUrl(m3uUrl))
                    }
                },
                onFailure = { _uiState.value = _uiState.value.copy(deviceRegistered = false) }
            )
        }
    }

    fun refreshPlaylist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            deviceRepository.getM3UUrl().fold(
                onSuccess = { m3uUrl ->
                    if (!m3uUrl.isNullOrBlank()) {
                        playlistRepository.loadPlaylistFromBackend().fold(
                            onSuccess = { channels ->
                                _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                                epgRepository.fetchEpg(constructEpgUrl(m3uUrl))
                            },
                            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
                        )
                    }
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    private fun constructEpgUrl(m3uUrl: String): String {
        return try {
            if (m3uUrl.contains("get.php")) {
                m3uUrl.replace("get.php", "xmltv.php").split("&type=")[0].split("&output=")[0]
            } else m3uUrl.replace(".m3u", ".xml").replace(".m3u8", ".xml")
        } catch (e: Exception) { m3uUrl }
    }

    fun loadPlaylistFromBackend() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            playlistRepository.loadPlaylistFromBackend().fold(
                onSuccess = { channels -> _uiState.value = _uiState.value.copy(channels = channels, isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun getEpgForChannel(tvgId: String?, tvgName: String?): List<EpgProgram> {
        return epgRepository.getProgramsForChannel(tvgId, tvgName)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
