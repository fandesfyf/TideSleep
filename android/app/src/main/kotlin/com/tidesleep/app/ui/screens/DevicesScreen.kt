package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TideSuccess
import com.tidesleep.app.viewmodel.TideSleepViewModel
import com.tidesleep.app.wearable.WearableConnectionState
import com.tidesleep.app.wearable.WearableSleepState

@Composable
fun DevicesScreen(viewModel: TideSleepViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val sleepState by viewModel.sleepState.collectAsStateWithLifecycle()
    val monitorSource = viewModel.monitorSource

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = "设备",
            subtitle = "小米手表/手环睡眠状态订阅",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = monitorSource == SleepMonitorSource.FAKE,
                onClick = { viewModel.switchMonitorSource(SleepMonitorSource.FAKE) },
                label = { Text("演示模式") },
            )
            FilterChip(
                selected = monitorSource == SleepMonitorSource.XIAOMI_WEAR,
                onClick = { viewModel.switchMonitorSource(SleepMonitorSource.XIAOMI_WEAR) },
                label = { Text("小米穿戴") },
            )
        }

        devices.forEach { device ->
            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "连接：${connectionLabel(device.connectionState)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                    )
                    Text(
                        text = "睡眠：${sleepLabel(device.sleepState)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (device.sleepState == WearableSleepState.Asleep) TideSuccess else TideOnSurfaceMuted,
                    )
                }
            }
        }

        if (monitorSource == SleepMonitorSource.FAKE) {
            TideCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "演示触发（Fake）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "在无真机时，手动模拟穿戴「入睡/醒来」以验证会话引擎。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.simulateSleepOnset() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("模拟入睡")
                        }
                        OutlinedButton(
                            onClick = { viewModel.simulateWake() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("模拟醒来")
                        }
                    }
                    Text(
                        text = "当前状态：${sleepLabel(sleepState)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        } else {
            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("小米穿戴 SDK", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "需集成小米穿戴第三方接口并授予 DEVICE_MANAGER 权限。当前为 Stub，请先在「演示模式」验证流程。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                    )
                }
            }
        }
    }
}

private fun connectionLabel(state: WearableConnectionState): String = when (state) {
    WearableConnectionState.Connected -> "已连接"
    WearableConnectionState.Connecting -> "连接中"
    WearableConnectionState.Disconnected -> "未连接"
}

private fun sleepLabel(state: WearableSleepState): String = when (state) {
    WearableSleepState.Asleep -> "睡着"
    WearableSleepState.Awake -> "清醒"
    WearableSleepState.Unknown -> "未知"
}
