package com.tidesleep.app.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tidesleep.app.TideSleepApplication
import com.tidesleep.app.wearable.SleepStateParser

/**
 * 接收米家自动化等外部广播：action [SleepStateParser.ACTION_SLEEP_STATE]。
 */
class SleepStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TideSleepApplication ?: return
        val state = SleepStateParser.parseIntent(intent) ?: return
        app.deliverExternalSleepState(state)
    }
}
