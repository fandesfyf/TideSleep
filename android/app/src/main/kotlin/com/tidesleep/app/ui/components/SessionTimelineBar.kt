package com.tidesleep.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tidesleep.app.session.SessionSnapshot
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TidePrimary
import com.tidesleep.app.ui.theme.TidePrimaryVariant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionTimelineBar(session: SessionSnapshot, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val sleepAt = session.sleepOnsetAt
    val stimAt = session.stimulationStartedAt

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(TideOnSurfaceMuted.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (stimAt != null) 0.7f else if (sleepAt != null) 0.4f else 0.1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TidePrimary),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimelinePoint(
                label = if (sleepAt != null) formatter.format(sleepAt) else "—",
                caption = "入睡",
                active = sleepAt != null,
            )
            TimelinePoint(
                label = if (stimAt != null) formatter.format(stimAt) else "—",
                caption = "开始脉冲",
                active = stimAt != null,
            )
            TimelinePoint(
                label = "现在",
                caption = if (session.pulseCount > 0) "${session.pulseCount} 次" else "等待",
                active = session.pulseCount > 0,
                highlight = true,
            )
        }
    }
}

@Composable
private fun TimelinePoint(
    label: String,
    caption: String,
    active: Boolean,
    highlight: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (highlight) 12.dp else 8.dp)
                .offset(y = (-8).dp)
                .clip(CircleShape)
                .background(if (highlight) TidePrimaryVariant else if (active) TidePrimary else TideOnSurfaceMuted),
        )
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = TideOnSurfaceMuted)
    }
}
