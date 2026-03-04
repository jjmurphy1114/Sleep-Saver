package com.example.sleepsaver.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sleepsaver.SleepSaverApplication
import com.example.sleepsaver.domain.InferenceInputs
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SleepCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SleepSaverApplication
        val snapshot = app.appContainer.snapshotStore.read()
        val latestSettings = app.appContainer.settingsRepository.settings.first()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val shouldEnter = app.appContainer.inferenceEngine.shouldEnterSleep(
            InferenceInputs(
                ambientLightLux = snapshot.lightLux,
                isFaceDown = snapshot.isFaceDown,
                isCharging = snapshot.isCharging,
                currentHour24 = hour,
                timestampMillis = System.currentTimeMillis()
            ),
            latestSettings
        )

        if (shouldEnter) {
            app.appContainer.sleepRepository.startSessionIfNeeded(System.currentTimeMillis())
        }

        return Result.success()
    }
}
