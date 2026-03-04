package com.example.sleepsaver

import android.content.Context
import androidx.room.Room
import com.example.sleepsaver.data.local.SleepRepository
import com.example.sleepsaver.data.local.SleepSaverDatabase
import com.example.sleepsaver.data.settings.ContextSnapshotStore
import com.example.sleepsaver.data.settings.SettingsRepository
import com.example.sleepsaver.domain.SleepInferenceEngine
import com.example.sleepsaver.platform.DndController
import com.example.sleepsaver.platform.EnvironmentSensorMonitor

class AppContainer(private val context: Context) {
    private val database: SleepSaverDatabase by lazy {
        Room.databaseBuilder(context, SleepSaverDatabase::class.java, "sleep_saver.db").build()
    }

    val sleepRepository: SleepRepository by lazy { SleepRepository(database.sleepDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val snapshotStore: ContextSnapshotStore by lazy { ContextSnapshotStore(context) }
    val sensorMonitor: EnvironmentSensorMonitor by lazy {
        EnvironmentSensorMonitor(context, snapshotStore)
    }
    val dndController: DndController by lazy { DndController(context) }
    val inferenceEngine: SleepInferenceEngine = SleepInferenceEngine()
}

