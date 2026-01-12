package com.metaplayer.iptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metaplayer.iptv.data.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun playChannel(channel: Channel) {
        _uiState.value = _uiState.value.copy(
            currentChannel = channel,
            isLoading = true,
            isPlaying = false
        )
    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            isLoading = false
        )
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
}
