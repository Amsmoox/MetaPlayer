package com.metaplayer.iptv.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
    onChannelClick: (Channel) -> Unit,
    onSettingsClick: () -> Unit,
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitConfirmation by remember { mutableStateOf(false) }

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

    if (uiState.isExpired) {
        ExpiredScreen(
            macAddress = uiState.macAddress,
            activationType = uiState.activationType,
            daysRemaining = uiState.daysRemaining,
            errorMessage = uiState.activationError ?: "Trial period expired",
            onReload = { viewModel.checkActivationStatus() },
            onExit = { showExitConfirmation = true }
        )
    } else if (uiState.channels.isEmpty() && !uiState.isLoading && uiState.error == null) {
        UniqueProNoPlaylistScreen(
            macAddress = uiState.macAddress,
            daysRemaining = uiState.daysRemaining,
            activationType = uiState.activationType,
            onReload = { 
                viewModel.checkActivationStatus()
                if (uiState.deviceRegistered) viewModel.refreshPlaylist() 
                else viewModel.loadM3UUrlFromBackend()
            },
            onExit = { showExitConfirmation = true }
        )
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF080808))) {
            if (uiState.isLoading) {
                ProfessionalLoadingScreen(
                    message = when {
                        uiState.channels.isEmpty() -> "Initializing System..."
                        else -> "Refreshing Content..."
                    }
                )
            } else if (uiState.error != null) {
                ErrorMessage(message = uiState.error!!, onDismiss = { viewModel.clearError() })
            } else {
                MainMenuScreen(
                    channels = uiState.channels,
                    viewModel = viewModel,
                    onCategoryClick = onCategoryClick,
                    onChannelClick = onChannelClick,
                    onSettingsClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun ProfessionalLoadingScreen(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color(0xFF0A0A0A), Color(0xFF050505)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Loading",
                modifier = Modifier.size(240.dp).scale(scale).alpha(alpha),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "META", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp), color = Color.White)
                Text(text = "PLAYER", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp), color = Color(0xFFFF9D00))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.width(200.dp).height(4.dp).background(Color.White.copy(alpha = 0.1f))) {
                val progressWidth by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 200f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "progress")
                Box(modifier = Modifier.width(progressWidth.dp).fillMaxHeight().background(Color(0xFFFF9D00)))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = message.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
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

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF0F0F0F), Color(0xFF050505))))) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1.2f).fillMaxHeight().padding(start = 64.dp, end = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "META", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 52.sp), color = Color.White)
                    Text(text = "PLAYER", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light, fontSize = 52.sp), color = Color(0xFFFF9D00))
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // LEGAL DISCLAIMER
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9D00).copy(alpha = 0.05f)),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF9D00), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "NOTICE: Meta Player is a media player. We do not provide any content or playlists.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                // Activation Info
                val daysText = if (daysRemaining != null) "$daysRemaining days left" else "Checking..."
                Text(text = "STATUS: ${activationType ?: "DEVICE"} ACTIVE ($daysText)", color = Color(0xFFFF9D00), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))

                InstructionStep(number = "01", title = "VISIT PORTAL", description = "Go to http://metabackend.com/ to manage your playlist")
                
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(text = "02. YOUR MAC ADDRESS", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).border(1.dp, Color(0xFF333333)).background(Color(0xFF111111)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = macAddress.uppercase(), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.White)
                        IconButton(onClick = { 
                            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("MAC", macAddress))
                            Toast.makeText(context, "MAC Copied", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, null, tint = Color(0xFFFF9D00)) }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfessionalActionBtn(text = "RELOAD SYSTEM", isPrimary = true, onClick = onReload)
                    ProfessionalActionBtn(text = "CLOSE APP", isPrimary = false, onClick = onExit)
                }
            }

            Box(modifier = Modifier.weight(0.8f).fillMaxHeight().background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(280.dp), contentScale = ContentScale.Fit)
                    Text(text = "META PLAYER PRO", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                    Text(text = "The ultimate 4K streaming experience.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = number, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFF9D00).copy(alpha = 0.5f), fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun ProfessionalActionBtn(text: String, isPrimary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) Color(0xFFFF9D00) else Color(0xFF222222), contentColor = if (isPrimary) Color.Black else Color.White),
        modifier = modifier.height(50.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ExpiredScreen(macAddress: String, activationType: String?, daysRemaining: Int? = null, errorMessage: String, onReload: () -> Unit, onExit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF1A0A0A), Color(0xFF0A0505))))) {
        Column(modifier = Modifier.fillMaxSize().padding(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "⚠️", style = MaterialTheme.typography.displayLarge, fontSize = 80.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = if (activationType == "YEARLY") "ACTIVATION EXPIRED" else "TRIAL PERIOD EXPIRED", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = Color(0xFFFF9D00))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))
            ProfessionalActionBtn(text = "CHECK STATUS", isPrimary = true, onClick = onReload, modifier = Modifier.width(200.dp))
        }
    }
}

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF0A0A0A)).border(2.dp, Color(0xFFFF9D00)).padding(40.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SentimentVeryDissatisfied, null, tint = Color(0xFFFF9D00), modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "OH NO! LEAVING?", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfessionalActionBtn(text = "I'LL STAY", isPrimary = true, onClick = onDismiss, modifier = Modifier.weight(1f))
                    ProfessionalActionBtn(text = "YES, EXIT", isPrimary = false, onClick = onConfirm, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE91E63)).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = message, color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text("DISMISS", color = Color.White, fontWeight = FontWeight.Black) }
        }
    }
}
