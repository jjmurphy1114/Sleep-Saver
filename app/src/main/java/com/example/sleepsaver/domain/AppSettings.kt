/*
 * Jacob Murphy
 */

package com.example.sleepsaver.domain

data class AppSettings(
    val sleepDetectionEnabled: Boolean = true,
    val lightThresholdLux: Float = 8f,
    val sleepWindowStartHour: Int = 22,
    val sleepWindowEndHour: Int = 2,
    val autoDndEnabled: Boolean = true,
    val manualDarkMode: Boolean = false
)

