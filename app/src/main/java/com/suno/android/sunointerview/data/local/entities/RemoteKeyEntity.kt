package com.suno.android.sunointerview.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey
    val songId: String,
    val prevKey: Int?,
    val nextKey: Int?
)
