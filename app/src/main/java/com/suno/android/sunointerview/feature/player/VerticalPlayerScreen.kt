package com.suno.android.sunointerview.feature.player

import android.content.ComponentName
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.suno.android.sunointerview.domain.model.Song
import com.suno.android.sunointerview.feature.home.HomeState
import com.suno.android.sunointerview.feature.home.HomeViewModel
import com.suno.android.sunointerview.ui.service.PlaybackService
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VerticalPlayerScreen(
    initialSongId: String?,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // Monitor if this screen is still the current destination.
    // This turns false immediately when back navigation starts.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isVisible = currentRoute?.startsWith("vertical_player") == true

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture: ListenableFuture<MediaController> =
            MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())

        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Use AnimatedVisibility to fade everything out during back navigation.
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is HomeState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is HomeState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is HomeState.Success -> {
                        val pagingItems = state.songs.collectAsLazyPagingItems()
                        val initialPage = remember(pagingItems.itemCount, initialSongId) {
                            if (initialSongId != null) {
                                (0 until pagingItems.itemCount).firstOrNull { pagingItems[it]?.id == initialSongId } ?: 0
                            } else 0
                        }

                        if (pagingItems.itemCount > 0) {
                            val pagerState = rememberPagerState(initialPage = initialPage) { pagingItems.itemCount }

                            LaunchedEffect(pagerState.currentPage, controller, pagingItems.itemCount, isVisible) {
                                if (controller == null || !isVisible) return@LaunchedEffect
                                
                                val song = if (pagerState.currentPage < pagingItems.itemCount) {
                                    pagingItems[pagerState.currentPage]
                                } else null
                                
                                if (song != null) {
                                    val mediaController = controller!!
                                    val urlToPlay = song.videoUrl ?: song.audioUrl
                                    val currentUri = mediaController.currentMediaItem?.localConfiguration?.uri?.toString()
                                    
                                    if (currentUri != urlToPlay && urlToPlay != null) {
                                        val mediaItem = MediaItem.Builder()
                                            .setUri(urlToPlay)
                                            .setMediaId(song.id)
                                            .setMediaMetadata(
                                                MediaMetadata.Builder()
                                                    .setTitle(song.title)
                                                    .setArtist(song.artist)
                                                    .setArtworkUri(song.imageUrl?.toUri())
                                                    .build()
                                            )
                                            .build()
                                        mediaController.setMediaItem(mediaItem)
                                        mediaController.prepare()
                                        mediaController.play()
                                    }
                                }
                            }

                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                                pageSpacing = 0.dp,
                                contentPadding = PaddingValues(vertical = 120.dp)
                            ) { page ->
                                val song = pagingItems[page]
                                if (song != null) {
                                    val pageOffset = (
                                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .zIndex(1f - abs(pageOffset))
                                            .graphicsLayer {
                                                translationY = pageOffset * size.height * 0.15f
                                                val fraction = 1f - abs(pageOffset).coerceIn(0f, 1f)
                                                scaleX = lerp(0.85f, 1f, fraction)
                                                scaleY = lerp(0.85f, 1f, fraction)
                                                alpha = lerp(0.5f, 1f, fraction)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlayerPage(
                                            song = song,
                                            controller = controller,
                                            isVisible = isVisible,
                                            modifier = Modifier
                                                .fillMaxWidth(0.90f)
                                                .fillMaxHeight(0.95f)
                                                .clip(RoundedCornerShape(24.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TopAppBar(
                    title = { Text("Suno Player", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}


@OptIn(UnstableApi::class)
@Composable
fun PlayerPage(
    song: Song,
    controller: MediaController?,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }

    val urlToPlay = song.videoUrl ?: song.audioUrl
    var isCurrentPage by remember { mutableStateOf(false) }

    DisposableEffect(controller, urlToPlay) {
        val listener = object : Player.Listener {
            private fun updateState() {
                val currentUri = controller?.currentMediaItem?.localConfiguration?.uri?.toString()
                isCurrentPage = currentUri == urlToPlay
                
                if (isCurrentPage) {
                    isPlaying = controller?.isPlaying ?: false
                    playbackState = controller?.playbackState ?: Player.STATE_IDLE
                    duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    currentPosition = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) = updateState()
            override fun onPlaybackStateChanged(state: Int) = updateState()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) = updateState()
        }

        controller?.addListener(listener)
        val currentUri = controller?.currentMediaItem?.localConfiguration?.uri?.toString()
        isCurrentPage = currentUri == urlToPlay
        if (isCurrentPage) {
            isPlaying = controller?.isPlaying ?: false
            playbackState = controller?.playbackState ?: Player.STATE_IDLE
            duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            currentPosition = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
        }

        onDispose {
            controller?.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, isCurrentPage, playbackState, isVisible) {
        while (isPlaying && isCurrentPage && playbackState != Player.STATE_ENDED && isVisible) {
            currentPosition = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            delay(500)
        }
    }

    val controlsEnabled = isCurrentPage && controller != null && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)

    Box(modifier = modifier.fillMaxSize().background(Color.DarkGray)) {
        AsyncImage(
            model = song.imageLargeUrl ?: song.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        if (song.videoUrl == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        if (song.videoUrl != null && controller != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        // Required workaround for smooth synchronization in Compose environments.
                        setEnableComposeSurfaceSyncWorkaround(true)
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = if (isCurrentPage && isVisible) controller else null
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    val targetPlayer = if (isCurrentPage && isVisible) controller else null
                    if (view.player != targetPlayer) {
                        view.player = targetPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Reveal video only when it's the current song, ready to play, and screen is visible.
                        alpha = if (isCurrentPage && playbackState == Player.STATE_READY && isVisible) 1f else 0f
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = {
                        if (isCurrentPage) {
                            currentPosition = (it * duration).toLong()
                        }
                    },
                    onValueChangeFinished = {
                        if (isCurrentPage) {
                            controller?.seekTo(currentPosition)
                        }
                    },
                    enabled = controlsEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.height(24.dp).fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isCurrentPage) {
                                val newPosition = ((controller?.currentPosition ?: 0L) - 10000).coerceAtLeast(0)
                                controller?.seekTo(newPosition)
                                currentPosition = newPosition
                            }
                        },
                        enabled = controlsEnabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isCurrentPage) {
                                controller?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            }
                        },
                        enabled = isCurrentPage && controller != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isCurrentPage) {
                                val newPosition = ((controller?.currentPosition ?: 0L) + 10000).coerceAtMost(duration)
                                controller?.seekTo(newPosition)
                                currentPosition = newPosition
                            }
                        },
                        enabled = controlsEnabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
