package com.suno.android.sunointerview.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.suno.android.sunointerview.data.ApiService
import com.suno.android.sunointerview.data.SongRemoteMediator
import com.suno.android.sunointerview.data.local.db.SongDatabase
import com.suno.android.sunointerview.domain.model.Song
import com.suno.android.sunointerview.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val songDatabase: SongDatabase
) : SongRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getSongs(): Flow<PagingData<Song>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            remoteMediator = SongRemoteMediator(apiService, songDatabase),
            pagingSourceFactory = { songDatabase.songDao().getSongs() }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    imageUrl = entity.imageUrl,
                    imageLargeUrl = entity.imageLargeUrl,
                    audioUrl = entity.audioUrl,
                    videoUrl = entity.videoUrl
                )
            }
        }
    }
}
