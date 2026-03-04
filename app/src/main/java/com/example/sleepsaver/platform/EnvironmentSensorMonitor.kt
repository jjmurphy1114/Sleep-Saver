package com.example.sleepsaver.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import com.example.sleepsaver.data.settings.ContextSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class EnvironmentSensorMonitor(
    context: Context,
    private val snapshotStore: ContextSnapshotStore
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _snapshot = MutableStateFlow(EnvironmentSnapshot())
    val snapshot: StateFlow<EnvironmentSnapshot> = _snapshot.asStateFlow()

    private val _disturbanceEvents = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val disturbanceEvents = _disturbanceEvents.asSharedFlow()

    private var isRegistered = false
    private var lastMagnitude = 0f

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> updateCharging(intent)
                Intent.ACTION_USER_PRESENT -> _disturbanceEvents.tryEmit(System.currentTimeMillis())
            }
        }
    }

    fun start() {
        if (isRegistered) return
        isRegistered = true
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        appContext.registerReceiver(
            stateReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT)
        )
        appContext.registerReceiver(
            stateReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    fun stop() {
        if (!isRegistered) return
        isRegistered = false
        sensorManager.unregisterListener(this)
        appContext.unregisterReceiver(stateReceiver)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> updateSnapshot(_snapshot.value.copy(ambientLightLux = event.values.firstOrNull(), lastUpdatedMillis = now))
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.firstOrNull() ?: return
                val isNear = distance < (proximitySensor?.maximumRange ?: distance)
                updateSnapshot(_snapshot.value.copy(isFaceDown = isNear, lastUpdatedMillis = now))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val magnitude = sqrt((x * x) + (y * y) + (z * z))
                if (lastMagnitude != 0f && abs(magnitude - lastMagnitude) > 6f) {
                    _disturbanceEvents.tryEmit(now)
                }
                lastMagnitude = magnitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateCharging(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        updateSnapshot(_snapshot.value.copy(isCharging = isCharging, lastUpdatedMillis = System.currentTimeMillis()))
    }

    private fun updateSnapshot(snapshot: EnvironmentSnapshot) {
        _snapshot.value = snapshot
        coroutineScope.launch {
            snapshotStore.update(
                lightLux = snapshot.ambientLightLux,
                isFaceDown = snapshot.isFaceDown,
                isCharging = snapshot.isCharging,
                now = snapshot.lastUpdatedMillis
            )
        }
    }
}

