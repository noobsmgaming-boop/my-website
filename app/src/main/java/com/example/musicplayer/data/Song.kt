package com.example.musicplayer.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** A local audio file discovered through MediaStore. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .build()
        )
        .build()
}
