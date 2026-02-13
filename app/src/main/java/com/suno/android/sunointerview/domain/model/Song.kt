package com.suno.android.sunointerview.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val imageLargeUrl: String?,
    val audioUrl: String?,
    val videoUrl: String?
)
