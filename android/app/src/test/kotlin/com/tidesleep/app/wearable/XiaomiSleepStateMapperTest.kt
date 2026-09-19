package com.tidesleep.app.wearable

import com.xiaomi.wearable.DataQueryResult
import com.xiaomi.wearable.DataSubscribeResult
import org.junit.Assert.assertEquals
import org.junit.Test

class XiaomiSleepStateMapperTest {

    @Test
    fun fromQuerySleepStatus_mapsAsleepAndAwake() {
        assertEquals(
            WearableSleepState.Asleep,
            XiaomiSleepStateMapper.fromQuerySleepStatus(DataSubscribeResult.RESULT_SLEEP_ASLEEP),
        )
        assertEquals(
            WearableSleepState.Awake,
            XiaomiSleepStateMapper.fromQuerySleepStatus(DataSubscribeResult.RESULT_SLEEP_AWAKE),
        )
    }

    @Test
    fun fromQueryResult_usesSleepStatus() {
        val result = object : DataQueryResult {
            override fun isConnected(): Boolean = true
            override fun getSleepStatus(): Int = DataSubscribeResult.RESULT_SLEEP_ASLEEP
        }
        assertEquals(WearableSleepState.Asleep, XiaomiSleepStateMapper.fromQueryResult(result))
    }

    @Test
    fun fromSubscribeResult_usesSleepStatus() {
        val data = object : DataSubscribeResult {
            override fun getConnectedStatus(): Int = DataSubscribeResult.RESULT_CONNECTION_CONNECTED
            override fun getSleepStatus(): Int = DataSubscribeResult.RESULT_SLEEP_AWAKE
        }
        assertEquals(WearableSleepState.Awake, XiaomiSleepStateMapper.fromSubscribeResult(data))
    }

    @Test
    fun fromConnectionStatus_mapsBoolean() {
        assertEquals(
            WearableConnectionState.Connected,
            XiaomiSleepStateMapper.fromConnectionStatus(true),
        )
        assertEquals(
            WearableConnectionState.Disconnected,
            XiaomiSleepStateMapper.fromConnectionStatus(false),
        )
    }
}
