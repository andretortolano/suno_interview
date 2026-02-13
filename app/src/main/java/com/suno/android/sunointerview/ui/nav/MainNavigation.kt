package com.suno.android.sunointerview.ui.nav

import android.content.ComponentName
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.MoreExecutors
import com.suno.android.sunointerview.ui.service.PlaybackService
import com.suno.android.sunointerview.feature.home.HomeScreen
import com.suno.android.sunointerview.feature.player.PlayerScreen
import com.suno.android.sunointerview.feature.player.PlayerScreenParams
import com.suno.android.sunointerview.feature.player.VerticalPlayerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    intent: Intent? = null
) {
    val context = LocalContext.current

    LaunchedEffect(intent) {
        if (intent?.action == PlaybackService.Companion.ACTION_OPEN_PLAYER) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                val controller = controllerFuture.get()
                val currentMediaItem = controller.currentMediaItem
                if (currentMediaItem != null) {
                    val metadata = currentMediaItem.mediaMetadata
                    val params = PlayerScreenParams(
                        title = metadata.title?.toString() ?: "",
                        artist = metadata.artist?.toString() ?: "",
                        audioUrl = currentMediaItem.localConfiguration?.uri?.toString() ?: "",
                        imageUrl = metadata.artworkUri?.toString() ?: ""
                    )
                    // You might want to navigate to VerticalPlayer here as well if that's the preferred UX
                    navController.navigate("vertical_player?songId=${currentMediaItem.mediaId}")
                }
                MediaController.releaseFuture(controllerFuture)
            }, MoreExecutors.directExecutor())
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("player/{songTitle}/{artistName}/{audioUrl}/{imageUrl}") { backStackEntry ->
            val params = PlayerScreenParams.fromUrlArguments(backStackEntry.arguments)

            PlayerScreen(
                songTitle = params.title,
                artistName = params.artist,
                audioUrl = params.audioUrl,
                imageUrl = params.imageUrl,
                navController = navController
            )
        }
        composable("vertical_player?songId={songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId")
            VerticalPlayerScreen(
                initialSongId = songId,
                navController = navController
            )
        }
    }
}
