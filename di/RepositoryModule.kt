package com.efvs.suppletrack.di

import com.efvs.suppletrack.data.local.IntakeDao
import com.efvs.suppletrack.data.local.ProfileDao
import com.efvs.suppletrack.data.local.SupplementDao
import com.efvs.suppletrack.data.repository.IntakeRepository
import com.efvs.suppletrack.data.repository.ProfileRepository
import com.efvs.suppletrack.data.repository.SupplementRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProfileRepository(dao: ProfileDao): ProfileRepository =
        ProfileRepository(dao)

    @Provides
    @Singleton
    fun provideSupplementRepository(dao: SupplementDao): SupplementRepository =
        SupplementRepository(dao)

    @Provides
    @Singleton
    fun provideIntakeRepository(dao: IntakeDao): IntakeRepository =
        IntakeRepository(dao)
}