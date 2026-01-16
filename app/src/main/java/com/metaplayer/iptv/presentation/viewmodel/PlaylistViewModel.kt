package com.metaplayer.iptv.presentation.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.*
import com.metaplayer.iptv.data.repository.DeviceRepository
import com.metaplayer.iptv.data.repository.EpgRepository
import com.metaplayer.iptv.data.repository.FavoritesRepository
import com.metaplayer.iptv.data.repository.HistoryRepository
import com.metaplayer.iptv.data.repository.PlaylistRepository
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlaylistUiState(
    val channels: List<Channel> = emptyList(),
    val favoriteUrls: Set<String> = emptySet(),
    val historyUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0f,
    val error: String? = null,
    val macAddress: String = "",
    val deviceRegistered: Boolean = false,
    val m3uUrlFromBackend: String? = null,
    val isActive: Boolean = true,
    val activationType: String? = null,
    val daysRemaining: Int? = null,
    val isExpired: Boolean = false,
    val activationError: String? = null,
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
    
    private var loadingJob: Job? = null

    init {
        val macAddress = DeviceManager.getMacAddress(application)
        _uiState.value = _uiState.value.copy(
            macAddress = macAddress,
            favoriteUrls = favoritesRepository.getFavorites(),
            historyUrls = historyRepository.getHistory()
        )
        initialLoad()
        startActivityTracking()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            val cacheFile = File(getApplication<Application>().filesDir, "playlist_cache.m3u")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                loadPlaylistInternal(forceRefresh = false)
                updateDeviceInfoSilently()
            } else {
                refreshAll(forceRefresh = false)
            }
        }
    }

    private suspend fun updateDeviceInfoSilently() {
        deviceRepository.getDeviceInfo().fold(
            onSuccess = { info ->
                updateActivationFromDeviceInfo(info)
                _uiState.value = _uiState.value.copy(
                    m3uUrlFromBackend = info.m3u_url,
                    deviceRegistered = true
                )
            },
            onFailure = { }
        )
    }

    fun refreshAll(forceRefresh: Boolean = false) {
        loadingJob?.cancel() 
        loadingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null, 
                loadingProgress = 0f,
                channels = emptyList() 
            )
            
            deviceRepository.getDeviceInfo().fold(
                onSuccess = { info ->
                    updateActivationFromDeviceInfo(info)
                    val m3uUrl = info.m3u_url
                    _uiState.value = _uiState.value.copy(
                        m3uUrlFromBackend = m3uUrl,
                        deviceRegistered = true
                    )
                    
                    if (!info.is_active || (info.activation_status?.expired == true)) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        loadPlaylistInternal(forceRefresh)
                    }
                },
                onFailure = { 
                    registerDeviceAndReload(forceRefresh)
                }
            )
        }
    }

    private suspend fun registerDeviceAndReload(forceRefresh: Boolean) {
        deviceRepository.registerDevice(m3uUrl = null).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(deviceRegistered = true)
                updateStatusSuspend()
                loadPlaylistInternal(forceRefresh)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deviceRegistered = false,
                    error = "Connection error. Please check your internet."
                )
            }
        )
    }

    private suspend fun loadPlaylistInternal(forceRefresh: Boolean) {
        playlistRepository.loadPlaylistStreaming(
            forceRefresh = forceRefresh,
            onProgress = { progress ->
                _uiState.value = _uiState.value.copy(loadingProgress = progress)
            },
            onChannelsUpdated = { partialChannels ->
                _uiState.value = _uiState.value.copy(channels = partialChannels)
            }
        ).fold(
            onSuccess = { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels, 
                    isLoading = false,
                    loadingProgress = 1f
                )
                _uiState.value.m3uUrlFromBackend?.let {
                    epgRepository.fetchEpg(constructEpgUrl(it))
                }
            },
            onFailure = { 
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        )
    }

    fun checkList() {
        val cacheFile = File(getApplication<Application>().filesDir, "playlist_cache.m3u")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            refreshAll(forceRefresh = false)
        } else {
            Toast.makeText(
                getApplication(),
                "No list found. Go to http://metabackend.com/ to upload your playlist.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    fun clearHistory() {
        historyRepository.clearHistory()
        _uiState.value = _uiState.value.copy(historyUrls = emptyList())
        Toast.makeText(getApplication(), "Watch history cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateActivationFromDeviceInfo(info: DeviceInfoResponse) {
        info.activation_status?.let { status ->
            _uiState.value = _uiState.value.copy(
                isActive = status.is_active,
                activationType = status.activation_type,
                daysRemaining = status.days_remaining,
                isExpired = status.expired,
                activationError = info.message ?: info.warning
            )
        } ?: run {
            _uiState.value = _uiState.value.copy(
                isActive = info.is_active,
                activationError = info.message ?: info.warning
            )
        }
    }

    private suspend fun updateStatusSuspend() {
        deviceRepository.getActivationStatus().fold(
            onSuccess = { status ->
                _uiState.value = _uiState.value.copy(
                    isActive = status.is_active,
                    activationType = status.activation_type,
                    daysRemaining = status.days_remaining,
                    isExpired = status.expired,
                    activationError = status.error ?: status.message
                )
            },
            onFailure = { 
                deviceRepository.trackActivity()
            }
        )
    }

    fun checkActivationStatus() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            updateStatusSuspend()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun updateNavigationState(group: String, channel: Channel?) {
        _uiState.value = _uiState.value.copy(
            lastSelectedGroup = group,
            lastSelectedChannel = channel
        )
    }

    fun logout() {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = PlaylistUiState(
                macAddress = currentState.macAddress,
                deviceRegistered = currentState.deviceRegistered,
                isActive = currentState.isActive,
                activationType = currentState.activationType,
                daysRemaining = currentState.daysRemaining,
                isExpired = currentState.isExpired,
                activationError = currentState.activationError
            )
            updateStatusSuspend()
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
    
    private fun startActivityTracking() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(300000)
                if (_uiState.value.deviceRegistered) {
                    deviceRepository.trackActivity().fold(
                        onSuccess = { activity ->
                            _uiState.value = _uiState.value.copy(
                                isActive = activity.is_active,
                                activationType = activity.activation_status.activation_type,
                                daysRemaining = activity.activation_status.days_remaining,
                                isExpired = activity.activation_status.expired,
                                activationError = activity.error ?: activity.message
                            )
                        },
                        onFailure = {}
                    )
                }
            }
        }
    }

    fun loadM3UUrlFromBackend() { refreshAll(forceRefresh = true) }
    fun refreshPlaylist() { refreshAll(forceRefresh = true) }

    private fun constructEpgUrl(m3uUrl: String): String {
        return try {
            if (m3uUrl.contains("get.php")) {
                m3uUrl.replace("get.php", "xmltv.php").split("&type=")[0].split("&output=")[0]
            } else m3uUrl.replace(".m3u", ".xml").replace(".m3u8", ".xml")
        } catch (e: Exception) { m3uUrl }
    }

    fun getEpgForChannel(tvgId: String?, tvgName: String?): List<EpgProgram> {
        return epgRepository.getProgramsForChannel(tvgId, tvgName)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
