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
import com.tidesleep.app.ui.components.SectionHeader
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.viewmodel.TideSleepViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordsScreen(viewModel: TideSleepViewModel) {
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        SectionHeader(
            title = "记录",
            subtitle = "本夜时间线与历史摘要（MVP）",
        )

        if (session.timeline.isEmpty()) {
            TideCard {
                Text(
                    text = "暂无记录。开启「今晚就寝」后，入睡、开播与停止事件将显示于此。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideOnSurfaceMuted,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(session.timeline) { event ->
                    TideCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(event.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = formatter.format(event.timestamp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TideOnSurfaceMuted,
                            )
                        }
                    }
                }
            }

            if (session.pulseCount > 0) {
                TideCard {
                    Text(
                        text = "本夜共播放 ${session.pulseCount} 次脉冲",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
