package com.tidesleep.app.playback

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Schedules pink-noise playback at an exact wall-clock time.
 *
 * Prefers [AlarmManager] exact alarms when permitted (Android 12+ requires
 * [android.Manifest.permission.SCHEDULE_EXACT_ALARM] only when
 * [AlarmManager.canScheduleExactAlarms] is false we fall back to WorkManager).
 */
class PinkNoiseScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedulePlaybackAt(triggerAtEpochMs: Long) {
        cancel()
        if (canUseExactAlarm()) {
            scheduleExactAlarm(triggerAtEpochMs)
        } else {
            scheduleWorkManager(triggerAtEpochMs)
        }
    }

    fun cancel() {
        alarmManager.cancel(createAlarmPendingIntent())
        WorkManager.getInstance(context).cancelUniqueWork(WORK_UNIQUE_NAME)
    }

    private fun canUseExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleExactAlarm(triggerAtEpochMs: Long) {
        val pendingIntent = createAlarmPendingIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMs,
                pendingIntent,
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMs,
                pendingIntent,
            )
        }
    }

    private fun scheduleWorkManager(triggerAtEpochMs: Long) {
        val delayMs = max(0L, triggerAtEpochMs - System.currentTimeMillis())
        val request = OneTimeWorkRequestBuilder<PinkNoiseStartWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(PinkNoiseStartWorker.KEY_TRIGGER_AT_MS to triggerAtEpochMs))
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, PinkNoiseStartReceiver::class.java).apply {
            action = ACTION_START_PINK_NOISE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_START_PINK_NOISE = "com.tidesleep.app.action.START_PINK_NOISE"
        private const val REQUEST_CODE = 42_001
        private const val WORK_UNIQUE_NAME = "pink_noise_start"
        private const val WORK_TAG = "pink_noise_start_worker"
    }
}
