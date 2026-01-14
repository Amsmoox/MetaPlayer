package com.metaplayer.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.ui.screens.CategoryChannelsScreen
import com.metaplayer.iptv.presentation.ui.screens.PlaylistScreen
import com.metaplayer.iptv.presentation.ui.screens.PlayerScreen
import com.metaplayer.iptv.presentation.ui.theme.MetaPlayerTheme
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel
import com.metaplayer.iptv.presentation.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MetaPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                    )
                    val playerViewModel: PlayerViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "playlist"
                    ) {
                        composable("playlist") {
                            PlaylistScreen(
                                viewModel = playlistViewModel,
                                onCategoryClick = { category ->
                                    navController.navigate("category/${category.name}")
                                },
                                onChannelClick = { channel ->
                                    playerViewModel.playChannel(channel)
                                    navController.navigate("player")
                                },
                                onExit = {
                                    finishAffinity()
                                }
                            )
                        }
                        
                        composable("category/{categoryName}") { backStackEntry ->
                            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                            val category = try {
                                ChannelCategory.valueOf(categoryName)
                            } catch (e: Exception) {
                                ChannelCategory.LIVE_TV
                            }
                            
                            val channels = playlistViewModel.uiState.value.channels
                            
                            CategoryChannelsScreen(
                                category = category,
                                channels = channels,
                                viewModel = playerViewModel,
                                playlistViewModel = playlistViewModel, // Pass for real EPG
                                onChannelClick = { channel ->
                                    playerViewModel.playChannel(channel)
                                    navController.navigate("player")
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        
                        composable("player") {
                            val currentChannel = playerViewModel.uiState.value.currentChannel
                            
                            if (currentChannel != null) {
                                PlayerScreen(
                                    channel = currentChannel,
                                    viewModel = playerViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
