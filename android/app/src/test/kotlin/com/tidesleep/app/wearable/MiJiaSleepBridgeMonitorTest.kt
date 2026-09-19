package com.tidesleep.app.wearable

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MiJiaSleepBridgeMonitorTest {

    @Test
    fun externalSleepState_updatesFlow() = runTest {
        val monitor = MiJiaSleepBridgeMonitor()
        monitor.start()

        monitor.onExternalSleepState(WearableSleepState.Asleep)
        assertEquals(WearableSleepState.Asleep, monitor.sleepState.value)

        monitor.onExternalSleepState(WearableSleepState.Awake)
        assertEquals(WearableSleepState.Awake, monitor.sleepState.value)
    }

    @Test
    fun simulateMethods_delegateToExternal() = runTest {
        val monitor = MiJiaSleepBridgeMonitor()
        monitor.simulateSleepOnset()
        assertEquals(WearableSleepState.Asleep, monitor.sleepState.value)
        monitor.simulateWake()
        assertEquals(WearableSleepState.Awake, monitor.sleepState.value)
    }
}
