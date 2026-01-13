package com.metaplayer.iptv.presentation.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.player.PlayerConfig
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun LiveTvScreen(
    channels: List<Channel>,
    playlistViewModel: PlaylistViewModel,
    onChannelClick: (Channel) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val groups = remember(channels) {
        listOf("ALL CHANNELS") + channels.mapNotNull { it.group }.distinct().sorted()
    }
    
    var selectedGroup by remember { mutableStateOf(groups.first()) }
    val filteredChannels = remember(selectedGroup, channels) {
        if (selectedGroup == "ALL CHANNELS") channels else channels.filter { it.group == selectedGroup }
    }
    
    var selectedChannel by remember { mutableStateOf(filteredChannels.firstOrNull()) }
    val currentEpg = remember(selectedChannel) {
        playlistViewModel.getEpgForChannel(selectedChannel?.tvgId, selectedChannel?.tvgName)
    }

    val exoPlayer = remember { PlayerConfig.createPlayer(context).apply { playWhenReady = true } }

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val mediaItem = MediaItem.fromUri(channel.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // COLUMN 1: GROUPS
        Column(modifier = Modifier.weight(0.5f).fillMaxHeight().background(Color(0xFF080808)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Box(modifier = Modifier.padding(20.dp)) {
                IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            }
            Text("GROUPS", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn {
                items(groups) { group ->
                    GroupItem(name = group, isSelected = selectedGroup == group, onClick = { 
                        selectedGroup = group
                        selectedChannel = filteredChannels.firstOrNull()
                    })
                }
            }
        }

        // COLUMN 2: CHANNELS
        Column(modifier = Modifier.weight(0.8f).fillMaxHeight().background(Color(0xFF0D0D0D)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Text("CHANNELS", modifier = Modifier.padding(24.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn {
                items(filteredChannels) { channel ->
                    ChannelItem(channel = channel, isSelected = selectedChannel == channel, onClick = { 
                        if (selectedChannel == channel) onChannelClick(channel) else selectedChannel = channel
                    })
                }
            }
        }

        // COLUMN 3: PREVIEW & EPG
        Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16/9f).background(Color.Black, RectangleShape).border(2.dp, Color(0xFF1A1A1A), RectangleShape), contentAlignment = Alignment.Center) {
                if (selectedChannel != null) {
                    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.PlayCircleFilled, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1A1A1A))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D0D0D), RectangleShape).padding(20.dp)) {
                if (selectedChannel != null && currentEpg.isNotEmpty()) {
                    Text("PROGRAM GUIDE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9D00), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    LazyColumn { items(currentEpg) { EpgRow(it.getFormattedTimeRange(), it.title) } }
                } else if (selectedChannel != null) {
                    Text("STREAM INFO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9D00))
                    Spacer(modifier = Modifier.height(16.dp))
                    StreamInfoLine("QUALITY", "ULTRA HD")
                    StreamInfoLine("STATUS", "STABLE")
                }
            }
        }
    }
}

@Composable
private fun GroupItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent).clickable { onClick() }.padding(horizontal = 24.dp, vertical = 14.dp)) {
        Text(name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal)
    }
}

@Composable
private fun ChannelItem(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent).clickable { onClick() }.padding(horizontal = 24.dp, vertical = 14.dp)) {
        Text(channel.name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StreamInfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EpgRow(time: String, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(time, color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.width(100.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
    }
}
