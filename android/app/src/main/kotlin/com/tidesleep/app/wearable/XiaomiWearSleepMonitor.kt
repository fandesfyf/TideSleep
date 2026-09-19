package com.tidesleep.app.wearable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 小米穿戴睡眠状态订阅 Stub。
 *
 * 正式集成需：
 * - Permission.DEVICE_MANAGER
 * - 小米穿戴第三方 SDK：query/subscribe 睡眠状态（入睡/出睡）
 *
 * 参见 docs/调研与实施方案.md §2.4 B 路径。
 */
class XiaomiWearSleepMonitor : WearableSleepMonitor {

    private val _sleepState = MutableStateFlow(WearableSleepState.Unknown)
    override val sleepState: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private val _devices = MutableStateFlow(
        listOf(
            WearableDeviceInfo(
                id = "xiaomi-watch-stub",
                displayName = "小米手表（待连接 SDK）",
                connectionState = WearableConnectionState.Disconnected,
                sleepState = WearableSleepState.Unknown,
            )
        )
    )
    override val devices: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    override suspend fun start() {
        _devices.value = _devices.value.map {
            it.copy(
                connectionState = WearableConnectionState.Connecting,
                sleepState = WearableSleepState.Unknown,
            )
        }
        // TODO: 接入小米穿戴 SDK subscribe 睡眠 DataItem
        _devices.value = _devices.value.map {
            it.copy(connectionState = WearableConnectionState.Disconnected)
        }
    }

    override suspend fun stop() {
        _sleepState.value = WearableSleepState.Unknown
    }

    override fun simulateSleepOnset() {
        // 真机 SDK 模式下不支持手动模拟
    }

    override fun simulateWake() {
        // 真机 SDK 模式下不支持手动模拟
    }
}
