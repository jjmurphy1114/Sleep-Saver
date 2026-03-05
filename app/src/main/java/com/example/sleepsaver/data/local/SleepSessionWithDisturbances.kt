/*
 * Jacob Murphy
 */

package com.example.sleepsaver.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SleepSessionWithDisturbances(
    @Embedded val session: SleepSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val disturbances: List<DisturbanceEventEntity>
)

