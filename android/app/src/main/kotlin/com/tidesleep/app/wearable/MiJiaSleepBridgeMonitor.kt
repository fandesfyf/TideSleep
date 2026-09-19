package com.tidesleep.app.wearable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * 米家自动化深链 / 广播桥接：由外部自动化写入入睡/醒来，无需小米穿戴 AAR。
 */
class MiJiaSleepBridgeMonitor : WearableSleepMonitor, SleepStateSink {

    private val _sleepState = MutableStateFlow(WearableSleepState.Unknown)
    override val sleepState: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private val _devices = MutableStateFlow(
        listOf(
            WearableDeviceInfo(
                id = "mijia-bridge",
                displayName = "米家自动化桥接",
                connectionState = WearableConnectionState.Disconnected,
                sleepState = WearableSleepState.Unknown,
            )
        )
    )
    override val devices: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    private val _lastEventAt = MutableStateFlow<Instant?>(null)
    val lastEventAt: StateFlow<Instant?> = _lastEventAt.asStateFlow()

    private var listening = false

    override suspend fun start() {
        listening = true
        _devices.value = _devices.value.map {
            it.copy(
                connectionState = WearableConnectionState.Connected,
                sleepState = _sleepState.value,
            )
        }
    }

    override suspend fun stop() {
        listening = false
        _devices.value = _devices.value.map {
            it.copy(connectionState = WearableConnectionState.Disconnected)
        }
    }

    override fun onExternalSleepState(state: WearableSleepState) {
        _sleepState.value = state
        _lastEventAt.value = Instant.now()
        _devices.value = _devices.value.map {
            it.copy(
                connectionState = if (listening) {
                    WearableConnectionState.Connected
                } else {
                    WearableConnectionState.Disconnected
                },
                sleepState = state,
            )
        }
    }

    override fun simulateSleepOnset() {
        onExternalSleepState(WearableSleepState.Asleep)
    }

    override fun simulateWake() {
        onExternalSleepState(WearableSleepState.Awake)
    }
}
