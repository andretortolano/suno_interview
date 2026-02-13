package com.suno.android.sunointerview.feature.player

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.core.net.toUri
import com.suno.android.sunointerview.ui.service.PlaybackService

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun PlayerScreen(
    songTitle: String?,
    artistName: String?,
    audioUrl: String?,
    imageUrl: String?,
    navController: NavController
) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }

    val controlsEnabled = controller != null && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture: ListenableFuture<MediaController> =
            MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val mediaController = controllerFuture.get()
            controller = mediaController
            
            // Sync initial state
            isPlaying = mediaController.isPlaying
            duration = mediaController.duration.coerceAtLeast(0L)
            currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
            playbackState = mediaController.playbackState

            val isSameSong = mediaController.currentMediaItem?.localConfiguration?.uri?.toString() == audioUrl

            if (!isSameSong) {
                audioUrl?.let { url ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(songTitle)
                                .setArtist(artistName)
                                .setArtworkUri(imageUrl?.toUri())
                                .build()
                        )
                        .build()
                    mediaController.setMediaItem(mediaItem)
                    mediaController.prepare()
                    mediaController.play()
                }
            }

            mediaController.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                    if (state == Player.STATE_READY) {
                        duration = mediaController.duration.coerceAtLeast(0L)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    currentPosition = newPosition.positionMs.coerceAtLeast(0L)
                }
            })
        }, MoreExecutors.directExecutor())

        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    LaunchedEffect(isPlaying, playbackState) {
        while (isPlaying && playbackState != Player.STATE_ENDED) {
            currentPosition = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = songTitle ?: "Unknown Title",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = artistName ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = {
                    val newPosition = (it * duration).toLong()
                    currentPosition = newPosition
                },
                onValueChangeFinished = {
                    controller?.seekTo(currentPosition)
                },
                enabled = controlsEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newPosition = ((controller?.currentPosition ?: 0L) - 10000).coerceAtLeast(0)
                        controller?.seekTo(newPosition)
                        currentPosition = newPosition
                    },
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(
                    onClick = {
                        controller?.let {
                            if (it.isPlaying) it.pause() else it.play()
                        }
                    },
                    enabled = controller != null,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val newPosition = ((controller?.currentPosition ?: 0L) + 10000).coerceAtMost(duration)
                        controller?.seekTo(newPosition)
                        currentPosition = newPosition
                    },
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
