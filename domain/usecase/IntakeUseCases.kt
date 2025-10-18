package com.efvs.suppletrack.domain.usecase

import com.efvs.suppletrack.data.local.IntakeEntity
import com.efvs.suppletrack.data.repository.IntakeRepository
import kotlinx.coroutines.flow.Flow

data class IntakeUseCases(
    val getIntakesForSupplement: GetIntakesForSupplement,
    val getIntakesForProfileInRange: GetIntakesForProfileInRange,
    val insertIntake: InsertIntake,
    val updateIntake: UpdateIntake,
    val deleteIntake: DeleteIntake,
)

class GetIntakesForSupplement(private val repository: IntakeRepository) {
    operator fun invoke(supplementId: Long): Flow<List<IntakeEntity>> =
        repository.getIntakesForSupplement(supplementId)
}

class GetIntakesForProfileInRange(private val repository: IntakeRepository) {
    operator fun invoke(profileId: Long, start: Long, end: Long): Flow<List<IntakeEntity>> =
        repository.getIntakesForProfileInRange(profileId, start, end)
}

class InsertIntake(private val repository: IntakeRepository) {
    suspend operator fun invoke(intake: IntakeEntity): Long = repository.insertIntake(intake)
}

class UpdateIntake(private val repository: IntakeRepository) {
    suspend operator fun invoke(intake: IntakeEntity) = repository.updateIntake(intake)
}

class DeleteIntake(private val repository: IntakeRepository) {
    suspend operator fun invoke(intake: IntakeEntity) = repository.deleteIntake(intake)
}