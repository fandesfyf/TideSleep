package com.tidesleep.app.wearable

/**
 * 外部 Intent / 深链向睡眠监控器投递状态的回调。
 */
fun interface SleepStateSink {
    fun onExternalSleepState(state: WearableSleepState)
}
