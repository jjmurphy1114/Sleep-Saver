/*
 * Jacob Murphy
 */

package com.example.sleepsaver.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long? = null
)

