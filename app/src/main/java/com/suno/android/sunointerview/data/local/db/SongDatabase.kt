package com.suno.android.sunointerview.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.suno.android.sunointerview.data.local.dao.RemoteKeyDao
import com.suno.android.sunointerview.data.local.dao.SongDao
import com.suno.android.sunointerview.data.local.entities.RemoteKeyEntity
import com.suno.android.sunointerview.data.local.entities.SongEntity

@Database(
    entities = [SongEntity::class, RemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun remoteKeyDao(): RemoteKeyDao

    companion object {
        @Volatile
        private var INSTANCE: SongDatabase? = null

        fun getInstance(context: Context): SongDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SongDatabase::class.java,
                    "suno_songs_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
