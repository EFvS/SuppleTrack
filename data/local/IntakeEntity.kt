package com.efvs.suppletrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intakes")
data class IntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val profileId: Long,
    val intakeTime: Long,
    val taken: Boolean,
    val takenAt: Long? // null if not taken yet
)