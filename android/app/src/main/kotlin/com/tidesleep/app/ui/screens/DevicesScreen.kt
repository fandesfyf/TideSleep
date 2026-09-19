package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.session.SessionPhase
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
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()

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
            SleepMonitorSource.FAKE -> FakeDemoCard(viewModel, sleepState, session.phase)
            SleepMonitorSource.MIJIA_BRIDGE -> MiJiaBridgeCard(onNavigateToScience, sleepState, viewModel, session.phase)
            SleepMonitorSource.XIAOMI_WEAR -> XiaomiSdkCard(onNavigateToScience)
        }
    }
}

@Composable
private fun FakeDemoCard(
    viewModel: TideSleepViewModel,
    sleepState: WearableSleepState,
    sessionPhase: SessionPhase,
) {
    TideCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("演示模式", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "当前睡眠状态：${sleepLabel(sleepState)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (sleepState == WearableSleepState.Asleep) TideSuccess else TideOnSurfaceMuted,
            )
            DebugSimulateSection(
                viewModel = viewModel,
                sessionPhase = sessionPhase,
                sleepState = sleepState,
            )
        }
    }
}

@Composable
private fun DebugSimulateSection(
    viewModel: TideSleepViewModel,
    sessionPhase: SessionPhase,
    sleepState: WearableSleepState,
) {
    var expanded by remember { mutableStateOf(false) }
    val sessionActive = sessionPhase != SessionPhase.Idle && sessionPhase != SessionPhase.Stopped

    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (expanded) "收起调试选项 ▲" else "展开调试选项 ▼")
    }

    if (expanded) {
        Text(
            text = "高级：入睡触发稀疏脉冲会话（非首页默认路径）。用于验证穿戴入睡/醒来流程。",
            style = MaterialTheme.typography.bodySmall,
            color = TideOnSurfaceMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    if (!sessionActive) viewModel.startSleepSession()
                    viewModel.simulateSleepOnset()
                },
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
        if (sessionActive) {
            OutlinedButton(
                onClick = { viewModel.stopSleepSession() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("停止入睡触发会话")
            }
        }
        Text(
            text = "会话：${phaseLabel(sessionPhase)} · 睡眠：${sleepLabel(sleepState)}",
            style = MaterialTheme.typography.bodySmall,
            color = TideOnSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MiJiaBridgeCard(
    onNavigateToScience: () -> Unit,
    sleepState: WearableSleepState,
    viewModel: TideSleepViewModel,
    sessionPhase: SessionPhase,
) {
    TideCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("米家自动化桥接", style = MaterialTheme.typography.titleMedium)
            Text(
                text = """
在米家创建自动化，将穿戴睡眠状态映射为深链：

• 睡着 → ${SleepStateParser.toDeepLink(WearableSleepState.Asleep)}
• 醒来 → ${SleepStateParser.toDeepLink(WearableSleepState.Awake)}

或使用广播 action：${SleepStateParser.ACTION_SLEEP_STATE}，extra「state」= asleep / awake。

高级模式：在设备页展开调试选项，启动「入睡触发稀疏脉冲」会话。
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = TideOnSurfaceMuted,
            )
            Text(
                text = "当前睡眠状态：${sleepLabel(sleepState)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (sleepState == WearableSleepState.Asleep) TideSuccess else TideOnSurfaceMuted,
            )
            DebugSimulateSection(
                viewModel = viewModel,
                sessionPhase = sessionPhase,
                sleepState = sleepState,
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

private fun phaseLabel(phase: SessionPhase): String = when (phase) {
    SessionPhase.Arming -> "准备中"
    SessionPhase.WaitingSleep -> "等待入睡"
    SessionPhase.WaitingDelay -> "入睡延迟中"
    SessionPhase.Stimulating -> "刺激进行中"
    SessionPhase.Stopped -> "已结束"
    SessionPhase.Idle -> "未开启"
}
