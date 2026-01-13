package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.viewmodel.PlayerViewModel
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun CategoryChannelsScreen(
    category: ChannelCategory,
    channels: List<Channel>,
    viewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onChannelClick: (Channel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryChannels = channels.filter { it.category == category }

    when (category) {
        ChannelCategory.LIVE_TV -> {
            LiveTvScreen(
                channels = categoryChannels,
                playlistViewModel = playlistViewModel,
                onChannelClick = onChannelClick,
                onBackClick = onBackClick
            )
        }
        ChannelCategory.MOVIES -> {
            VodScreen(
                channels = categoryChannels,
                title = "Movies",
                playlistViewModel = playlistViewModel, // Added missing parameter
                onMovieClick = onChannelClick,
                onBackClick = onBackClick
            )
        }
        ChannelCategory.SERIES -> {
            VodScreen(
                channels = categoryChannels,
                title = "Series",
                playlistViewModel = playlistViewModel, // Added missing parameter
                onMovieClick = onChannelClick,
                onBackClick = onBackClick
            )
        }
        else -> {
            // Default fallback to Live TV layout for any other category
            LiveTvScreen(
                channels = categoryChannels,
                playlistViewModel = playlistViewModel,
                onChannelClick = onChannelClick,
                onBackClick = onBackClick
            )
        }
    }
}
