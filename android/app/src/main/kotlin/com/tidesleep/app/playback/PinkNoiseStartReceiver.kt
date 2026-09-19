package com.tidesleep.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tidesleep.app.TideSleepApplication

class PinkNoiseStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PinkNoiseScheduler.ACTION_START_PINK_NOISE) return
        val app = context.applicationContext as TideSleepApplication
        app.onScheduledPinkNoiseStart()
    }
}
