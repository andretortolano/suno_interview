package com.suno.android.sunointerview.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val imageLargeUrl: String?,
    val audioUrl: String?,
    val videoUrl: String?
)
