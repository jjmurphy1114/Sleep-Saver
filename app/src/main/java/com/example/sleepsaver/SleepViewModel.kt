/*
 * Jacob Murphy
 */

package com.example.sleepsaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sleepsaver.data.local.SleepRepository
import com.example.sleepsaver.data.local.SleepSessionWithDisturbances
import com.example.sleepsaver.data.settings.SettingsRepository
import com.example.sleepsaver.domain.AppSettings
import com.example.sleepsaver.domain.InferenceInputs
import com.example.sleepsaver.domain.SleepInferenceEngine
import com.example.sleepsaver.platform.DndController
import com.example.sleepsaver.platform.EnvironmentSensorMonitor
import com.example.sleepsaver.platform.EnvironmentSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardNightSummary(
    val sessionId: Long,
    val startLabel: String,
    val durationMinutes: Long,
    val disturbances: Int
)

data class DashboardUiState(
    val isSleepModeActive: Boolean = false,
    val tonightStartTime: String = "--",
    val disturbanceCountTonight: Int = 0,
    val consistencyScore: Int = 0,
    val weeklySummary: List<DashboardNightSummary> = emptyList(),
    val ambientLightLabel: String = "Unknown",
    val chargingLabel: String = "Not Charging",
    val faceDownLabel: String = "Not Face Down"
)

class SleepViewModel(
    private val sleepRepository: SleepRepository,
    private val settingsRepository: SettingsRepository,
    private val sensorMonitor: EnvironmentSensorMonitor,
    private val dndController: DndController,
    private val inferenceEngine: SleepInferenceEngine
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    private val activeSession = sleepRepository.observeActiveSession().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    private val recentSessions = sleepRepository.observeRecentSessions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val ticker = MutableStateFlow(System.currentTimeMillis())

    val dashboard: StateFlow<DashboardUiState> = combine(
        activeSession,
        recentSessions,
        sensorMonitor.snapshot,
        ticker
    ) { activeSession, recentSessions, snapshot, _ ->
        val tonight = activeSession?.startTimeEpochMillis?.let { formatTime(it) } ?: "--"
        val disturbanceCount = activeSession?.id?.let { id ->
            recentSessions.firstOrNull { it.session.id == id }?.disturbances?.size
        } ?: 0
        DashboardUiState(
            isSleepModeActive = activeSession != null,
            tonightStartTime = tonight,
            disturbanceCountTonight = disturbanceCount,
            consistencyScore = calculateConsistencyScore(recentSessions),
            weeklySummary = recentSessions.map { it.toSummary() },
            ambientLightLabel = snapshot.ambientLightLux?.let { "${it.toInt()} lux" } ?: "Unknown",
            chargingLabel = if (snapshot.isCharging) "Charging" else "Not Charging",
            faceDownLabel = if (snapshot.isFaceDown) "Face Down" else "Not Face Down"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        sensorMonitor.start()

        viewModelScope.launch {
            while (true) {
                ticker.value = System.currentTimeMillis()
                evaluateSleepInference(sensorMonitor.snapshot.value, settings.value)
                delay(60_000)
            }
        }

        viewModelScope.launch {
            sensorMonitor.disturbanceEvents.collect { timestamp ->
                val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
                val isSleepModeActive = activeSession.value != null
                if (isSleepModeActive) {
                    sleepRepository.logDisturbance(timestamp)
                }
            }
        }
    }

    fun onDetectionEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDetectionEnabled(enabled) }
    }

    fun onLightThresholdChanged(value: Float) {
        viewModelScope.launch { settingsRepository.setLightThreshold(value) }
    }

    fun onWindowStartChanged(hour: Int) {
        viewModelScope.launch { settingsRepository.setWindowStart(hour.coerceIn(0, 23)) }
    }

    fun onWindowEndChanged(hour: Int) {
        viewModelScope.launch { settingsRepository.setWindowEnd(hour.coerceIn(0, 23)) }
    }

    fun onAutoDndChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoDnd(enabled) }
    }

    fun onDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { sleepRepository.clearAllData() }
    }

    override fun onCleared() {
        sensorMonitor.stop()
        super.onCleared()
    }

    private suspend fun evaluateSleepInference(snapshot: EnvironmentSnapshot, settings: AppSettings) {
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val inputs = InferenceInputs(
            ambientLightLux = snapshot.ambientLightLux,
            isFaceDown = snapshot.isFaceDown,
            isCharging = snapshot.isCharging,
            currentHour24 = hour,
            timestampMillis = now
        )

        val isSleepMode = activeSession.value != null
        if (!isSleepMode && inferenceEngine.shouldEnterSleep(inputs, settings)) {
            sleepRepository.startSessionIfNeeded(now)
            if (settings.autoDndEnabled) {
                dndController.setEnabled(true)
            }
            return
        }

        if (isSleepMode && inferenceEngine.shouldExitSleep(inputs, settings)) {
            sleepRepository.endActiveSession(now)
            if (settings.autoDndEnabled) {
                dndController.setEnabled(false)
            }
        }
    }

    private fun calculateConsistencyScore(sessions: List<SleepSessionWithDisturbances>): Int {
        if (sessions.size < 2) return 100
        val starts = sessions.map { it.session.startTimeEpochMillis }.sorted()
        val diffs = starts.zipWithNext { a, b -> kotlin.math.abs(a - b) }
        val averageDiffMinutes = diffs.average() / 60_000.0
        return (100 - averageDiffMinutes.toInt().coerceAtMost(100)).coerceAtLeast(0)
    }

    private fun SleepSessionWithDisturbances.toSummary(): DashboardNightSummary {
        val duration = ((session.endTimeEpochMillis ?: System.currentTimeMillis()) - session.startTimeEpochMillis) / 60_000
        return DashboardNightSummary(
            sessionId = session.id,
            startLabel = formatTime(session.startTimeEpochMillis),
            durationMinutes = duration.coerceAtLeast(0),
            disturbances = disturbances.size
        )
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

class SleepViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepViewModel(
                sleepRepository = container.sleepRepository,
                settingsRepository = container.settingsRepository,
                sensorMonitor = container.sensorMonitor,
                dndController = container.dndController,
                inferenceEngine = container.inferenceEngine
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
