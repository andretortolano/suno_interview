package com.suno.android.sunointerview.feature.player

import android.os.Bundle
import com.suno.android.sunointerview.domain.model.Song
import java.net.URLDecoder
import java.net.URLEncoder

data class PlayerScreenParams(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val imageUrl: String
) {
    fun toRoute(): String {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val encodedAudioUrl = URLEncoder.encode(audioUrl, "UTF-8")
        val encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8")
        return "player/$encodedTitle/$encodedArtist/$encodedAudioUrl/$encodedImageUrl"
    }

    companion object {
        fun from(song: Song): PlayerScreenParams {
            return PlayerScreenParams(
                title = song.title,
                artist = song.artist,
                audioUrl = song.audioUrl ?: "",
                imageUrl = song.imageUrl ?: ""
            )
        }

        fun fromUrlArguments(arguments: Bundle?): PlayerScreenParams {
            return PlayerScreenParams(
                title = URLDecoder.decode(arguments?.getString("songTitle") ?: "", "UTF-8"),
                artist = URLDecoder.decode(arguments?.getString("artistName") ?: "", "UTF-8"),
                audioUrl = URLDecoder.decode(arguments?.getString("audioUrl") ?: "", "UTF-8"),
                imageUrl = URLDecoder.decode(arguments?.getString("imageUrl") ?: "", "UTF-8")
            )
        }
    }
}
