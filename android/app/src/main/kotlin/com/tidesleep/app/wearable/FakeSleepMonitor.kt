package com.tidesleep.app.wearable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 演示用假穿戴：用于无真机时验证会话状态机与音频脉冲。
 */
class FakeSleepMonitor : WearableSleepMonitor {

    private val _sleepState = MutableStateFlow(WearableSleepState.Awake)
    override val sleepState: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private val _devices = MutableStateFlow(
        listOf(
            WearableDeviceInfo(
                id = "fake-xiaomi-band",
                displayName = "演示手环（Fake）",
                connectionState = WearableConnectionState.Connected,
                sleepState = WearableSleepState.Awake,
            )
        )
    )
    override val devices: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    override suspend fun start() {
        _devices.value = _devices.value.map {
            it.copy(connectionState = WearableConnectionState.Connected)
        }
    }

    override suspend fun stop() {
        _sleepState.value = WearableSleepState.Awake
        _devices.value = _devices.value.map {
            it.copy(sleepState = WearableSleepState.Awake)
        }
    }

    override fun simulateSleepOnset() {
        _sleepState.value = WearableSleepState.Asleep
        _devices.value = _devices.value.map {
            it.copy(sleepState = WearableSleepState.Asleep)
        }
    }

    override fun simulateWake() {
        _sleepState.value = WearableSleepState.Awake
        _devices.value = _devices.value.map {
            it.copy(sleepState = WearableSleepState.Awake)
        }
    }
}
