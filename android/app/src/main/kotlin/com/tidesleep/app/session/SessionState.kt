package com.tidesleep.app.session

import java.time.Instant

enum class SessionPhase {
    Idle,
    Arming,
    WaitingSleep,
    Stimulating,
    Stopped,
}

enum class StopReason {
    Manual,
    WakeDetected,
    Timeout,
    Error,
}

data class SessionTimelineEvent(
    val label: String,
    val timestamp: Instant,
)

data class SessionSnapshot(
    val phase: SessionPhase = SessionPhase.Idle,
    val stopReason: StopReason? = null,
    val pulseCount: Int = 0,
    val remainingStimulationMs: Long = 0L,
    val timeline: List<SessionTimelineEvent> = emptyList(),
    val statusMessage: String = "",
)
