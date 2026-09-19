package com.tidesleep.app.data

import com.tidesleep.app.session.SessionTimelineEvent
import com.tidesleep.app.session.StopReason
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NightSummary(
    val date: LocalDate,
    val timeline: List<SessionTimelineEvent>,
    val pulseCount: Int,
    val stopReason: StopReason?,
    val endedAt: Instant,
)

@Serializable
data class NightSummaryDto(
    val date: String,
    val events: List<TimelineEventDto>,
    val pulseCount: Int,
    val stopReason: String?,
    val endedAtMillis: Long,
)

@Serializable
data class TimelineEventDto(
    val label: String,
    val epochMillis: Long,
)

fun NightSummary.toDto(): NightSummaryDto = NightSummaryDto(
    date = date.toString(),
    events = timeline.map { TimelineEventDto(it.label, it.timestamp.toEpochMilli()) },
    pulseCount = pulseCount,
    stopReason = stopReason?.name,
    endedAtMillis = endedAt.toEpochMilli(),
)

fun NightSummaryDto.toNightSummary(zone: ZoneId = ZoneId.systemDefault()): NightSummary = NightSummary(
    date = LocalDate.parse(date),
    timeline = events.map { SessionTimelineEvent(it.label, Instant.ofEpochMilli(it.epochMillis)) },
    pulseCount = pulseCount,
    stopReason = stopReason?.let { runCatching { StopReason.valueOf(it) }.getOrNull() },
    endedAt = Instant.ofEpochMilli(endedAtMillis),
)
