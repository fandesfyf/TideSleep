package com.tidesleep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.data.PlaybackStartMode
import com.tidesleep.app.data.PlaybackStartPreferences
import com.tidesleep.app.data.SafetyPreset
import com.tidesleep.app.playback.PlaybackArmedState
import com.tidesleep.app.playback.PlaybackStartController
import com.tidesleep.app.playback.PlaybackStartUiState
import com.tidesleep.app.ui.components.DeviceStatusChips
import com.tidesleep.app.ui.theme.TideBackground
import com.tidesleep.app.ui.theme.TideError
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TidePrimary
import com.tidesleep.app.ui.theme.TidePrimaryVariant
import com.tidesleep.app.ui.theme.TideSurface
import com.tidesleep.app.viewmodel.TideSleepViewModel
import java.util.Calendar

@Composable
fun TonightHomeScreen(
    viewModel: TideSleepViewModel,
    onNavigateToScience: () -> Unit,
    onNavigateToSafety: () -> Unit,
) {
    val playbackState by viewModel.playbackStartState.collectAsStateWithLifecycle()
    val safetyConfig by viewModel.safetyConfig.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val monitorSource by viewModel.monitorSource.collectAsStateWithLifecycle()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeHeader(
            statusText = homeStatusText(playbackState),
        )

        if (safetyConfig.preset == SafetyPreset.DEMO) {
            AssistChip(
                onClick = onNavigateToSafety,
                label = { Text("演示预设 · 音量 ${safetyConfig.volumePercent}%") },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!playbackState.isPlaying && !playbackState.isArmed) {
            StartModeSegmentedControl(
                selectedMode = playbackState.mode,
                enabled = disclaimerAccepted,
                onModeSelected = viewModel::setPlaybackStartMode,
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (playbackState.mode) {
                PlaybackStartMode.COUNTDOWN -> {
                    CountdownDurationPicker(
                        selectedMinutes = playbackState.countdownMinutes,
                        enabled = disclaimerAccepted,
                        onSelect = viewModel::setCountdownMinutes,
                    )
                }
                PlaybackStartMode.SCHEDULED -> {
                    ScheduledTimePicker(
                        hour = playbackState.scheduledHour,
                        minute = playbackState.scheduledMinute,
                        enabled = disclaimerAccepted,
                        onPickTime = { showTimePicker = true },
                    )
                }
                PlaybackStartMode.IMMEDIATE -> Unit
            }
        }

        Spacer(modifier = Modifier.weight(0.25f))

        TonightOrbButton(
            playbackState = playbackState,
            enabled = disclaimerAccepted && (!playbackState.isArmed || playbackState.isPlaying),
            onPrimaryClick = viewModel::onHomePrimaryAction,
        )

        ArmedOrPlayingCaption(
            playbackState = playbackState,
            volumePercent = safetyConfig.volumePercent,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = "低频粉红噪声，用于助眠环境",
            style = MaterialTheme.typography.bodySmall,
            color = TideOnSurfaceMuted.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (!disclaimerAccepted) {
            Text(
                text = "请先完成首次免责声明确认",
                style = MaterialTheme.typography.bodySmall,
                color = TidePrimaryVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (playbackState.isArmed) {
            TextButton(onClick = viewModel::cancelArmedPlayback) {
                Text("取消", color = TideOnSurfaceMuted)
            }
        } else if (playbackState.mode != PlaybackStartMode.IMMEDIATE) {
            TextButton(onClick = onNavigateToScience) {
                Text("了解原理", color = TidePrimaryVariant)
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        DeviceStatusChips(
            devices = devices,
            monitorSource = monitorSource,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (showTimePicker) {
        ScheduleTimePickerDialog(
            initialHour = playbackState.scheduledHour,
            initialMinute = playbackState.scheduledMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setScheduledTime(hour, minute)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun HomeHeader(statusText: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("汐眠", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "TideSleep",
            style = MaterialTheme.typography.labelLarge,
            color = TideOnSurfaceMuted,
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun StartModeSegmentedControl(
    selectedMode: PlaybackStartMode,
    enabled: Boolean,
    onModeSelected: (PlaybackStartMode) -> Unit,
) {
    val modes = listOf(
        PlaybackStartMode.IMMEDIATE to "立即",
        PlaybackStartMode.COUNTDOWN to "倒计时",
        PlaybackStartMode.SCHEDULED to "定时",
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { if (enabled) onModeSelected(mode) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                modifier = Modifier.weight(1f),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountdownDurationPicker(
    selectedMinutes: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "倒计时时长",
            style = MaterialTheme.typography.labelLarge,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaybackStartPreferences.COUNTDOWN_OPTIONS_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { if (enabled) onSelect(minutes) },
                    enabled = enabled,
                    label = { Text("$minutes 分钟") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TidePrimary.copy(alpha = 0.25f),
                        selectedLabelColor = TidePrimaryVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScheduledTimePicker(
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onPickTime: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "定时时间",
            style = MaterialTheme.typography.labelLarge,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        AssistChip(
            onClick = { if (enabled) onPickTime() },
            enabled = enabled,
            label = {
                Text(
                    PlaybackStartController.formatClockTime(hour, minute),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = TideSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ArmedOrPlayingCaption(
    playbackState: PlaybackStartUiState,
    volumePercent: Int,
    modifier: Modifier = Modifier,
) {
    val text = when {
        playbackState.isPlaying -> {
            val elapsed = playbackState.playingStartedAtEpochMs?.let { startedAt ->
                val elapsedMs = System.currentTimeMillis() - startedAt
                PlaybackStartController.formatElapsedMmSs(elapsedMs)
            }
            if (elapsed != null) {
                "粉红噪声 · 连续播放 · $elapsed · 音量 $volumePercent%"
            } else {
                "粉红噪声 · 连续播放 · 音量 $volumePercent%"
            }
        }
        playbackState.armed is PlaybackArmedState.Countdown -> {
            val armed = playbackState.armed as PlaybackArmedState.Countdown
            "倒计时 ${PlaybackStartController.formatRemainingMmSs(armed.remainingMs)} 后开始"
        }
        playbackState.armed is PlaybackArmedState.Scheduled -> {
            val armed = playbackState.armed as PlaybackArmedState.Scheduled
            val calendar = Calendar.getInstance().apply { timeInMillis = armed.triggerAtEpochMs }
            val time = PlaybackStartController.formatClockTime(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
            )
            "将于 $time 开始"
        }
        else -> "粉红噪声 · 连续播放"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TideOnSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun TonightOrbButton(
    playbackState: PlaybackStartUiState,
    enabled: Boolean,
    onPrimaryClick: () -> Unit,
) {
    val isPlaying = playbackState.isPlaying
    val progress = armedProgress(playbackState)

    val glowBrush = if (isPlaying) {
        Brush.radialGradient(
            colors = listOf(TideError.copy(alpha = 0.45f), TideBackground),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(TidePrimary.copy(alpha = 0.5f), TideBackground),
        )
    }
    val borderColor = when {
        isPlaying -> TideError.copy(alpha = 0.8f)
        playbackState.isArmed -> TidePrimaryVariant.copy(alpha = 0.9f)
        else -> TidePrimaryVariant.copy(alpha = 0.6f)
    }

    Box(contentAlignment = Alignment.Center) {
        if (progress != null) {
            Canvas(modifier = Modifier.size(236.dp)) {
                val strokeWidth = 4.dp.toPx()
                drawArc(
                    color = TidePrimary.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = TidePrimaryVariant,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(glowBrush)
                .border(2.dp, borderColor, CircleShape)
                .clickable(enabled = enabled) { onPrimaryClick() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) TideError.copy(alpha = 0.15f) else TideSurface,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = primaryCtaLabel(playbackState),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isPlaying) TideError else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (!isPlaying && !playbackState.isArmed) {
                        Text(
                            text = secondaryCtaHint(playbackState),
                            style = MaterialTheme.typography.bodySmall,
                            color = TideOnSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun homeStatusText(state: PlaybackStartUiState): String = when {
    state.isPlaying -> "播放中"
    state.isArmed -> "今晚已就绪"
    else -> "今晚已就绪"
}

private fun primaryCtaLabel(state: PlaybackStartUiState): String = when {
    state.isPlaying -> "停止"
    state.armed is PlaybackArmedState.Countdown ->
        PlaybackStartController.formatRemainingMmSs(state.armed.remainingMs)
    state.armed is PlaybackArmedState.Scheduled -> "已设定"
    state.mode == PlaybackStartMode.COUNTDOWN -> "开始倒计时"
    state.mode == PlaybackStartMode.SCHEDULED -> "设定定时"
    else -> "开始播放"
}

private fun secondaryCtaHint(state: PlaybackStartUiState): String = when (state.mode) {
    PlaybackStartMode.COUNTDOWN -> "${state.countdownMinutes} 分钟后开始"
    PlaybackStartMode.SCHEDULED ->
        PlaybackStartController.formatClockTime(state.scheduledHour, state.scheduledMinute)
    PlaybackStartMode.IMMEDIATE -> "立即开始"
}

private fun armedProgress(state: PlaybackStartUiState): Float? {
    val armed = state.armed ?: return null
    return when (armed) {
        is PlaybackArmedState.Countdown -> {
            if (armed.totalMs <= 0L) null
            else (armed.remainingMs.toFloat() / armed.totalMs.toFloat()).coerceIn(0f, 1f)
        }
        is PlaybackArmedState.Scheduled -> {
            val now = System.currentTimeMillis()
            val totalWindow = 60 * 60 * 1000L
            val remaining = (armed.triggerAtEpochMs - now).coerceAtLeast(0L)
            (remaining.toFloat() / totalWindow.toFloat()).coerceIn(0f, 1f)
        }
    }
}
