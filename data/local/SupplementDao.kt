package com.efvs.suppletrack.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements WHERE profileId = :profileId")
    fun getSupplementsForProfile(profileId: Long): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :supplementId LIMIT 1")
    suspend fun getSupplementById(supplementId: Long): SupplementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    @Update
    suspend fun updateSupplement(supplement: SupplementEntity)

    @Delete
    suspend fun deleteSupplement(supplement: SupplementEntity)
}