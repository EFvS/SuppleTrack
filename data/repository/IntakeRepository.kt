package com.efvs.suppletrack.data.repository

import com.efvs.suppletrack.data.local.IntakeDao
import com.efvs.suppletrack.data.local.IntakeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IntakeRepository @Inject constructor(
    private val dao: IntakeDao
) {
    fun getIntakesForSupplement(supplementId: Long): Flow<List<IntakeEntity>> =
        dao.getIntakesForSupplement(supplementId)

    fun getIntakesForProfileInRange(profileId: Long, start: Long, end: Long): Flow<List<IntakeEntity>> =
        dao.getIntakesForProfileInRange(profileId, start, end)

    suspend fun insertIntake(intake: IntakeEntity): Long = dao.insertIntake(intake)

    suspend fun updateIntake(intake: IntakeEntity) = dao.updateIntake(intake)

    suspend fun deleteIntake(intake: IntakeEntity) = dao.deleteIntake(intake)
}