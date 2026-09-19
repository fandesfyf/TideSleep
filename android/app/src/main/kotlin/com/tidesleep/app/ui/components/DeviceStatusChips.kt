package com.tidesleep.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TideSuccess
import com.tidesleep.app.ui.theme.TideWarning
import com.tidesleep.app.wearable.WearableConnectionState
import com.tidesleep.app.wearable.WearableDeviceInfo
import com.tidesleep.app.wearable.XiaomiWearSdk

@Composable
fun DeviceStatusChips(
    devices: List<WearableDeviceInfo>,
    monitorSource: SleepMonitorSource,
    modifier: Modifier = Modifier,
) {
    val watchConnected = devices.any { it.connectionState == WearableConnectionState.Connected }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DeviceChip(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.Watch, null, Modifier.size(20.dp)) },
            title = "手表",
            status = when {
                monitorSource == SleepMonitorSource.FAKE && watchConnected -> "演示已连接"
                monitorSource == SleepMonitorSource.MIJIA_BRIDGE && watchConnected -> "米家桥接"
                monitorSource == SleepMonitorSource.MIJIA_BRIDGE -> "待配置自动化"
                monitorSource == SleepMonitorSource.XIAOMI_WEAR && watchConnected -> "已连接"
                monitorSource == SleepMonitorSource.XIAOMI_WEAR && !XiaomiWearSdk.isAvailable() ->
                    "需 SDK AAR"
                monitorSource == SleepMonitorSource.XIAOMI_WEAR -> "连接中"
                else -> "待连接"
            },
            isPositive = watchConnected,
        )
        DeviceChip(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.Speaker, null, Modifier.size(20.dp)) },
            title = "音箱",
            status = if (monitorSource == SleepMonitorSource.MIJIA_BRIDGE) "可选米家" else "米家待配置",
            isPositive = monitorSource == SleepMonitorSource.MIJIA_BRIDGE,
        )
        DeviceChip(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(20.dp)) },
            title = "手机",
            status = "扬声器就绪",
            isPositive = true,
        )
    }
}

@Composable
private fun DeviceChip(
    icon: @Composable () -> Unit,
    title: String,
    status: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    TideCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPositive) TideSuccess else TideWarning,
            )
        }
    }
}
