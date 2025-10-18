package com.efvs.suppletrack.data.repository

import com.efvs.suppletrack.data.local.ProfileDao
import com.efvs.suppletrack.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val dao: ProfileDao
) {
    fun getAllProfiles(): Flow<List<ProfileEntity>> = dao.getAllProfiles()

    suspend fun insertProfile(profile: ProfileEntity): Long = dao.insertProfile(profile)

    suspend fun updateProfile(profile: ProfileEntity) = dao.updateProfile(profile)

    suspend fun deleteProfile(profile: ProfileEntity) = dao.deleteProfile(profile)
}