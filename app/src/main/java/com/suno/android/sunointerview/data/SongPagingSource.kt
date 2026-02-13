package com.suno.android.sunointerview.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suno.android.sunointerview.domain.model.Song as DomainSong

class SongPagingSource(
    private val apiService: ApiService
) : PagingSource<Int, DomainSong>() {

    override fun getRefreshKey(state: PagingState<Int, DomainSong>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DomainSong> {
        val page = params.key ?: 1
        return try {
            val response = apiService.getSongs(page = page, pageSize = params.loadSize)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                val songs = apiResponse?.songs?.filterNotNull()?.map { apiSong ->
                    DomainSong(
                        id = apiSong.clip?.id ?: "",
                        title = apiSong.clip?.title ?: "Unknown Title",
                        artist = apiSong.clip?.displayName ?: "Unknown Artist",
                        imageUrl = apiSong.clip?.imageUrl,
                        imageLargeUrl = apiSong.clip?.imageLargeUrl,
                        audioUrl = apiSong.clip?.audioUrl,
                        videoUrl = apiSong.clip?.videoUrl
                    )
                } ?: emptyList()
                
                val nextKey = if (songs.isEmpty() || (apiResponse?.totalPages != null && page >= apiResponse.totalPages)) {
                    null
                } else {
                    page + 1
                }
                LoadResult.Page(
                    data = songs,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Exception("Failed to load songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
