/*
 * Jacob Murphy
 */

package com.example.sleepsaver.domain

data class InferenceInputs(
    val ambientLightLux: Float?,
    val isFaceDown: Boolean,
    val isCharging: Boolean,
    val currentHour24: Int,
    val timestampMillis: Long
)
