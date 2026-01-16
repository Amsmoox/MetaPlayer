package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun VodScreen(
    channels: List<Channel>,
    title: String,
    playlistViewModel: PlaylistViewModel,
    onMovieClick: (Channel) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by playlistViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val groups = remember(channels, uiState.favoriteUrls, uiState.historyUrls) {
        listOf("FAVORITES", "LAST SEEN", "ALL CONTENT") + channels.mapNotNull { it.group }.distinct().sorted()
    }
    
    var selectedGroup by remember { mutableStateOf(uiState.lastSelectedGroup) }
    
    val filteredChannels = remember(selectedGroup, searchQuery, channels, uiState.favoriteUrls, uiState.historyUrls) {
        // GLOBAL SEARCH LOGIC: If search query is not empty, ignore the selected group and search everything
        if (searchQuery.isNotEmpty()) {
            channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            val baseList = when (selectedGroup) {
                "FAVORITES" -> channels.filter { uiState.favoriteUrls.contains(it.url) }
                "LAST SEEN" -> uiState.historyUrls.mapNotNull { url -> channels.find { it.url == url } }
                "ALL CONTENT" -> channels
                else -> channels.filter { it.group == selectedGroup }
            }
            baseList
        }
    }

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
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups) { group ->
                    VodGroupItem(
                        name = group, 
                        isSelected = if (searchQuery.isEmpty()) selectedGroup == group else false,
                        icon = when(group) { "FAVORITES" -> Icons.Default.Star; "LAST SEEN" -> Icons.Default.History; else -> null },
                        bgColor = when(group) { "FAVORITES" -> if (selectedGroup == group && searchQuery.isEmpty()) Color(0xFFFF9D00).copy(alpha = 0.2f) else Color.Transparent; "LAST SEEN" -> if (selectedGroup == group && searchQuery.isEmpty()) Color(0xFF333333) else Color.Transparent; else -> Color.Transparent },
                        onClick = { 
                            searchQuery = "" // Clear search when a group is clicked
                            selectedGroup = group 
                            playlistViewModel.updateNavigationState(group, null)
                        }
                    )
                }
            }
        }

        // GRID
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                val headerTitle = if (searchQuery.isEmpty()) selectedGroup.uppercase() else "SEARCH RESULTS"
                Text(text = headerTitle, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp), color = Color.White, fontWeight = FontWeight.Black)
                Text(text = "${filteredChannels.size} TITLES", style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp), color = Color.DarkGray, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredChannels) { movie -> 
                    MoviePoster(
                        movie = movie, 
                        isFavorite = uiState.favoriteUrls.contains(movie.url),
                        onFavoriteToggle = { playlistViewModel.toggleFavorite(movie) },
                        onClick = { 
                            playlistViewModel.addToHistory(movie)
                            // Only update navigation state if not searching
                            if (searchQuery.isEmpty()) {
                                playlistViewModel.updateNavigationState(selectedGroup, movie)
                            }
                            onMovieClick(movie) 
                        }
                    ) 
                }
            }
        }
    }
}

@Composable
private fun VodGroupItem(name: String, isSelected: Boolean, icon: ImageVector?, bgColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected && icon == null) Color(0xFF151515) else bgColor).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(imageVector = icon, null, tint = if (isSelected) Color(0xFFFF9D00) else Color.Gray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(12.dp)) }
            Text(text = name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MoviePoster(movie: Channel, isFavorite: Boolean, onFavoriteToggle: () -> Unit, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Box(modifier = Modifier.aspectRatio(0.68f).background(Color(0xFF0F0F0F), RectangleShape).border(1.dp, Color(0xFF1A1A1A), RectangleShape)) {
            AsyncImage(model = movie.logo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(alpha = 0.6f)).clickable { onFavoriteToggle() }, contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (isFavorite) Color(0xFFFF9D00) else Color.White, modifier = Modifier.size(14.dp))
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(30.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = movie.name.uppercase(), color = Color.LightGray, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
    }
}
