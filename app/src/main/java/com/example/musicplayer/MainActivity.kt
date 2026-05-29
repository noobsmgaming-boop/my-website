package com.example.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musicplayer.ads.BannerAd
import com.example.musicplayer.data.Song
import com.example.musicplayer.ui.MusicPlayerUiState
import com.example.musicplayer.ui.MusicPlayerViewModel
import com.example.musicplayer.ui.theme.KotlinMusicPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MusicPlayerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            KotlinMusicPlayerTheme(darkTheme = uiState.isDarkMode) {
                MusicPlayerApp(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@Composable
private fun MusicPlayerApp(viewModel: MusicPlayerViewModel, uiState: MusicPlayerUiState) {
    var hasAudioPermission by remember { mutableStateOf(false) }
    val permissions = remember { requiredRuntimePermissions() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPermission = permissions.audioPermission?.let { grants[it] == true } ?: true
        if (hasAudioPermission) viewModel.loadSongs()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.all.toTypedArray())
    }

    if (hasAudioPermission) {
        PlayerScreen(uiState = uiState, viewModel = viewModel)
    } else {
        PermissionScreen(onRequestPermission = { permissionLauncher.launch(permissions.all.toTypedArray()) })
    }
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("Allow music access", style = MaterialTheme.typography.headlineSmall)
            Text(
                "This player needs audio permission to build a playlist from songs stored on your phone.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRequestPermission) { Text("Grant permission") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(uiState: MusicPlayerUiState, viewModel: MusicPlayerViewModel) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Kotlin Music Player") },
                actions = {
                    IconButton(onClick = viewModel::toggleDarkMode) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle dark mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                NowPlayingBar(uiState = uiState, viewModel = viewModel)
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::search,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search songs, artists, or albums") }
            )
            Spacer(Modifier.height(12.dp))
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null -> Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                uiState.songs.isEmpty() -> EmptyPlaylist()
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isPlaying = uiState.playbackState.mediaId == song.id.toString(),
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylist() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No songs found on this device.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SongRow(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(song = song)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDuration(song.durationMillis), style = MaterialTheme.typography.labelMedium)
            }
            if (isPlaying) {
                Icon(Icons.Default.MusicNote, contentDescription = "Currently playing")
            }
        }
    }
}

@Composable
private fun AlbumArt(song: Song) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Album art for ${song.album}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (song.albumArtUri == null) {
            Icon(Icons.Default.MusicNote, contentDescription = null)
        }
    }
}

@Composable
private fun NowPlayingBar(uiState: MusicPlayerUiState, viewModel: MusicPlayerViewModel) {
    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.playbackState.title.ifBlank { "Select a song" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.playbackState.artist.ifBlank { "Playlist ready" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = viewModel::previous) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous") }
            FilledTonalIconButton(onClick = viewModel::playPause) {
                Icon(
                    imageVector = if (uiState.playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.playbackState.isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = viewModel::next) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
        }
    }
}

private data class RuntimePermissions(val audioPermission: String?, val all: List<String>)

private fun requiredRuntimePermissions(): RuntimePermissions {
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissions = buildList {
        add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return RuntimePermissions(audioPermission, permissions)
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
