package com.efvs.suppletrack.domain.usecase

import com.efvs.suppletrack.data.local.SupplementEntity
import com.efvs.suppletrack.data.repository.SupplementRepository
import kotlinx.coroutines.flow.Flow

data class SupplementUseCases(
    val getSupplementsForProfile: GetSupplementsForProfile,
    val insertSupplement: InsertSupplement,
    val updateSupplement: UpdateSupplement,
    val deleteSupplement: DeleteSupplement,
)

class GetSupplementsForProfile(private val repository: SupplementRepository) {
    operator fun invoke(profileId: Long): Flow<List<SupplementEntity>> =
        repository.getSupplementsForProfile(profileId)
}

class InsertSupplement(private val repository: SupplementRepository) {
    suspend operator fun invoke(supplement: SupplementEntity): Long = repository.insertSupplement(supplement)
}

class UpdateSupplement(private val repository: SupplementRepository) {
    suspend operator fun invoke(supplement: SupplementEntity) = repository.updateSupplement(supplement)
}

class DeleteSupplement(private val repository: SupplementRepository) {
    suspend operator fun invoke(supplement: SupplementEntity) = repository.deleteSupplement(supplement)
}