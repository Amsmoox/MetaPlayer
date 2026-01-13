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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
    var searchQuery by remember { mutableStateOf("") }
    val groups = remember(channels) {
        listOf("ALL CONTENT") + channels.mapNotNull { it.group }.distinct().sorted()
    }
    var selectedGroup by remember { mutableStateOf(groups.first()) }
    val filteredChannels = remember(selectedGroup, searchQuery, channels) {
        channels.filter { movie ->
            val matchesGroup = (selectedGroup == "ALL CONTENT" || movie.group == selectedGroup)
            val matchesSearch = movie.name.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // SIDEBAR: GROUPS & SEARCH (Narrower: 200dp)
        Column(modifier = Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF080808)).border(0.5.dp, Color(0xFF1A1A1A), RectangleShape)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title.uppercase(), style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), color = Color.White, fontWeight = FontWeight.Black)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(Color(0xFF111111), RectangleShape).border(1.dp, Color(0xFF222222), RectangleShape).padding(8.dp)) {
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
            
            Text("CATEGORIES", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups) { group ->
                    VodGroupItem(name = group, isSelected = selectedGroup == group, onClick = { selectedGroup = group })
                }
            }
        }

        // POSTER GRID
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = selectedGroup.uppercase(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp), color = Color.White, fontWeight = FontWeight.Black)
                Text(text = "${filteredChannels.size} RESULTS", style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp), color = Color.DarkGray, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (filteredChannels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("NO TITLES FOUND", color = Color.DarkGray, fontWeight = FontWeight.Black) }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredChannels) { movie -> MoviePoster(movie = movie, onClick = { onMovieClick(movie) }) }
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(movie: Channel, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Box(modifier = Modifier.aspectRatio(0.68f).background(Color(0xFF0F0F0F), RectangleShape).border(1.dp, Color(0xFF1A1A1A), RectangleShape)) {
            AsyncImage(model = movie.logo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(30.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = movie.name.uppercase(), color = Color.LightGray, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VodGroupItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF151515) else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Box(modifier = Modifier.size(3.dp, 12.dp).background(Color(0xFFFF9D00)))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = name, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
