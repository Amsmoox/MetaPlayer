package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.ChannelCategory

@Composable
fun MainMenuScreen(
    channels: List<Channel>,
    onCategoryClick: (ChannelCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveCount = channels.count { it.category == ChannelCategory.LIVE_TV }
    val moviesCount = channels.count { it.category == ChannelCategory.MOVIES }
    val seriesCount = channels.count { it.category == ChannelCategory.SERIES }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        // Dynamic Accent Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9D00).copy(alpha = 0.05f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        text = "SELECT YOUR ENTERTAINMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9D00),
                        letterSpacing = 2.sp
                    )
                }
            }

            // Main Grid
            Row(
                modifier = Modifier.fillMaxWidth().height(420.dp),
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
            }
        }
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0D0D0D), RectangleShape)
            .border(1.dp, Color(0xFF1A1A1A), RectangleShape)
            .clickable { onClick() }
    ) {
        // Top Background "Artistic" Zone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.1f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Large stylized icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = accentColor.copy(alpha = 0.05f)
            )
            // Smaller sharp icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = accentColor
            )
        }

        // Bottom Info Zone
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .background(Color.Black, RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
