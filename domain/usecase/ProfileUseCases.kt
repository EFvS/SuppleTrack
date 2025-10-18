package com.efvs.suppletrack.domain.usecase

import com.efvs.suppletrack.data.local.ProfileEntity
import com.efvs.suppletrack.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

data class ProfileUseCases(
    val getAllProfiles: GetAllProfiles,
    val insertProfile: InsertProfile,
    val updateProfile: UpdateProfile,
    val deleteProfile: DeleteProfile,
)

class GetAllProfiles(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<List<ProfileEntity>> = repository.getAllProfiles()
}

class InsertProfile(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: ProfileEntity): Long = repository.insertProfile(profile)
}

class UpdateProfile(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: ProfileEntity) = repository.updateProfile(profile)
}

class DeleteProfile(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: ProfileEntity) = repository.deleteProfile(profile)
}