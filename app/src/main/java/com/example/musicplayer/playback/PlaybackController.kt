package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await

class PlaybackController(context: Context) {
    private val appContext = context.applicationContext
    private val controllerState = MutableStateFlow<MediaController?>(null)

    suspend fun connect() {
        if (controllerState.value != null) return
        val token = SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
        val controller = MediaController.Builder(appContext, token).buildAsync().await()
        controllerState.value = controller
    }

    suspend fun setPlaylist(items: List<MediaItem>, startIndex: Int) {
        val controller = controllerState.filterNotNull().first()
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playPause() {
        controllerState.value?.let { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    fun next() {
        controllerState.value?.seekToNextMediaItem()
    }

    fun previous() {
        controllerState.value?.seekToPreviousMediaItem()
    }

    fun release() {
        controllerState.value?.let(MediaController::release)
        controllerState.value = null
    }

    fun playerState(): Flow<PlaybackState> = callbackFlow {
        val controller = controllerState.filterNotNull().first()
        fun sendState() {
            trySend(
                PlaybackState(
                    isPlaying = controller.isPlaying,
                    mediaId = controller.currentMediaItem?.mediaId,
                    title = controller.mediaMetadata.title?.toString().orEmpty(),
                    artist = controller.mediaMetadata.artist?.toString().orEmpty()
                )
            )
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = sendState()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = sendState()
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) = sendState()
        }
        controller.addListener(listener)
        sendState()
        awaitClose { controller.removeListener(listener) }
    }
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = ""
)
