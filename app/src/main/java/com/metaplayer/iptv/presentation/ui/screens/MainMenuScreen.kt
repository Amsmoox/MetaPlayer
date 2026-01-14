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
    onSettingsClick: () -> Unit,
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
        // Background Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9D00).copy(alpha = 0.03f), Color.Transparent),
                        radius = 2000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(70.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "META PLAYER",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9D00)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ULTRA 4K EXPERIENCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivationStatusBadge(
                        activationType = uiState.activationType,
                        daysRemaining = uiState.daysRemaining,
                        isActive = uiState.isActive
                    )
                    IconButton(onClick = { viewModel.refreshPlaylist() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.Gray)
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("LOGOUT", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. PRO LAYOUT SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // BIG LIVE TV CARD (The Focus)
                MainFeatureCard(
                    title = "LIVE TV",
                    subtitle = "BROADCAST",
                    count = liveCount,
                    icon = Icons.Default.Tv,
                    modifier = Modifier.weight(1.8f),
                    onClick = { onCategoryClick(ChannelCategory.LIVE_TV) }
                )

                // VERTICAL STACK FOR OTHERS
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // MOVIES
                    SecondaryActionCard(
                        title = "MOVIES",
                        icon = Icons.Default.Movie,
                        count = moviesCount,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(ChannelCategory.MOVIES) }
                    )
                    // SERIES
                    SecondaryActionCard(
                        title = "SERIES",
                        icon = Icons.Default.VideoLibrary,
                        count = seriesCount,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(ChannelCategory.SERIES) }
                    )
                    // SETTINGS (Smallest / Different Style)
                    SettingsMiniCard(
                        onClick = onSettingsClick
                    )
                }
            }

            // 3. QUICK RESUME (Bottom Bar)
            if (recentChannels.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK RESUME",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFFFF9D00).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentChannels) { channel ->
                            QuickResumeItem(channel = channel, onClick = { onChannelClick(channel) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainFeatureCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0D0D0D), RectangleShape)
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFF9D00), Color.Transparent)), RectangleShape)
            .clickable { onClick() }
    ) {
        // Large Background Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(280.dp)
                .offset(x = 60.dp, y = 60.dp),
            tint = Color(0xFFFF9D00).copy(alpha = 0.05f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9D00),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 56.sp
                    ),
                    color = Color.White
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFF9D00))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$count CHANNELS AVAILABLE",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    title: String,
    icon: ImageVector,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D), RectangleShape)
            .border(1.dp, Color(0xFF1A1A1A), RectangleShape)
            .clickable { onClick() }
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFF151515))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Text(
                text = "$count ITEMS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
    }
}

@Composable
private fun SettingsMiniCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D))))
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "SYSTEM SETTINGS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "INFO",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFF9D00)
        )
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
        activationType == "LIFETIME" -> "LIFETIME"
        else -> "${daysRemaining ?: 0} DAYS"
    }
    val statusColor = if (isActive) Color(0xFFFF9D00) else Color(0xFFFF5252)

    Row(
        modifier = Modifier
            .background(Color(0xFF1A1A1A))
            .border(1.dp, statusColor.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(statusColor))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = statusText, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuickResumeItem(channel: Channel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(50.dp)
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
