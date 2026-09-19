package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SafetyPreset
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TideWarning
import com.tidesleep.app.viewmodel.TideSleepViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetySettingsScreen(
    viewModel: TideSleepViewModel,
    onBack: () -> Unit,
) {
    val config by viewModel.safetyConfig.collectAsStateWithLifecycle()

    var delaySeconds by remember(config) {
        mutableIntStateOf(config.postSleepDelay.inWholeSeconds.toInt())
    }
    var maxMinutes by remember(config) {
        mutableIntStateOf(config.maxSessionDuration.inWholeMinutes.toInt().coerceAtLeast(1))
    }
    var volume by remember(config) {
        mutableFloatStateOf(config.volumePercent.toFloat())
    }
    var intervalMean by remember(config) {
        mutableIntStateOf(config.pulseIntervalMeanSeconds)
    }
    var selectedPreset by remember(config) {
        mutableIntStateOf(config.preset.ordinal)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("安全与刺激参数") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TideCard {
                Text(
                    text = "无 EEG 时为开放环，非论文闭环",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideWarning,
                    modifier = Modifier.padding(16.dp),
                )
            }

            SectionHeader(title = "安全预设", subtitle = "演示预设便于 1 分钟内完成 Fake 流程")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPreset == SafetyPreset.DEMO.ordinal,
                    onClick = {
                        selectedPreset = SafetyPreset.DEMO.ordinal
                        val demo = SafetyConfig.demo()
                        delaySeconds = demo.postSleepDelay.inWholeSeconds.toInt()
                        maxMinutes = demo.maxSessionDuration.inWholeMinutes.toInt()
                        volume = demo.volumePercent.toFloat()
                        viewModel.applyPreset(SafetyPreset.DEMO)
                    },
                    label = { Text("演示") },
                )
                FilterChip(
                    selected = selectedPreset == SafetyPreset.SCIENTIFIC.ordinal,
                    onClick = {
                        selectedPreset = SafetyPreset.SCIENTIFIC.ordinal
                        val sci = SafetyConfig.scientific()
                        delaySeconds = sci.postSleepDelay.inWholeSeconds.toInt()
                        maxMinutes = sci.maxSessionDuration.inWholeMinutes.toInt()
                        volume = sci.volumePercent.toFloat()
                        viewModel.applyPreset(SafetyPreset.SCIENTIFIC)
                    },
                    label = { Text("科学默认") },
                )
                FilterChip(
                    selected = selectedPreset == SafetyPreset.CUSTOM.ordinal,
                    onClick = { selectedPreset = SafetyPreset.CUSTOM.ordinal },
                    label = { Text("自定义") },
                )
            }

            OutlinedButton(
                onClick = { viewModel.playCalibrationPulse() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("试听一发脉冲")
            }

            SettingSlider(
                label = "入睡后延迟",
                valueText = formatDelayLabel(delaySeconds),
                value = delaySeconds.toFloat(),
                range = 10f..1800f,
                steps = 0,
                onValueChange = {
                    delaySeconds = it.toInt()
                    selectedPreset = SafetyPreset.CUSTOM.ordinal
                    persistCustom(viewModel, delaySeconds, maxMinutes, volume, intervalMean)
                },
            )

            SettingSlider(
                label = "单晚最长刺激",
                valueText = "${maxMinutes} 分钟",
                value = maxMinutes.toFloat(),
                range = 1f..120f,
                steps = 118,
                onValueChange = {
                    maxMinutes = it.toInt()
                    selectedPreset = SafetyPreset.CUSTOM.ordinal
                    persistCustom(viewModel, delaySeconds, maxMinutes, volume, intervalMean)
                },
            )

            SettingSlider(
                label = "脉冲间隔（均值）",
                valueText = "${intervalMean} 秒 ±2 秒抖动",
                value = intervalMean.toFloat(),
                range = 2f..8f,
                steps = 5,
                onValueChange = {
                    intervalMean = it.toInt()
                    selectedPreset = SafetyPreset.CUSTOM.ordinal
                    persistCustom(viewModel, delaySeconds, maxMinutes, volume, intervalMean)
                },
            )

            SettingSlider(
                label = "音量上限",
                valueText = "${volume.toInt()}%",
                value = volume,
                range = 5f..50f,
                steps = 8,
                onValueChange = {
                    volume = it
                    selectedPreset = SafetyPreset.CUSTOM.ordinal
                    persistCustom(viewModel, delaySeconds, maxMinutes, volume, intervalMean)
                },
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
}

private fun formatDelayLabel(seconds: Int): String {
    return if (seconds < 120) "${seconds} 秒" else "${seconds / 60} 分钟"
}

private fun persistCustom(
    viewModel: TideSleepViewModel,
    delaySeconds: Int,
    maxMinutes: Int,
    volume: Float,
    intervalMean: Int,
) {
    viewModel.updateSafetyConfigCustom(
        postSleepDelay = delaySeconds.seconds,
        maxSessionDuration = maxMinutes.minutes,
        volumePercent = volume.toInt(),
        pulseIntervalMeanSeconds = intervalMean,
    )
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
