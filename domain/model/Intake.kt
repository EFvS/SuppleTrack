package com.efvs.suppletrack.domain.model

data class Intake(
    val id: Long = 0,
    val supplementId: Long,
    val profileId: Long,
    val intakeTime: Long,
    val taken: Boolean,
    val takenAt: Long?
)