package com.tidesleep.app.wearable

import com.xiaomi.wearable.DataQueryResult
import com.xiaomi.wearable.DataSubscribeResult

/**
 * 将小米穿戴 SDK 睡眠/连接返回值映射为应用内 [WearableSleepState]。
 */
object XiaomiSleepStateMapper {

    fun fromQuerySleepStatus(status: Int): WearableSleepState = when (status) {
        DataSubscribeResult.RESULT_SLEEP_ASLEEP -> WearableSleepState.Asleep
        DataSubscribeResult.RESULT_SLEEP_AWAKE -> WearableSleepState.Awake
        else -> WearableSleepState.Unknown
    }

    fun fromQueryResult(result: DataQueryResult): WearableSleepState =
        fromQuerySleepStatus(result.sleepStatus)

    fun fromSubscribeResult(data: DataSubscribeResult): WearableSleepState =
        fromQuerySleepStatus(data.sleepStatus)

    fun fromConnectionStatus(connected: Boolean): WearableConnectionState =
        if (connected) WearableConnectionState.Connected else WearableConnectionState.Disconnected
}
