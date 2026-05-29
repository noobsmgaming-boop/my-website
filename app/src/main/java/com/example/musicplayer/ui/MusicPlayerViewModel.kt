package com.example.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.Song
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val playbackController = PlaybackController(application)
    private val songs = MutableStateFlow<List<Song>>(emptyList())

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = combine(_uiState, songs) { state, allSongs ->
        val query = state.searchQuery.trim()
        state.copy(
            songs = if (query.isEmpty()) {
                allSongs
            } else {
                allSongs.filter { song ->
                    song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true)
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MusicPlayerUiState())

    init {
        viewModelScope.launch {
            playbackController.connect()
            playbackController.playerState().collect { playbackState ->
                _uiState.update { it.copy(playbackState = playbackState) }
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadSongs() }
                .onSuccess { loadedSongs ->
                    songs.value = loadedSongs
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load songs from this device."
                        )
                    }
                }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            val allSongs = songs.value
            val index = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playbackController.setPlaylist(allSongs.map { it.toMediaItem() }, index)
        }
    }

    fun playPause() = playbackController.playPause()
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}

data class MusicPlayerUiState(
    val songs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isDarkMode: Boolean = true,
    val errorMessage: String? = null,
    val playbackState: PlaybackState = PlaybackState()
)
