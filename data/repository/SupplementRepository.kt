package com.efvs.suppletrack.data.repository

import com.efvs.suppletrack.data.local.SupplementDao
import com.efvs.suppletrack.data.local.SupplementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SupplementRepository @Inject constructor(
    private val dao: SupplementDao
) {
    fun getSupplementsForProfile(profileId: Long): Flow<List<SupplementEntity>> =
        dao.getSupplementsForProfile(profileId)

    suspend fun insertSupplement(supplement: SupplementEntity): Long = dao.insertSupplement(supplement)

    suspend fun updateSupplement(supplement: SupplementEntity) = dao.updateSupplement(supplement)

    suspend fun deleteSupplement(supplement: SupplementEntity) = dao.deleteSupplement(supplement)
}