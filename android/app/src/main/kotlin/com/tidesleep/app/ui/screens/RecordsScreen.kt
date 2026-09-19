package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.viewmodel.TideSleepViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordsScreen(viewModel: TideSleepViewModel) {
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val history by viewModel.nightHistory.collectAsStateWithLifecycle()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val showCurrent = session.timeline.isNotEmpty() &&
        session.phase != SessionPhase.Idle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        SectionHeader(
            title = "记录",
            subtitle = "历史会话持久化保存，重启后仍可查看",
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showCurrent) {
                item {
                    NightRecordCard(
                        title = "进行中 / 本夜",
                        pulseCount = session.pulseCount,
                        stopReason = session.stopReason?.name,
                        events = session.timeline,
                        timeFormatter = timeFormatter,
                    )
                }
            }

            if (history.isEmpty() && !showCurrent) {
                item {
                    TideCard {
                        Text(
                            text = "暂无历史记录。使用演示预设，可在 1 分钟内完成一次完整流程。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TideOnSurfaceMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(history, key = { "${it.date}-${it.endedAt.toEpochMilli()}" }) { summary ->
                NightRecordCard(
                    title = dateFormatter.format(summary.date),
                    pulseCount = summary.pulseCount,
                    stopReason = summary.stopReason?.name,
                    events = summary.timeline,
                    timeFormatter = timeFormatter,
                )
            }
        }
    }
}

@Composable
private fun NightRecordCard(
    title: String,
    pulseCount: Int,
    stopReason: String?,
    events: List<com.tidesleep.app.session.SessionTimelineEvent>,
    timeFormatter: DateTimeFormatter,
) {
    TideCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (pulseCount > 0) {
                Text(
                    text = "脉冲 ${pulseCount} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = TideOnSurfaceMuted,
                )
            }
            if (stopReason != null) {
                Text(
                    text = "停止：$stopReason",
                    style = MaterialTheme.typography.bodySmall,
                    color = TideOnSurfaceMuted,
                )
            }
            events.forEach { event ->
                Text(
                    text = "${timeFormatter.format(event.timestamp)} · ${event.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
