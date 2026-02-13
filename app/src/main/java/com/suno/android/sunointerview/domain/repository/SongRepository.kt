package com.suno.android.sunointerview.domain.repository

import androidx.paging.PagingData
import com.suno.android.sunointerview.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<PagingData<Song>>
}
