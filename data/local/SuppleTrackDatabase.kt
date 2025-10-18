package com.efvs.suppletrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, SupplementEntity::class, IntakeEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SuppleTrackDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun supplementDao(): SupplementDao
    abstract fun intakeDao(): IntakeDao
}