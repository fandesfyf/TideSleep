package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.ui.components.SessionTimelineBar
import com.tidesleep.app.ui.theme.TideError
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TidePrimary
import com.tidesleep.app.viewmodel.TideSleepViewModel
import java.util.concurrent.TimeUnit

@Composable
fun SessionActiveScreen(
    viewModel: TideSleepViewModel,
    onBack: () -> Unit,
) {
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val safetyConfig by viewModel.safetyConfig.collectAsStateWithLifecycle()

    LaunchedEffect(session.phase) {
        if (session.phase == SessionPhase.Stopped || session.phase == SessionPhase.Idle) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("夜间主动时段", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "正在通过温和脉冲，帮助你更快进入深睡",
            style = MaterialTheme.typography.bodyMedium,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        when (session.phase) {
            SessionPhase.Arming -> {
                CircularProgressIndicator(color = TidePrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("准备监听…", style = MaterialTheme.typography.titleLarge)
            }
            SessionPhase.WaitingSleep -> {
                Text("等待入睡", style = MaterialTheme.typography.displayLarge)
                Text(
                    text = "穿戴确认入睡后，将延迟 ${safetyConfig.formatPostSleepDelay()} 再播放",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TideOnSurfaceMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            SessionPhase.WaitingDelay -> {
                AssistChip(onClick = {}, label = { Text("延迟中") })
                Text(
                    text = "已入睡，${safetyConfig.formatPostSleepDelay()}后开始稀疏脉冲",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            SessionPhase.Stimulating -> {
                AssistChip(onClick = {}, label = { Text("刺激中") })
                Text(
                    text = "已播放 ${session.pulseCount} 次 · 50 ms 粉红噪声",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "剩余 ${formatRemaining(session.remainingStimulationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideOnSurfaceMuted,
                )
            }
            else -> Unit
        }

        SessionTimelineBar(session = session, modifier = Modifier.padding(vertical = 24.dp))

        Text(
            text = session.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = TideOnSurfaceMuted,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "你的安全与舒适是优先",
            style = MaterialTheme.typography.bodySmall,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Button(
            onClick = { viewModel.stopSleepSession() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TideError),
        ) {
            Text("立即停止", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "${minutes}分${seconds}秒"
}
