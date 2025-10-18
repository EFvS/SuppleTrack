package com.efvs.suppletrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val dosage: String?,
    val note: String?,
    val icon: String,
    val color: Long,
    val intervalType: String?, // DAILY, EVERY_X_DAYS, WEEKDAYS, PRN
    val intervalData: String?, // JSON or CSV per type
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean = true
)