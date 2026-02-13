package com.suno.android.sunointerview.domain.usecase

import androidx.paging.PagingData
import com.suno.android.sunointerview.domain.model.Song
import com.suno.android.sunointerview.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(private val repository: SongRepository) {
    operator fun invoke(): Flow<PagingData<Song>> {
        return repository.getSongs()
    }
}
