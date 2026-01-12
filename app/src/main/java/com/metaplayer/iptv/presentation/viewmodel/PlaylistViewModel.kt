package com.metaplayer.iptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val playlistUrl: String = ""
)

class PlaylistViewModel(
    private val repository: PlaylistRepository = PlaylistRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

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

            repository.loadPlaylist(url).fold(
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
