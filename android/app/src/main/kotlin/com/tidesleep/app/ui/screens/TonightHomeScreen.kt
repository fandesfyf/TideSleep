package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.ui.components.DisclaimerBanner
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TidePrimary
import com.tidesleep.app.viewmodel.TideSleepViewModel

@Composable
fun TonightHomeScreen(
    viewModel: TideSleepViewModel,
    onNavigateToScience: () -> Unit,
    onNavigateToSafety: () -> Unit,
) {
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val isActive = session.phase != SessionPhase.Idle && session.phase != SessionPhase.Stopped

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = "汐眠 TideSleep",
            subtitle = "等你睡着，再推一把慢波",
        )

        DisclaimerBanner(
            text = "开放环 MVP：确认入睡后再播放稀疏粉红噪声脉冲。无 EEG，不等于论文级相位闭环。"
        )

        TideCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = phaseLabel(session.phase),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.statusMessage.ifBlank { "点击下方按钮开始今晚就寝" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideOnSurfaceMuted,
                )
                if (session.pulseCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已播放 ${session.pulseCount} 次脉冲",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isActive) {
            OutlinedButton(
                onClick = { viewModel.stopTonight() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("停止今晚")
            }
        } else {
            Button(
                onClick = { viewModel.startTonight() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TidePrimary),
            ) {
                Text("今晚就寝", style = MaterialTheme.typography.titleMedium)
            }
        }

        OutlinedButton(
            onClick = onNavigateToSafety,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("安全设置")
        }

        OutlinedButton(
            onClick = onNavigateToScience,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("科学说明与米家向导")
        }
    }
}

private fun phaseLabel(phase: SessionPhase): String = when (phase) {
    SessionPhase.Idle -> "未开启"
    SessionPhase.Arming -> "准备中"
    SessionPhase.WaitingSleep -> "等待入睡"
    SessionPhase.Stimulating -> "刺激进行中"
    SessionPhase.Stopped -> "已结束"
}
