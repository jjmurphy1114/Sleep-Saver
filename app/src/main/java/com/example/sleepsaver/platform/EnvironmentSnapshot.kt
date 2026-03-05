/*
 * Jacob Murphy
 */

package com.example.sleepsaver.platform

data class EnvironmentSnapshot(
    val ambientLightLux: Float? = null,
    val isFaceDown: Boolean = false,
    val isCharging: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

