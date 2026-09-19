package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.viewmodel.TideSleepViewModel
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetySettingsScreen(
    viewModel: TideSleepViewModel,
    onBack: () -> Unit,
) {
    val initial = viewModel.safetyConfig
    var delayMinutes by remember { mutableIntStateOf(initial.postSleepDelay.inWholeMinutes.toInt()) }
    var maxMinutes by remember { mutableIntStateOf(initial.maxSessionDuration.inWholeMinutes.toInt()) }
    var volume by remember { mutableFloatStateOf(initial.volumePercent.toFloat()) }
    var intervalMean by remember { mutableIntStateOf(initial.pulseIntervalMeanSeconds) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("安全设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = "开放环安全参数",
                subtitle = "默认对齐论文刺激时长，但无 EEG 相位锁定",
            )

            SettingSlider(
                label = "入睡后延迟",
                valueText = "${delayMinutes} 分钟",
                value = delayMinutes.toFloat(),
                range = 5f..30f,
                steps = 24,
                onValueChange = { delayMinutes = it.toInt() },
            )

            SettingSlider(
                label = "单晚最长刺激",
                valueText = "${maxMinutes} 分钟",
                value = maxMinutes.toFloat(),
                range = 30f..120f,
                steps = 17,
                onValueChange = { maxMinutes = it.toInt() },
            )

            SettingSlider(
                label = "脉冲间隔（均值）",
                valueText = "${intervalMean} 秒 ±2 秒抖动",
                value = intervalMean.toFloat(),
                range = 2f..8f,
                steps = 5,
                onValueChange = { intervalMean = it.toInt() },
            )

            SettingSlider(
                label = "音量上限",
                valueText = "${volume.toInt()}%",
                value = volume,
                range = 5f..50f,
                steps = 8,
                onValueChange = { volume = it },
            )

            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("固定参数", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "脉冲时长：50 ms 粉红噪声（1/f）\n停止条件：出睡 / 超时 / 手动",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

        }
    }

    LaunchedEffect(delayMinutes, maxMinutes, intervalMean, volume) {
        viewModel.updateSafetyConfig(
            SafetyConfig(
                postSleepDelay = delayMinutes.minutes,
                maxSessionDuration = maxMinutes.minutes,
                pulseDurationMs = 50,
                pulseIntervalMeanSeconds = intervalMean,
                pulseIntervalJitterSeconds = 2,
                volumePercent = volume.toInt(),
            )
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    TideCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = TideOnSurfaceMuted)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
