package com.metaplayer.iptv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.EpgProgram
import com.metaplayer.iptv.data.repository.DeviceRepository
import com.metaplayer.iptv.data.repository.EpgRepository
import com.metaplayer.iptv.data.repository.PlaylistRepository
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val playlistUrl: String = "",
    val macAddress: String = "",
    val deviceRegistered: Boolean = false,
    val isRegistering: Boolean = false,
    val m3uUrlFromBackend: String? = null,
    val isRefreshing: Boolean = false
)

class PlaylistViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository(application)
    private val deviceRepository = DeviceRepository(application)
    val epgRepository = EpgRepository()

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        val macAddress = DeviceManager.getMacAddress(application)
        _uiState.value = _uiState.value.copy(macAddress = macAddress)
        loadM3UUrlFromBackend()
    }

    fun loadM3UUrlFromBackend() {
        viewModelScope.launch {
            deviceRepository.getM3UUrl().fold(
                onSuccess = { m3uUrl ->
                    _uiState.value = _uiState.value.copy(
                        m3uUrlFromBackend = m3uUrl,
                        deviceRegistered = true,
                        playlistUrl = m3uUrl
                    )
                    if (m3uUrl.isNotBlank()) {
                        loadPlaylistFromBackend()
                        
                        // SMART EPG LOGIC: 
                        // 1. Try to construct the standard Xtream Codes XMLTV URL
                        // 2. Fallback to guessing if it's a generic file
                        val epgUrl = constructEpgUrl(m3uUrl)
                        epgRepository.fetchEpg(epgUrl)
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        deviceRegistered = false,
                        m3uUrlFromBackend = null
                    )
                }
            )
        }
    }

    /**
     * Professional URL Construction Logic
     * Converts: http://domain.com/get.php?username=X&password=Y&type=m3u_plus&output=mpegts
     * Into:    http://domain.com/xmltv.php?username=X&password=Y
     */
    private fun constructEpgUrl(m3uUrl: String): String {
        return try {
            when {
                m3uUrl.contains("get.php") && m3uUrl.contains("username=") -> {
                    val url = m3uUrl.replace("get.php", "xmltv.php")
                    val urlParts = url.split("?")
                    if (urlParts.size == 2) {
                        val params = urlParts[1].split("&")
                        val relevantParams = params.filter { 
                            it.startsWith("username=") || it.startsWith("password=")
                        }
                        "${urlParts[0]}?${relevantParams.joinToString("&")}"
                    } else {
                        url.split("&type=")[0].split("&output=")[0]
                    }
                }
                m3uUrl.contains("/get.php") && m3uUrl.contains("/") -> {
                    m3uUrl.replace("/get.php", "/xmltv.php")
                        .split("&type=")[0]
                        .split("&output=")[0]
                }
                m3uUrl.endsWith(".m3u") -> m3uUrl.replace(".m3u", ".xml")
                m3uUrl.endsWith(".m3u8") -> m3uUrl.replace(".m3u8", ".xml")
                else -> m3uUrl.replace(".m3u8", ".xml.gz").replace(".m3u", ".xml.gz")
            }
        } catch (e: Exception) {
            m3uUrl.replace("get.php", "xmltv.php").split("&type=")[0].split("&output=")[0]
        }
    }

    fun loadPlaylistFromBackend() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            playlistRepository.loadPlaylistFromBackend().fold(
                onSuccess = { channels ->
                    _uiState.value = _uiState.value.copy(
                        channels = channels,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load playlist"
                    )
                }
            )
        }
    }
    
    /**
     * UPDATED: Get EPG programs by ID or Name
     */
    fun getEpgForChannel(tvgId: String?, tvgName: String?): List<EpgProgram> {
        return epgRepository.getProgramsForChannel(tvgId, tvgName)
    }

    fun refreshPlaylist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, isLoading = true, error = null)
            val macAddress = DeviceManager.getMacAddress(getApplication())
            val api = com.metaplayer.iptv.data.api.ApiClient.api
            
            try {
                val response = api.refreshPlaylist(macAddress)
                if (response.isSuccessful && response.body()?.success == true) {
                    kotlinx.coroutines.delay(1000)
                    loadPlaylistFromBackend()
                    
                    val m3uUrl = _uiState.value.m3uUrlFromBackend
                    if (!m3uUrl.isNullOrBlank()) {
                        val epgUrl = constructEpgUrl(m3uUrl)
                        epgRepository.fetchEpg(epgUrl)
                    }
                    
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
