package com.tidesleep.app.playback

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tidesleep.app.TideSleepApplication

class PinkNoiseStartWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TideSleepApplication
        app.onScheduledPinkNoiseStart()
        return Result.success()
    }

    companion object {
        const val KEY_TRIGGER_AT_MS = "trigger_at_ms"
    }
}
