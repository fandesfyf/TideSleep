package com.tidesleep.app.bridge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.tidesleep.app.TideSleepApplication
import com.tidesleep.app.wearable.SleepStateParser

/**
 * 处理 tidesleep://sleep?state=asleep|awake 深链，更新睡眠状态后结束。
 * 米家自动化「打开链接」动作可指向此 Activity。
 */
class SleepBridgeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val app = application as? TideSleepApplication ?: return
        val state = SleepStateParser.parseIntent(intent) ?: return
        app.deliverExternalSleepState(state)
    }
}
