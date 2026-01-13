package com.metaplayer.iptv.presentation.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
    var searchQuery by remember { mutableStateOf("") }
    
    val groups = remember(channels) {
        listOf("ALL CHANNELS") + channels.mapNotNull { it.group }.distinct().sorted()
    }
    
    var selectedGroup by remember { mutableStateOf(groups.first()) }
    
    val filteredChannels = remember(selectedGroup, searchQuery, channels) {
        channels.filter { channel ->
            val matchesGroup = (selectedGroup == "ALL CHANNELS" || channel.group == selectedGroup)
            val matchesSearch = channel.name.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
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
        // COLUMN 1: GROUPS & SEARCH (Narrower)
        Column(modifier = Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF080808)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("LIVE TV", style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), color = Color.White, fontWeight = FontWeight.Black)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color(0xFF111111), RectangleShape)
                    .border(1.dp, Color(0xFF222222), RectangleShape)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        cursorBrush = SolidColor(Color(0xFFFF9D00)),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("SEARCH...", color = Color.DarkGray, fontSize = 12.sp)
                            innerTextField()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("GROUPS", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups) { group ->
                    GroupItem(name = group, isSelected = selectedGroup == group, onClick = { 
                        selectedGroup = group
                        selectedChannel = filteredChannels.firstOrNull()
                    })
                }
            }
        }

        // COLUMN 2: CHANNELS (Narrower)
        Column(modifier = Modifier.width(240.dp).fillMaxHeight().background(Color(0xFF0D0D0D)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Text("CHANNELS", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChannels) { channel ->
                    ChannelItem(channel = channel, isSelected = selectedChannel == channel, onClick = { 
                        if (selectedChannel == channel) onChannelClick(channel) else selectedChannel = channel
                    })
                }
            }
        }

        // COLUMN 3: PREVIEW (Wider)
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16/9f).background(Color.Black, RectangleShape).border(2.dp, Color(0xFF1A1A1A), RectangleShape), contentAlignment = Alignment.Center) {
                if (selectedChannel != null) {
                    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.PlayCircleFilled, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1A1A1A))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D0D0D), RectangleShape).padding(16.dp)) {
                if (selectedChannel != null && currentEpg.isNotEmpty()) {
                    Text("PROGRAM GUIDE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF9D00), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn { items(currentEpg) { EpgRow(it.getFormattedTimeRange(), it.title) } }
                } else if (selectedChannel != null) {
                    Text("STREAM INFO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF9D00), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    StreamInfoLine("QUALITY", "ULTRA HD")
                    StreamInfoLine("STATUS", "STABLE")
                }
            }
        }
    }
}

@Composable
private fun GroupItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChannelItem(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Box(modifier = Modifier.size(3.dp, 14.dp).background(Color(0xFFFF9D00)))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(channel.name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StreamInfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color.Gray, modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
        Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EpgRow(time: String, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(time, color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black, modifier = Modifier.width(90.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
