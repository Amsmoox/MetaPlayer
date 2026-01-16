package com.metaplayer.iptv

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metaplayer.iptv.data.model.ChannelCategory
import com.metaplayer.iptv.presentation.ui.screens.CategoryChannelsScreen
import com.metaplayer.iptv.presentation.ui.screens.PlaylistScreen
import com.metaplayer.iptv.presentation.ui.screens.PlayerScreen
import com.metaplayer.iptv.presentation.ui.screens.SettingsScreen
import com.metaplayer.iptv.presentation.ui.theme.MetaPlayerTheme
import com.metaplayer.iptv.presentation.viewmodel.PlaylistViewModel
import com.metaplayer.iptv.presentation.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            MetaPlayerTheme {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

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
                                onSettingsClick = {
                                    navController.navigate("settings")
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
                                playlistViewModel = playlistViewModel,
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
                                    playlistViewModel = playlistViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = playlistViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
