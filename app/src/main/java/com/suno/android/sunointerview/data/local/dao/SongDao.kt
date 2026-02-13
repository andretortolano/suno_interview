package com.suno.android.sunointerview.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.suno.android.sunointerview.data.local.entities.SongEntity

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs")
    fun getSongs(): PagingSource<Int, SongEntity>

    @Query("DELETE FROM songs")
    suspend fun clearAll()
}
