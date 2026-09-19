package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tidesleep.app.ui.theme.TideWarning
import com.tidesleep.app.viewmodel.TideSleepViewModel
import com.tidesleep.app.wearable.SleepStateParser
import com.tidesleep.app.wearable.WearableConnectionState
import com.tidesleep.app.wearable.WearableSleepState
import com.tidesleep.app.wearable.XiaomiWearSdk

@Composable
fun DevicesScreen(
    viewModel: TideSleepViewModel,
    onNavigateToScience: () -> Unit = {},
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val sleepState by viewModel.sleepState.collectAsStateWithLifecycle()
    val monitorSource by viewModel.monitorSource.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = "我的设备",
            subtitle = "管理睡眠设备与播放来源",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = monitorSource == SleepMonitorSource.FAKE,
                onClick = { viewModel.switchMonitorSource(SleepMonitorSource.FAKE) },
                label = { Text("演示模式") },
            )
            FilterChip(
                selected = monitorSource == SleepMonitorSource.MIJIA_BRIDGE,
                onClick = { viewModel.switchMonitorSource(SleepMonitorSource.MIJIA_BRIDGE) },
                label = { Text("米家自动化") },
            )
            FilterChip(
                selected = monitorSource == SleepMonitorSource.XIAOMI_WEAR,
                onClick = { viewModel.switchMonitorSource(SleepMonitorSource.XIAOMI_WEAR) },
                label = { Text("小米穿戴 SDK") },
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

        when (monitorSource) {
            SleepMonitorSource.FAKE -> FakeDemoCard(viewModel, sleepState)
            SleepMonitorSource.MIJIA_BRIDGE -> MiJiaBridgeCard(onNavigateToScience, sleepState)
            SleepMonitorSource.XIAOMI_WEAR -> XiaomiSdkCard(onNavigateToScience)
        }
    }
}

@Composable
private fun FakeDemoCard(viewModel: TideSleepViewModel, sleepState: WearableSleepState) {
    TideCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("演示触发（Fake）", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "手动模拟穿戴「入睡/醒来」以在约 1 分钟内验证完整流程。",
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
                text = "当前：${sleepLabel(sleepState)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun MiJiaBridgeCard(onNavigateToScience: () -> Unit, sleepState: WearableSleepState) {
    TideCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("米家自动化桥接", style = MaterialTheme.typography.titleMedium)
            Text(
                text = """
在米家创建自动化，将穿戴睡眠状态映射为深链：

• 睡着 → ${SleepStateParser.toDeepLink(WearableSleepState.Asleep)}
• 醒来 → ${SleepStateParser.toDeepLink(WearableSleepState.Awake)}

或使用广播 action：${SleepStateParser.ACTION_SLEEP_STATE}，extra「state」= asleep / awake。

请先「开启今晚」，再让自动化触发；前台服务会保持会话监听。
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = TideOnSurfaceMuted,
            )
            Text(
                text = "当前睡眠状态：${sleepLabel(sleepState)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (sleepState == WearableSleepState.Asleep) TideSuccess else TideOnSurfaceMuted,
            )
            OutlinedButton(onClick = onNavigateToScience, modifier = Modifier.fillMaxWidth()) {
                Text("查看米家配置向导")
            }
        }
    }
}

@Composable
private fun XiaomiSdkCard(onNavigateToScience: () -> Unit) {
    val sdkMissing = !XiaomiWearSdk.isAvailable()
    TideCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("小米穿戴 SDK", style = MaterialTheme.typography.titleMedium)
            if (sdkMissing) {
                Text(
                    text = XiaomiWearSdk.MISSING_AAR_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideWarning,
                )
                Text(
                    text = "将官方 AAR 放入 android/app/libs/ 后重新编译；或改用「米家自动化」。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideOnSurfaceMuted,
                )
            }
            Text(
                text = """
☐ 手环/手表绑定「小米运动健康」
☐ 开放平台申请第三方能力（包名 com.tidesleep.app）
☐ libs/ 放入官方 wearable AAR
☐ 授予 DEVICE_MANAGER + NOTIFY
☐ subscribe ITEM_SLEEP（入睡/出睡）
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = TideOnSurfaceMuted,
            )
            OutlinedButton(onClick = onNavigateToScience, modifier = Modifier.fillMaxWidth()) {
                Text("查看接入文档")
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
