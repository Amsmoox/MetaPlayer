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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metaplayer.iptv.data.model.Channel

@Composable
fun VodScreen(
    channels: List<Channel>,
    title: String,
    onMovieClick: (Channel) -> Unit,
    onBackClick: () -> Unit
) {
    val groups = remember(channels) {
        listOf("ALL CONTENT") + channels.mapNotNull { it.group }.distinct().sorted()
    }
    
    var selectedGroup by remember { mutableStateOf(groups.first()) }
    val filteredChannels = remember(selectedGroup, channels) {
        if (selectedGroup == "ALL CONTENT") channels else channels.filter { it.group == selectedGroup }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // SIDEBAR: VOD GROUPS
        Column(modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0xFF080808)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Box(modifier = Modifier.padding(20.dp)) {
                IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            }
            Text(title.uppercase(), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn {
                items(groups) { group ->
                    VodGroupItem(name = group, isSelected = selectedGroup == group, onClick = { selectedGroup = group })
                }
            }
        }

        // POSTER GRID
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 32.dp, vertical = 24.dp)) {
            Text(text = "${selectedGroup.uppercase()} (${filteredChannels.size})", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(filteredChannels) { movie ->
                    MoviePoster(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(movie: Channel, onClick: () -> Unit) {
    Column(modifier = Modifier.width(160.dp).clickable { onClick() }) {
        Box(modifier = Modifier.aspectRatio(0.7f).background(Color(0xFF111111), RectangleShape).border(1.dp, Color(0xFF222222), RectangleShape)) {
            AsyncImage(model = movie.logo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).padding(6.dp)) {
                Text(movie.name, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun VodGroupItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent).clickable { onClick() }.padding(horizontal = 24.dp, vertical = 14.dp)) {
        Text(name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal)
    }
}
