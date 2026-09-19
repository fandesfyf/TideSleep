package com.tidesleep.app.wearable

import android.content.Context
import com.xiaomi.wearable.AuthApi
import com.xiaomi.wearable.DataItem
import com.xiaomi.wearable.Node
import com.xiaomi.wearable.NodeApi
import com.xiaomi.wearable.OnDataChangedListener
import com.xiaomi.wearable.OnFailureListener
import com.xiaomi.wearable.OnSuccessListener
import com.xiaomi.wearable.Permission
import com.xiaomi.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 小米穿戴睡眠状态订阅（官方 Wear SDK v1.4 API）。
 *
 * 需 Permission.DEVICE_MANAGER + NOTIFY，query/subscribe DataItem.ITEM_SLEEP。
 * 无 AAR 时编译仍通过（wear-stubs），运行时报错并提示放入 libs/。
 */
class XiaomiWearSleepMonitor(
    private val context: Context,
) : WearableSleepMonitor {

    private val _sleepState = MutableStateFlow(WearableSleepState.Unknown)
    override val sleepState: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private val _devices = MutableStateFlow<List<WearableDeviceInfo>>(emptyList())
    override val devices: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    private val _sdkError = MutableStateFlow<String?>(null)
    val sdkError: StateFlow<String?> = _sdkError.asStateFlow()

    private var nodeApi: NodeApi? = null
    private var authApi: AuthApi? = null
    private var activeNodeId: String? = null
    private var sleepListener: OnDataChangedListener? = null
    private var connectionListener: OnDataChangedListener? = null

    override suspend fun start() {
        if (!XiaomiWearSdk.isAvailable()) {
            _sdkError.value = XiaomiWearSdk.MISSING_AAR_MESSAGE
            _devices.value = listOf(
                WearableDeviceInfo(
                    id = "xiaomi-sdk-missing",
                    displayName = "小米穿戴（缺少 SDK AAR）",
                    connectionState = WearableConnectionState.Disconnected,
                    sleepState = WearableSleepState.Unknown,
                )
            )
            return
        }

        _sdkError.value = null
        _devices.value = listOf(
            WearableDeviceInfo(
                id = "xiaomi-connecting",
                displayName = "小米穿戴设备",
                connectionState = WearableConnectionState.Connecting,
                sleepState = WearableSleepState.Unknown,
            )
        )

        try {
            nodeApi = Wearable.getNodeApi(context)
            authApi = Wearable.getAuthApi(context)

            val nodes = awaitTask { nodeApi!!.getConnectedNodes() }
            if (nodes.isEmpty()) {
                _devices.value = listOf(
                    WearableDeviceInfo(
                        id = "xiaomi-none",
                        displayName = "未检测到已连接穿戴设备",
                        connectionState = WearableConnectionState.Disconnected,
                        sleepState = WearableSleepState.Unknown,
                    )
                )
                return
            }

            val node = nodes.first()
            val nodeId = node.id
            activeNodeId = nodeId

            val permissions = arrayOf(Permission.DEVICE_MANAGER, Permission.NOTIFY)
            val granted = awaitTask { authApi!!.requestPermission(nodeId, *permissions) }
            if (!granted.contains(Permission.DEVICE_MANAGER)) {
                _sdkError.value = "未获得 DEVICE_MANAGER 权限"
                return
            }

            val api = nodeApi!!
            val displayName = node.displayName ?: "小米穿戴设备"

            val connectionResult = awaitTask { api.query(nodeId, DataItem.ITEM_CONNECTION) }
            val connected = connectionResult.isConnected

            val sleepResult = awaitTask { api.query(nodeId, DataItem.ITEM_SLEEP) }
            val initialSleep = XiaomiSleepStateMapper.fromQueryResult(sleepResult)
            _sleepState.value = initialSleep

            _devices.value = listOf(
                WearableDeviceInfo(
                    id = nodeId,
                    displayName = displayName,
                    connectionState = XiaomiSleepStateMapper.fromConnectionStatus(connected),
                    sleepState = initialSleep,
                )
            )

            connectionListener = OnDataChangedListener { _, dataItem, data ->
                if (dataItem.type == DataItem.ITEM_CONNECTION.type) {
                    val isConnected = data.connectedStatus ==
                        com.xiaomi.wearable.DataSubscribeResult.RESULT_CONNECTION_CONNECTED
                    updateDeviceConnection(nodeId, displayName, isConnected)
                }
            }
            sleepListener = OnDataChangedListener { _, dataItem, data ->
                if (dataItem.type == DataItem.ITEM_SLEEP.type) {
                    val mapped = XiaomiSleepStateMapper.fromSubscribeResult(data)
                    _sleepState.value = mapped
                    updateDeviceSleep(nodeId, displayName, mapped)
                }
            }

            awaitTask { api.subscribe(nodeId, DataItem.ITEM_CONNECTION, connectionListener!!) }
            awaitTask { api.subscribe(nodeId, DataItem.ITEM_SLEEP, sleepListener!!) }
        } catch (e: Exception) {
            _sdkError.value = e.message ?: "小米穿戴 SDK 初始化失败"
            _devices.value = listOf(
                WearableDeviceInfo(
                    id = "xiaomi-error",
                    displayName = "小米穿戴连接失败",
                    connectionState = WearableConnectionState.Disconnected,
                    sleepState = WearableSleepState.Unknown,
                )
            )
        }
    }

    override suspend fun stop() {
        val nodeId = activeNodeId
        val api = nodeApi
        if (nodeId != null && api != null) {
            runCatching {
                connectionListener?.let { awaitTask { api.unsubscribe(nodeId, DataItem.ITEM_CONNECTION) } }
                sleepListener?.let { awaitTask { api.unsubscribe(nodeId, DataItem.ITEM_SLEEP) } }
            }
        }
        connectionListener = null
        sleepListener = null
        activeNodeId = null
        _sleepState.value = WearableSleepState.Unknown
    }

    override fun simulateSleepOnset() {
        // 真机 SDK 模式下不支持手动模拟
    }

    override fun simulateWake() {
        // 真机 SDK 模式下不支持手动模拟
    }

    private fun updateDeviceConnection(nodeId: String, displayName: String, connected: Boolean) {
        _devices.value = listOf(
            WearableDeviceInfo(
                id = nodeId,
                displayName = displayName,
                connectionState = XiaomiSleepStateMapper.fromConnectionStatus(connected),
                sleepState = _sleepState.value,
            )
        )
    }

    private fun updateDeviceSleep(nodeId: String, displayName: String, state: WearableSleepState) {
        val current = _devices.value.firstOrNull()
        _devices.value = listOf(
            WearableDeviceInfo(
                id = nodeId,
                displayName = displayName,
                connectionState = current?.connectionState ?: WearableConnectionState.Connected,
                sleepState = state,
            )
        )
    }

    private suspend fun <T> awaitTask(taskProvider: () -> com.xiaomi.wearable.WearableTask<T>): T =
        suspendCancellableCoroutine { cont ->
            taskProvider()
                .addOnSuccessListener(OnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result)
                })
                .addOnFailureListener(OnFailureListener { e ->
                    if (cont.isActive) cont.resumeWith(Result.failure(e))
                })
        }
}
