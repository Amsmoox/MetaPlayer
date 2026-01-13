package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onCategoryClick: (ChannelCategory) -> Unit,
    onChannelClick: (Channel) -> Unit, // Added for Quick Resume
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.channels.isEmpty() && !uiState.isLoading && uiState.error == null) {
        UniqueProNoPlaylistScreen(
            macAddress = uiState.macAddress,
            onReload = { 
                // If device is registered, refresh playlist; otherwise try to load from backend
                if (uiState.deviceRegistered) {
                    viewModel.refreshPlaylist()
                } else {
                    viewModel.loadM3UUrlFromBackend()
                }
            },
            onExit = onExit ?: { /* No exit handler */ }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF080808))
        ) {
            val error = uiState.error
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF9D00))
                    }
                }
                error != null -> {
                    ErrorMessage(message = error, onDismiss = { viewModel.clearError() })
                }
                else -> {
                    MainMenuScreen(
                        channels = uiState.channels,
                        viewModel = viewModel, // Passed missing viewModel
                        onCategoryClick = onCategoryClick,
                        onChannelClick = onChannelClick // Passed missing onChannelClick
                    )
                }
            }
        }
    }
}

@Composable
private fun UniqueProNoPlaylistScreen(
    macAddress: String,
    onReload: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF050505))
                )
            )
    ) {
        // Subtle background accent (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9D00).copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDE: Identity & Instructions
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(start = 64.dp, end = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Unique Branding
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "META",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 52.sp,
                                letterSpacing = (-2).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAYER",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 52.sp,
                                letterSpacing = (-2).sp
                            ),
                            color = Color(0xFFFF9D00)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF9D00), RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ULTRA 4K PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                // Step-by-Step Info
                InstructionStep(
                    number = "01",
                    title = "VISIT PORTAL",
                    description = "Go to http://metabackend.com/ on your PC or Phone"
                )

                Spacer(modifier = Modifier.height(32.dp))

                // MAC Address (The only key needed)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF9D00), RectangleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "02. YOUR MAC ADDRESS",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF333333), RectangleShape)
                            .background(Color(0xFF111111), RectangleShape)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = macAddress.uppercase().ifBlank { "DETECTING..." },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val clip = android.content.ClipData.newPlainText("MAC", macAddress)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "MAC Copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFFF9D00))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfessionalActionBtn(text = "RELOAD SYSTEM", isPrimary = true, onClick = onReload)
                    ProfessionalActionBtn(text = "CLOSE APP", isPrimary = false, onClick = onExit)
                }
            }

            // RIGHT SIDE: QR Experience
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0A))
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Styled QR Container
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(2.dp, Color(0xFFFF9D00), RectangleShape)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Placeholder icon for QR
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "QUICK ACTIVATE",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Scan the code with your phone camera\nto upload your playlist instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFFF9D00).copy(alpha = 0.5f),
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ProfessionalActionBtn(text: String, isPrimary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) Color(0xFFFF9D00) else Color(0xFF222222),
            contentColor = if (isPrimary) Color.Black else Color.White
        ),
        modifier = Modifier
            .height(56.dp)
            .padding(0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE91E63), RectangleShape)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(text = message, color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}
