/*
 * Jacob Murphy
 */

package com.example.sleepsaver.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Insert
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Insert
    suspend fun insertDisturbance(event: DisturbanceEventEntity)

    @Query("UPDATE sleep_sessions SET endTimeEpochMillis = :endTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: Long)

    @Query("SELECT * FROM sleep_sessions WHERE endTimeEpochMillis IS NULL ORDER BY startTimeEpochMillis DESC LIMIT 1")
    fun observeActiveSession(): Flow<SleepSessionEntity?>

    @Transaction
    @Query("SELECT * FROM sleep_sessions ORDER BY startTimeEpochMillis DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<SleepSessionWithDisturbances>>

    @Query("SELECT COUNT(*) FROM disturbance_events WHERE sessionId = :sessionId")
    fun observeDisturbanceCount(sessionId: Long): Flow<Int>

    @Query("DELETE FROM disturbance_events")
    suspend fun clearDisturbances()

    @Query("DELETE FROM sleep_sessions")
    suspend fun clearSessions()
}

