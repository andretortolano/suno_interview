package com.suno.android.sunointerview.di

import android.content.Context
import com.suno.android.sunointerview.data.local.dao.SongDao
import com.suno.android.sunointerview.data.local.db.SongDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SongDatabase {
        return SongDatabase.getInstance(context)
    }

    @Provides
    fun provideSongDao(database: SongDatabase): SongDao {
        return database.songDao()
    }
}
