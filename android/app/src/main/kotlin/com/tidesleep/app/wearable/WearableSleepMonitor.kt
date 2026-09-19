package com.tidesleep.app.wearable

import kotlinx.coroutines.flow.StateFlow

enum class WearableSleepState {
    Unknown,
    Awake,
    Asleep,
}

enum class WearableConnectionState {
    Disconnected,
    Connecting,
    Connected,
}

data class WearableDeviceInfo(
    val id: String,
    val displayName: String,
    val connectionState: WearableConnectionState,
    val sleepState: WearableSleepState,
)

interface WearableSleepMonitor {
    val devices: StateFlow<List<WearableDeviceInfo>>
    val sleepState: StateFlow<WearableSleepState>

    suspend fun start()
    suspend fun stop()

    /** 演示/测试用：手动注入入睡事件 */
    fun simulateSleepOnset()
    /** 演示/测试用：手动注入醒来事件 */
    fun simulateWake()
}
