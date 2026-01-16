package com.metaplayer.iptv.presentation.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaplayer.iptv.R
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun SettingsScreen(
    viewModel: PlaylistViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isLandscape) 64.dp else 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "SETTINGS & INFO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = if (isLandscape) 32.sp else 24.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 40.dp else 24.dp))

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left Side - Logo and Branding
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BrandingInfo()
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                    // Right Side - Details
                    Column(modifier = Modifier.weight(1.5f)) {
                        SettingsContent(uiState, context, viewModel)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrandingInfo(small = true)
                    Spacer(modifier = Modifier.height(32.dp))
                    SettingsContent(uiState, context, viewModel)
                }
            }
        }
    }
}

@Composable
private fun BrandingInfo(small: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(if (small) 120.dp else 200.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "META PLAYER PRO",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
        Text(
            text = "Version 1.0.2 (Ultra 4K)",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFF9D00)
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: com.metaplayer.iptv.presentation.viewmodel.PlaylistUiState,
    context: Context,
    viewModel: PlaylistViewModel
) {
    // SYSTEM ACTIONS SECTION
    SettingsSectionTitle("SYSTEM ACTIONS")
    
    ActionRow(label = "Refresh Playlist", icon = Icons.Default.Refresh, color = Color(0xFFFF9D00)) {
        viewModel.refreshPlaylist()
    }
    ActionRow(label = "Clear Watch History", icon = Icons.Default.History, color = Color.White) {
        viewModel.clearHistory()
    }
    ActionRow(label = "Check for Updates", icon = Icons.Default.SystemUpdate, color = Color.White) {
        Toast.makeText(context, "System is up to date", Toast.LENGTH_SHORT).show()
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Activation Section
    SettingsSectionTitle("ACTIVATION STATUS")
    val statusText = when {
        !uiState.isActive -> "EXPIRED"
        uiState.activationType == "LIFETIME" -> "LIFETIME ACTIVE"
        else -> "${uiState.activationType ?: "ACTIVE"} (${uiState.daysRemaining ?: 0} days remaining)"
    }
    val statusColor = if (uiState.isActive) Color(0xFF4CAF50) else Color(0xFFFF5252)
    InfoRow(label = "Status", value = statusText, valueColor = statusColor)
    InfoRow(label = "MAC Address", value = uiState.macAddress.uppercase(), showCopy = true) {
        copyToClipboard(context, "MAC Address", uiState.macAddress)
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Portal Section
    SettingsSectionTitle("MANAGEMENT PORTAL")
    InfoRow(label = "Website", value = "http://metabackend.com", showCopy = true) {
        copyToClipboard(context, "Website", "http://metabackend.com")
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Device Section
    SettingsSectionTitle("DEVICE INFORMATION")
    InfoRow(label = "Model", value = android.os.Build.MODEL)
    InfoRow(label = "Android Version", value = android.os.Build.VERSION.RELEASE)
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        color = Color(0xFFFF9D00).copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ActionRow(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    showCopy: Boolean = false,
    onCopy: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = valueColor)
        }
        if (showCopy) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFF9D00), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}
