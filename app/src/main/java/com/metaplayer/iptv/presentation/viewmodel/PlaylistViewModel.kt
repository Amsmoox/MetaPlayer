package com.metaplayer.iptv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.repository.DeviceRepository
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
    val m3uUrlFromBackend: String? = null
)

class PlaylistViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository()
    private val deviceRepository = DeviceRepository(application)

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        // Get MAC address and check device registration on init
        val macAddress = DeviceManager.getMacAddress(application)
        _uiState.value = _uiState.value.copy(macAddress = macAddress)
        
        // Try to load M3U URL from backend
        loadM3UUrlFromBackend()
    }

    /**
     * Load M3U URL from backend for this device.
     */
    fun loadM3UUrlFromBackend() {
        viewModelScope.launch {
            deviceRepository.getM3UUrl().fold(
                onSuccess = { m3uUrl ->
                    _uiState.value = _uiState.value.copy(
                        m3uUrlFromBackend = m3uUrl,
                        deviceRegistered = true,
                        playlistUrl = m3uUrl
                    )
                    // Auto-load playlist if URL is available
                    if (m3uUrl.isNotBlank()) {
                        loadPlaylist(m3uUrl)
                    }
                },
                onFailure = {
                    // Device not registered or no M3U URL set
                    _uiState.value = _uiState.value.copy(
                        deviceRegistered = false,
                        m3uUrlFromBackend = null
                    )
                }
            )
        }
    }

    /**
     * Register device with backend and set M3U URL.
     */
    fun registerDevice(m3uUrl: String) {
        if (m3uUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid M3U URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRegistering = true, error = null)

            deviceRepository.updateM3UUrl(m3uUrl).fold(
                onSuccess = { deviceInfo ->
                    _uiState.value = _uiState.value.copy(
                        isRegistering = false,
                        deviceRegistered = true,
                        m3uUrlFromBackend = deviceInfo.m3u_url,
                        playlistUrl = deviceInfo.m3u_url,
                        error = null
                    )
                    // Load playlist after registration
                    loadPlaylist(deviceInfo.m3u_url)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isRegistering = false,
                        error = exception.message ?: "Failed to register device"
                    )
                }
            )
        }
    }

    /**
     * Load playlist from URL (direct or from backend).
     */
    fun loadPlaylist(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid M3U URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                playlistUrl = url
            )

            playlistRepository.loadPlaylist(url).fold(
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
