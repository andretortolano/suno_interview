package com.suno.android.sunointerview.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.suno.android.sunointerview.data.local.db.SongDatabase
import com.suno.android.sunointerview.data.local.entities.RemoteKeyEntity
import com.suno.android.sunointerview.data.local.entities.SongEntity

@OptIn(ExperimentalPagingApi::class)
class SongRemoteMediator(
    private val apiService: ApiService,
    private val songDatabase: SongDatabase
) : RemoteMediator<Int, SongEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SongEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: 1
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        try {
            val response = apiService.getSongs(page = page, pageSize = state.config.pageSize)
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                val songs = apiResponse?.songs?.filterNotNull() ?: emptyList()
                val endOfPaginationReached = songs.isEmpty() || (apiResponse?.totalPages != null && page >= apiResponse.totalPages)

                songDatabase.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        songDatabase.remoteKeyDao().clearAll()
                        songDatabase.songDao().clearAll()
                    }
                    val prevKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPaginationReached) null else page + 1
                    val keys = songs.map {
                        RemoteKeyEntity(songId = it.clip?.id ?: "", prevKey = prevKey, nextKey = nextKey)
                    }
                    songDatabase.remoteKeyDao().insertAll(keys)
                    songDatabase.songDao().insertAll(songs.map { apiSong ->
                        SongEntity(
                            id = apiSong.clip?.id ?: "",
                            title = apiSong.clip?.title ?: "Unknown Title",
                            artist = apiSong.clip?.displayName ?: "Unknown Artist",
                            imageUrl = apiSong.clip?.imageUrl,
                            imageLargeUrl = apiSong.clip?.imageLargeUrl,
                            audioUrl = apiSong.clip?.audioUrl,
                            videoUrl = apiSong.clip?.videoUrl
                        )
                    })
                }
                return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            } else {
                return MediatorResult.Error(Exception("Failed to load songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, SongEntity>): RemoteKeyEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { song ->
                songDatabase.remoteKeyDao().getRemoteKeyBySongId(song.id)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, SongEntity>): RemoteKeyEntity? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { song ->
                songDatabase.remoteKeyDao().getRemoteKeyBySongId(song.id)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, SongEntity>): RemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { songId ->
                songDatabase.remoteKeyDao().getRemoteKeyBySongId(songId)
            }
        }
    }
}
