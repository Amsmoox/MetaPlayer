package com.metaplayer.iptv.presentation.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
    val uiState by playlistViewModel.uiState.collectAsState()
    val epgLoaded by playlistViewModel.epgRepository.epgLoaded.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val groups = remember(channels, uiState.favoriteUrls, uiState.historyUrls) {
        val list = mutableListOf("FAVORITES", "LAST SEEN", "ALL CHANNELS")
        list.addAll(channels.mapNotNull { it.group }.distinct().sorted())
        list
    }
    
    var selectedGroup by remember { mutableStateOf(uiState.lastSelectedGroup) }
    
    val filteredChannels = remember(selectedGroup, searchQuery, channels, uiState.favoriteUrls, uiState.historyUrls) {
        // GLOBAL SEARCH LOGIC: If search query is not empty, ignore the selected group and search everything
        if (searchQuery.isNotEmpty()) {
            channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            when (selectedGroup) {
                "FAVORITES" -> channels.filter { uiState.favoriteUrls.contains(it.url) }
                "LAST SEEN" -> uiState.historyUrls.mapNotNull { url -> channels.find { it.url == url } }
                "ALL CHANNELS" -> channels
                else -> channels.filter { it.group == selectedGroup }
            }
        }
    }
    
    var selectedChannel by remember { 
        mutableStateOf(uiState.lastSelectedChannel?.takeIf { filteredChannels.contains(it) } ?: filteredChannels.firstOrNull()) 
    }

    val currentEpg = remember(selectedChannel, epgLoaded) {
        playlistViewModel.getEpgForChannel(selectedChannel?.tvgId, selectedChannel?.tvgName)
    }

    val exoPlayer = remember { PlayerConfig.createPlayer(context).apply { playWhenReady = true } }

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val mediaItem = MediaItem.fromUri(channel.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            playlistViewModel.updateNavigationState(selectedGroup, channel)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // SIDEBAR
        Column(modifier = Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF080808)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Row {
                    IconButton(onClick = { playlistViewModel.refreshPlaylist() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = { playlistViewModel.logout() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp)) }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222)).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery, 
                        onValueChange = { searchQuery = it }, 
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp), 
                        cursorBrush = SolidColor(Color(0xFFFF9D00)), 
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Only show groups if not searching
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups) { group ->
                    SpecialGroupItem(
                        name = group, 
                        isSelected = if (searchQuery.isEmpty()) selectedGroup == group else false,
                        icon = when(group) { "FAVORITES" -> Icons.Default.Star; "LAST SEEN" -> Icons.Default.History; else -> null },
                        onClick = { 
                            searchQuery = "" // Clear search when a group is explicitly clicked
                            selectedGroup = group
                            selectedChannel = filteredChannels.firstOrNull()
                            playlistViewModel.updateNavigationState(group, selectedChannel)
                        }
                    )
                }
            }
        }

        // CHANNELS
        Column(modifier = Modifier.width(240.dp).fillMaxHeight().background(Color(0xFF0D0D0D)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            val titleText = if (searchQuery.isEmpty()) "CHANNELS" else "SEARCH RESULTS"
            Text(titleText, modifier = Modifier.padding(16.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChannels) { channel ->
                    ChannelItem(
                        channel = channel, 
                        isSelected = selectedChannel == channel, 
                        isFavorite = uiState.favoriteUrls.contains(channel.url),
                        onFavoriteToggle = { playlistViewModel.toggleFavorite(channel) },
                        onClick = { 
                            if (selectedChannel == channel) {
                                playlistViewModel.addToHistory(channel)
                                onChannelClick(channel)
                            } else {
                                selectedChannel = channel
                                // Don't update global state with search results
                                if (searchQuery.isEmpty()) {
                                    playlistViewModel.updateNavigationState(selectedGroup, channel)
                                }
                            }
                        }
                    )
                }
            }
        }

        // PREVIEW & EPG
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16/9f).background(Color.Black, RectangleShape).border(2.dp, Color(0xFF1A1A1A), RectangleShape)) {
                if (selectedChannel != null) {
                    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize())
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D0D0D), RectangleShape).padding(16.dp)) {
                Text("PROGRAM GUIDE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF9D00), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (selectedChannel != null && currentEpg.isNotEmpty()) {
                    LazyColumn {
                        items(currentEpg) { program ->
                            Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                                Text(program.getFormattedTimeRange(), color = Color(0xFFFF9D00), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                                Text(program.title, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else if (selectedChannel != null) {
                    Column {
                        Text("STREAM INFORMATION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        StreamInfoLine(Icons.Default.Dns, "PROVIDER", "IPPROTV PREMIUM")
                        StreamInfoLine(Icons.Default.Speed, "NETWORK", "STABLE CONNECTION")
                        StreamInfoLine(Icons.Default.HighQuality, "OUTPUT", "4K ULTRA HD")
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("EPG is currently updating or not provided by this server.", color = Color.DarkGray, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamInfoLine(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.DarkGray, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 9.sp, modifier = Modifier.width(70.dp))
        Text(value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpecialGroupItem(name: String, isSelected: Boolean, icon: ImageVector?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val backgroundColor = when {
        isFocused -> Color(0xFFFF9D00).copy(alpha = 0.3f)
        isSelected -> Color(0xFF1A1A1A)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(if (isFocused) 1.dp else 0.dp, if (isFocused) Color(0xFFFF9D00) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { 
                Icon(imageVector = icon, contentDescription = null, tint = if (isSelected || isFocused) Color(0xFFFF9D00) else Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(12.dp)) 
            }
            Text(text = name, color = if (isSelected || isFocused) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.Normal)
        }
    }
}

@Composable
private fun ChannelItem(channel: Channel, isSelected: Boolean, isFavorite: Boolean, onFavoriteToggle: () -> Unit, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> Color(0xFFFF9D00).copy(alpha = 0.2f)
        isSelected -> Color(0xFF1A1A1A)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(if (isFocused) 1.dp else 0.dp, if (isFocused) Color(0xFFFF9D00) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = if (isFavorite) Color(0xFFFF9D00) else Color.DarkGray, modifier = Modifier.size(16.dp).clickable { onFavoriteToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(channel.name, color = if (isSelected || isFocused) Color.White else Color.Gray, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
