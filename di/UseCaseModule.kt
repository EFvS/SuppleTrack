package com.efvs.suppletrack.di

import com.efvs.suppletrack.data.repository.IntakeRepository
import com.efvs.suppletrack.data.repository.ProfileRepository
import com.efvs.suppletrack.data.repository.SupplementRepository
import com.efvs.suppletrack.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideProfileUseCases(repository: ProfileRepository): ProfileUseCases =
        ProfileUseCases(
            getAllProfiles = GetAllProfiles(repository),
            insertProfile = InsertProfile(repository),
            updateProfile = UpdateProfile(repository),
            deleteProfile = DeleteProfile(repository),
        )

    @Provides
    @Singleton
    fun provideSupplementUseCases(repository: SupplementRepository): SupplementUseCases =
        SupplementUseCases(
            getSupplementsForProfile = GetSupplementsForProfile(repository),
            insertSupplement = InsertSupplement(repository),
            updateSupplement = UpdateSupplement(repository),
            deleteSupplement = DeleteSupplement(repository),
        )

    @Provides
    @Singleton
    fun provideIntakeUseCases(repository: IntakeRepository): IntakeUseCases =
        IntakeUseCases(
            getIntakesForSupplement = GetIntakesForSupplement(repository),
            getIntakesForProfileInRange = GetIntakesForProfileInRange(repository),
            insertIntake = InsertIntake(repository),
            updateIntake = UpdateIntake(repository),
            deleteIntake = DeleteIntake(repository),
        )
}