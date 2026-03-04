package com.example.sleepsaver.platform

import android.app.NotificationManager
import android.content.Context
import android.os.Build

class DndController(context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun setEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!notificationManager.isNotificationPolicyAccessGranted) return
        notificationManager.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }
}

