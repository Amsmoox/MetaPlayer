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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.ui.window.Dialog
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel
import com.metaplayer.iptv.R

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onCategoryClick: (ChannelCategory) -> Unit,
    onChannelClick: (Channel) -> Unit, // Added for Quick Resume
    onSettingsClick: () -> Unit, // New parameter
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Refresh activation status if missing or invalid after logout
    LaunchedEffect(Unit) {
        if (uiState.deviceRegistered && (uiState.daysRemaining == null || uiState.activationType == null)) {
            viewModel.checkActivationStatus()
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = { 
                showExitConfirmation = false
                onExit?.invoke() 
            },
            onDismiss = { showExitConfirmation = false }
        )
    }

    // Check if expired first - show expired screen
    if (uiState.isExpired) {
        ExpiredScreen(
            macAddress = uiState.macAddress,
            activationType = uiState.activationType,
            daysRemaining = uiState.daysRemaining,
            errorMessage = uiState.activationError ?: "Trial period expired",
            onReload = { 
                viewModel.checkActivationStatus()
            },
            onExit = { showExitConfirmation = true }
        )
    } else if (uiState.channels.isEmpty() && !uiState.isLoading && uiState.error == null) {
        UniqueProNoPlaylistScreen(
            macAddress = uiState.macAddress,
            daysRemaining = uiState.daysRemaining,
            activationType = uiState.activationType,
            onReload = { 
                // First refresh activation status to ensure we have the latest daysRemaining
                viewModel.checkActivationStatus()
                // Then try to reload the playlist
                if (uiState.deviceRegistered) {
                    viewModel.refreshPlaylist()
                } else {
                    viewModel.loadM3UUrlFromBackend()
                }
            },
            onExit = { showExitConfirmation = true }
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
                        viewModel = viewModel,
                        onCategoryClick = onCategoryClick,
                        onChannelClick = onChannelClick,
                        onSettingsClick = onSettingsClick // Pass to MainMenuScreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF0A0A0A), RectangleShape)
                .border(2.dp, Color(0xFFFF9D00), RectangleShape)
                .padding(40.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // BIG SAD FACE ICON
                Icon(
                    imageVector = Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = null,
                    tint = Color(0xFFFF9D00),
                    modifier = Modifier.size(100.dp).padding(bottom = 24.dp)
                )

                Text(
                    text = "OH NO! LEAVING?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Are you sure you want to exit Meta Player Pro?\nYour premium entertainment is waiting for you!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfessionalActionBtn(
                        text = "I'LL STAY",
                        isPrimary = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    ProfessionalActionBtn(
                        text = "YES, EXIT",
                        isPrimary = false,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UniqueProNoPlaylistScreen(
    macAddress: String,
    daysRemaining: Int? = null,
    activationType: String? = null,
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

                Spacer(modifier = Modifier.height(48.dp))

                // Activation Info Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    val daysText = if (daysRemaining != null) "$daysRemaining days left" else "Checking status..."
                    val statusText = when (activationType) {
                        "LIFETIME" -> "LIFETIME ACTIVE"
                        "YEARLY" -> "YEARLY ACTIVE ($daysText)"
                        "FREE_TRIAL" -> "FREE TRIAL ($daysText)"
                        else -> "DEVICE ACTIVE ($daysText)"
                    }
                    val statusColor = if (activationType == "LIFETIME") Color(0xFF4CAF50) else Color(0xFFFF9D00)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(statusColor))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Step-by-Step Info
                InstructionStep(
                    number = "01",
                    title = "VISIT PORTAL",
                    description = "Go to http://metabackend.com/ on your PC or Phone"
                )

                Spacer(modifier = Modifier.height(24.dp))

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

            // RIGHT SIDE: Logo Experience
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0A))
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(320.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "META PLAYER PRO",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Experience the best 4K streaming\nquality on your device.",
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
private fun ProfessionalActionBtn(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) Color(0xFFFF9D00) else Color(0xFF222222),
            contentColor = if (isPrimary) Color.Black else Color.White
        ),
        modifier = modifier
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
private fun ExpiredScreen(
    macAddress: String,
    activationType: String?,
    daysRemaining: Int? = null,
    errorMessage: String,
    onReload: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0A0A), Color(0xFF0A0505))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Expired Icon/Message
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 80.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val title = if (activationType == "YEARLY") "ACTIVATION EXPIRED" else "TRIAL PERIOD EXPIRED"
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFFFF9D00),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Please contact support or visit our portal to activate your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // MAC Address Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF333333), RectangleShape)
                    .background(Color(0xFF111111), RectangleShape)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOUR MAC ADDRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9D00),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = macAddress.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfessionalActionBtn(
                    text = "CHECK STATUS",
                    isPrimary = true,
                    onClick = onReload,
                    modifier = Modifier.weight(1f)
                )
                ProfessionalActionBtn(
                    text = "EXIT",
                    isPrimary = false,
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
