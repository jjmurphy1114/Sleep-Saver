package com.example.sleepsaver.domain

class SleepInferenceEngine {
    fun shouldEnterSleep(inputs: InferenceInputs, settings: AppSettings): Boolean {
        if (!settings.sleepDetectionEnabled) return false
        val inWindow = isWithinWindow(inputs.currentHour24, settings.sleepWindowStartHour, settings.sleepWindowEndHour)
        val lowLight = (inputs.ambientLightLux ?: Float.MAX_VALUE) < settings.lightThresholdLux
        return lowLight && inputs.isFaceDown && inputs.isCharging && inWindow
    }

    fun shouldExitSleep(inputs: InferenceInputs, settings: AppSettings): Boolean {
        val inWindow = isWithinWindow(inputs.currentHour24, settings.sleepWindowStartHour, settings.sleepWindowEndHour)
        val bright = (inputs.ambientLightLux ?: 0f) > settings.lightThresholdLux * 2f
        return !inWindow || (!inputs.isCharging && bright)
    }

    fun shouldLogDisturbance(currentHour24: Int, isSleepMode: Boolean): Boolean {
        return isSleepMode && currentHour24 in 0..5
    }

    private fun isWithinWindow(hour: Int, start: Int, end: Int): Boolean {
        return if (start <= end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
    }
}

