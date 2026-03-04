package com.example.sleepsaver.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SleepRepository(private val dao: SleepDao) {
    fun observeActiveSession(): Flow<SleepSessionEntity?> = dao.observeActiveSession()

    fun observeRecentSessions(limit: Int = 7): Flow<List<SleepSessionWithDisturbances>> =
        dao.observeRecentSessions(limit)

    suspend fun startSessionIfNeeded(startTime: Long): Long? {
        val active = dao.observeActiveSession().firstOrNull()
        if (active != null) return null
        return dao.insertSession(SleepSessionEntity(startTimeEpochMillis = startTime))
    }

    suspend fun endActiveSession(endTime: Long) {
        val active = dao.observeActiveSession().firstOrNull() ?: return
        dao.endSession(active.id, endTime)
    }

    suspend fun logDisturbance(timestamp: Long) {
        val active = dao.observeActiveSession().firstOrNull() ?: return
        dao.insertDisturbance(DisturbanceEventEntity(sessionId = active.id, timestampEpochMillis = timestamp))
    }

    suspend fun clearAllData() {
        dao.clearDisturbances()
        dao.clearSessions()
    }
}

