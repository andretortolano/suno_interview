package com.suno.android.sunointerview.feature.home

import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors
import com.suno.android.sunointerview.ui.service.PlaybackService
import com.suno.android.sunointerview.feature.player.PlayerScreenParams
import com.suno.android.sunointerview.domain.model.Song
import com.suno.android.sunointerview.feature.home.component.HomeMiniPlayer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var currentMediaItem by remember { mutableStateOf(controller?.currentMediaItem) }
    // Add a state to trigger recomposition for player changes
    var playerEventTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val mediaController = controllerFuture.get()
            controller = mediaController
            currentMediaItem = mediaController.currentMediaItem

            mediaController.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaItem = mediaItem
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                        playerEventTrigger++
                    }
                }
            })
        }, MoreExecutors.directExecutor())

        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suno Songs") }
            )
        },
        bottomBar = {
            val currentController = controller
            val mediaItem = currentMediaItem
            // Accessing playerEventTrigger to ensure recomposition
            if (playerEventTrigger >= 0 && currentController != null && mediaItem != null) {
                HomeMiniPlayer(
                    mediaItem = mediaItem,
                    player = currentController,
                    onPlayerClick = {
                        navController.navigate("vertical_player?songId=${mediaItem.mediaId}")
                    },
                    onDismiss = {
                        currentController.stop()
                        currentController.clearMediaItems()
                        currentMediaItem = null
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is HomeState.Success -> {
                val pagingItems = state.songs.collectAsLazyPagingItems()
                
                Box(modifier = Modifier.fillMaxSize()) {
                    SongList(
                        pagingItems = pagingItems,
                        onSongClick = { song ->
                            navController.navigate("vertical_player?songId=${song.id}")
                        },
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Paging states handled within the Success view
                    if (pagingItems.loadState.refresh is LoadState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
fun SongList(
    pagingItems: LazyPagingItems<Song>,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(pagingItems.itemCount) { index ->
            val song = pagingItems[index] ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = song.title, fontWeight = FontWeight.Bold)
                    Text(text = song.artist, style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider()
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
