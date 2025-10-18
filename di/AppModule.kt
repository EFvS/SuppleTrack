package com.efvs.suppletrack.di

import android.content.Context
import androidx.room.Room
import com.efvs.suppletrack.data.local.SuppleTrackDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SuppleTrackDatabase =
        Room.databaseBuilder(
            context,
            SuppleTrackDatabase::class.java,
            "suppletrack_db"
        ).build()

    @Provides
    fun provideProfileDao(db: SuppleTrackDatabase) = db.profileDao()

    @Provides
    fun provideSupplementDao(db: SuppleTrackDatabase) = db.supplementDao()

    @Provides
    fun provideIntakeDao(db: SuppleTrackDatabase) = db.intakeDao()
}