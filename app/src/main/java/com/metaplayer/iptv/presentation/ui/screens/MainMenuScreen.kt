package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metaplayer.iptv.R
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun MainMenuScreen(
    channels: List<Channel>,
    viewModel: PlaylistViewModel,
    onCategoryClick: (ChannelCategory) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSettingsClick: () -> Unit, // Added parameter
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val liveCount = channels.count { it.category == ChannelCategory.LIVE_TV }
    val moviesCount = channels.count { it.category == ChannelCategory.MOVIES }
    val seriesCount = channels.count { it.category == ChannelCategory.SERIES }

    val recentChannels = remember(uiState.historyUrls, channels) {
        uiState.historyUrls.mapNotNull { url -> channels.find { it.url == url } }.take(10)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 1. TOP HEADER (Branding + Activation Status + Actions)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "META PLAYER PRO",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "ULTRA 4K STREAMING",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9D00),
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Activation Status Badge
                ActivationStatusBadge(
                    activationType = uiState.activationType,
                    daysRemaining = uiState.daysRemaining,
                    isActive = uiState.isActive
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.refreshPlaylist() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.logout() },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOGOUT", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 2. QUICK RESUME ROW (If history exists)
            if (recentChannels.isNotEmpty()) {
                Text(
                    text = "RESUME WATCHING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recentChannels) { channel ->
                        QuickResumeItem(channel = channel, onClick = { onChannelClick(channel) })
                    }
                }
            }

            // 3. MAIN CATEGORY CARDS
            Row(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MenuCategoryCard(
                    title = "LIVE TV",
                    subtitle = "TV CHANNELS",
                    count = liveCount,
                    icon = Icons.Default.Tv,
                    accentColor = Color(0xFFFF9D00),
                    onClick = { onCategoryClick(ChannelCategory.LIVE_TV) },
                    modifier = Modifier.weight(1f)
                )

                MenuCategoryCard(
                    title = "MOVIES",
                    subtitle = "CINEMA VOD",
                    count = moviesCount,
                    icon = Icons.Default.Movie,
                    accentColor = Color(0xFFFF9D00),
                    onClick = { onCategoryClick(ChannelCategory.MOVIES) },
                    modifier = Modifier.weight(1f)
                )

                MenuCategoryCard(
                    title = "SERIES",
                    subtitle = "TV SHOWS",
                    count = seriesCount,
                    icon = Icons.Default.VideoLibrary,
                    accentColor = Color(0xFFFF9D00),
                    onClick = { onCategoryClick(ChannelCategory.SERIES) },
                    modifier = Modifier.weight(1f)
                )

                MenuCategoryCard(
                    title = "SETTINGS",
                    subtitle = "APP INFO",
                    count = 0,
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF9E9E9E),
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(0.6f), // Smaller card
                    showCount = false
                )
            }
        }
    }
}

@Composable
private fun ActivationStatusBadge(
    activationType: String?,
    daysRemaining: Int?,
    isActive: Boolean
) {
    val statusText = when {
        !isActive -> "EXPIRED"
        activationType == "LIFETIME" -> "LIFETIME ACTIVE"
        activationType == "YEARLY" -> "YEARLY ACTIVE (${daysRemaining ?: 0} days left)"
        activationType == "FREE_TRIAL" -> "TRIAL (${daysRemaining ?: 0} days left)"
        else -> "ACTIVE"
    }

    val statusColor = when {
        !isActive -> Color(0xFFFF5252)
        activationType == "LIFETIME" -> Color(0xFF4CAF50)
        activationType == "YEARLY" -> Color(0xFFFF9D00)
        else -> Color(0xFF2196F3)
    }

    Box(
        modifier = Modifier
            .border(1.dp, statusColor.copy(alpha = 0.5f), RectangleShape)
            .background(statusColor.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, RectangleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun QuickResumeItem(channel: Channel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .background(Color(0xFF0D0D0D), RectangleShape)
            .border(1.dp, Color(0xFF1A1A1A), RectangleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
        )
        Text(
            text = channel.name,
            modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MenuCategoryCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0D0D0D), RectangleShape)
            .border(1.dp, Color(0xFF1A1A1A), RectangleShape)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.1f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = accentColor)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = accentColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color.White)
            if (showCount) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "$count ITEMS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
            }
        }
    }
}
