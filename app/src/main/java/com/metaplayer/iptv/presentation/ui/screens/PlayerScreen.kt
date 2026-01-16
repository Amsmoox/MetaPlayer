package com.metaplayer.iptv.presentation.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.metaplayer.iptv.data.model.Channel
import com.metaplayer.iptv.player.PlayerConfig
import com.metaplayer.iptv.presentation.viewmodel.PlayerViewModel
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    channel: Channel,
    viewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playlistState by playlistViewModel.uiState.collectAsState()
    
    var showControls by remember { mutableStateOf(false) }
    var showChannelList by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    
    // Track highlighted channel index (for navigation without switching)
    var highlightedChannelIndex by remember { mutableStateOf(-1) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Focus Requesters
    val mainFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    
    val exoPlayer = remember {
        PlayerConfig.createPlayer(context).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                }
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = "Error: ${error.errorCodeName}"
                    isBuffering = false
                }
            })
            playWhenReady = true
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, showChannelList) {
        if (showControls && !showChannelList) {
            delay(4000)
            showControls = false
        }
    }

    // Ensure main box has focus to catch keys
    LaunchedEffect(Unit) {
        mainFocusRequester.requestFocus()
    }

    // Handle focus when list opens
    LaunchedEffect(showChannelList) {
        if (showChannelList) {
            // Initialize highlighted index to current playing channel
            val currentIndex = playlistState.channels.indexOf(uiState.currentChannel)
            highlightedChannelIndex = if (currentIndex != -1) currentIndex else 0
            delay(150)
            listFocusRequester.requestFocus()
            // Scroll to highlighted channel
            if (highlightedChannelIndex != -1) {
                scope.launch { listState.animateScrollToItem(highlightedChannelIndex) }
            }
        } else {
            delay(50)
            mainFocusRequester.requestFocus()
        }
    }

    // Initial play and channel changes
    LaunchedEffect(uiState.currentChannel) {
        uiState.currentChannel?.let { chan ->
            playbackError = null
            isBuffering = true
            exoPlayer.setMediaItem(MediaItem.fromUri(chan.url))
            exoPlayer.prepare()
            exoPlayer.play()
            
            val index = playlistState.channels.indexOf(chan)
            if (index != -1) {
                // Update highlighted index to match current playing channel
                highlightedChannelIndex = index
                if (!showChannelList) {
                    listState.scrollToItem(index)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    fun switchChannel(offset: Int) {
        val currentChannels = playlistState.channels
        val currentIndex = currentChannels.indexOf(uiState.currentChannel)
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + offset).coerceIn(0, currentChannels.size - 1)
            viewModel.playChannel(currentChannels[nextIndex])
            showControls = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(mainFocusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (showChannelList) {
                                // Navigate up in list without switching channel
                                if (highlightedChannelIndex > 0) {
                                    highlightedChannelIndex--
                                    scope.launch { listState.animateScrollToItem(highlightedChannelIndex) }
                                }
                                true
                            } else {
                                switchChannel(-1)
                                true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (showChannelList) {
                                // Navigate down in list without switching channel
                                if (highlightedChannelIndex < playlistState.channels.size - 1) {
                                    highlightedChannelIndex++
                                    scope.launch { listState.animateScrollToItem(highlightedChannelIndex) }
                                }
                                true
                            } else {
                                switchChannel(1)
                                true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (showChannelList) {
                                showChannelList = false
                                showControls = false
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showChannelList) {
                                showChannelList = true
                                showControls = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (showChannelList) {
                                showChannelList = false
                                showControls = false
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (showChannelList) {
                                // Switch to highlighted channel when OK is pressed
                                if (highlightedChannelIndex >= 0 && highlightedChannelIndex < playlistState.channels.size) {
                                    viewModel.playChannel(playlistState.channels[highlightedChannelIndex])
                                }
                                showChannelList = false
                                showControls = false
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable(interactionSource = interactionSource, indication = null) {
                if (showChannelList) {
                    showChannelList = false
                    showControls = false
                } else {
                    showChannelList = true
                    showControls = true
                }
            }
    ) {
        // 1. VIDEO LAYER
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    focusable = android.view.View.NOT_FOCUSABLE
                    isFocusableInTouchMode = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. CHANNEL LIST SIDE PANEL
        AnimatedVisibility(
            visible = showChannelList,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RectangleShape)
                    .clickable(enabled = false) { } 
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, null, tint = Color(0xFFFF9D00))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("QUICK SWITCH", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    
                    LazyColumn(
                        state = listState, 
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(listFocusRequester)
                            .focusable()
                    ) {
                        itemsIndexed(playlistState.channels) { index, chan ->
                            QuickChannelItem(
                                channel = chan,
                                isSelected = uiState.currentChannel == chan,
                                isHighlighted = highlightedChannelIndex == index,
                                onClick = { 
                                    viewModel.playChannel(chan)
                                    highlightedChannelIndex = index
                                    showChannelList = false
                                    showControls = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. TOP OVERLAY
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(20.dp))
                uiState.currentChannel?.let {
                    Text(it.name.uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
        }

        // 4. STATUS INDICATORS
        if (isBuffering && !showChannelList) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp).align(Alignment.Center).zIndex(20f), color = Color(0xFFFF9D00))
        }

        if (playbackError != null) {
            PlaybackErrorOverlay(error = playbackError!!, onBack = onBackClick)
        }
    }
}

@Composable
private fun QuickChannelItem(
    channel: Channel,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Highlighted = navigating to it, Selected = currently playing
    val bgColor = when {
        isHighlighted -> Color(0xFFFF9D00).copy(alpha = 0.3f) // Orange highlight when navigating
        isSelected -> Color(0xFF1A1A1A) // Dark background for currently playing
        else -> Color.Transparent
    }
    
    val borderColor = when {
        isHighlighted -> Color(0xFFFF9D00) // Orange border when highlighted
        isSelected -> Color(0xFFFF9D00).copy(alpha = 0.5f) // Subtle border for playing channel
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(if (isHighlighted || isSelected) 1.dp else 0.dp, borderColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(16.dp)
    ) {
        Text(
            text = channel.name,
            color = when {
                isHighlighted -> Color.White // White when highlighted (navigating to it)
                isSelected -> Color(0xFFFF9D00) // Orange for currently playing
                else -> Color.Gray
            },
            fontWeight = if (isHighlighted || isSelected) FontWeight.Black else FontWeight.Normal,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaybackErrorOverlay(error: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(24.dp).zIndex(100f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 60.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(error, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9D00))) {
            Text("GO BACK", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
