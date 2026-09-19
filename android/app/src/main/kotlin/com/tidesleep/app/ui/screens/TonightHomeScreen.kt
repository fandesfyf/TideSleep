package com.tidesleep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.data.SafetyPreset
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.ui.components.DeviceStatusChips
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideBackground
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TidePrimary
import com.tidesleep.app.ui.theme.TidePrimaryVariant
import com.tidesleep.app.ui.theme.TideSurface
import com.tidesleep.app.viewmodel.TideSleepViewModel

@Composable
fun TonightHomeScreen(
    viewModel: TideSleepViewModel,
    onNavigateToScience: () -> Unit,
    onNavigateToSafety: () -> Unit,
) {
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val safetyConfig by viewModel.safetyConfig.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val monitorSource by viewModel.monitorSource.collectAsStateWithLifecycle()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()

    val isActive = session.phase != SessionPhase.Idle && session.phase != SessionPhase.Stopped

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RowWithHeader()

        if (safetyConfig.preset == SafetyPreset.DEMO) {
            AssistChip(
                onClick = onNavigateToSafety,
                label = { Text("演示预设 · 延迟 ${safetyConfig.formatPostSleepDelay()}") },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isActive) phaseLabel(session.phase) else "今晚已待命",
            style = MaterialTheme.typography.titleMedium,
            color = TideOnSurfaceMuted,
        )

        Spacer(modifier = Modifier.weight(0.3f))

        TonightOrbButton(
            isActive = isActive,
            enabled = disclaimerAccepted,
            onClick = {
                if (isActive) viewModel.stopTonight() else viewModel.startTonight()
            },
        )

        Text(
            text = "确认入睡后再播放粉红噪声脉冲",
            style = MaterialTheme.typography.bodyMedium,
            color = TideOnSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )

        if (!disclaimerAccepted) {
            Text(
                text = "请先完成首次免责声明确认",
                style = MaterialTheme.typography.bodySmall,
                color = TidePrimaryVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.weight(0.2f))

        DeviceStatusChips(
            devices = devices,
            monitorSource = monitorSource,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (session.statusMessage.isNotBlank() && isActive) {
            TideCard {
                Text(
                    text = session.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RowWithHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("汐眠", style = MaterialTheme.typography.displayLarge)
        Text("TideSleep", style = MaterialTheme.typography.bodyMedium, color = TideOnSurfaceMuted)
    }
}

@Composable
private fun TonightOrbButton(
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val glowBrush = Brush.radialGradient(
        colors = listOf(TidePrimary.copy(alpha = 0.5f), TideBackground),
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(glowBrush)
            .border(2.dp, TidePrimaryVariant.copy(alpha = 0.6f), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(TideSurface),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isActive) "停止今晚" else "开启今晚",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "汐眠睡眠模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = TideOnSurfaceMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun phaseLabel(phase: SessionPhase): String = when (phase) {
    SessionPhase.Arming -> "准备中"
    SessionPhase.WaitingSleep -> "等待入睡"
    SessionPhase.WaitingDelay -> "入睡延迟中"
    SessionPhase.Stimulating -> "刺激进行中"
    SessionPhase.Stopped -> "已结束"
    SessionPhase.Idle -> "未开启"
}
