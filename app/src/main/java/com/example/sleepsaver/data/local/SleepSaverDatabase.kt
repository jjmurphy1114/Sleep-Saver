/*
 * Jacob Murphy
 */

package com.example.sleepsaver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SleepSessionEntity::class, DisturbanceEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SleepSaverDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
}

