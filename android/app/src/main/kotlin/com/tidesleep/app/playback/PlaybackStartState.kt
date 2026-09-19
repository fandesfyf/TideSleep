package com.tidesleep.app.playback

import com.tidesleep.app.data.PlaybackStartMode

sealed interface PlaybackArmedState {
    data class Countdown(
        val remainingMs: Long,
        val totalMs: Long,
        val triggerAtEpochMs: Long,
    ) : PlaybackArmedState

    data class Scheduled(
        val triggerAtEpochMs: Long,
    ) : PlaybackArmedState
}

data class PlaybackStartUiState(
    val mode: PlaybackStartMode = PlaybackStartMode.IMMEDIATE,
    val countdownMinutes: Int = PlaybackStartPreferencesDefaults.countdownMinutes,
    val scheduledHour: Int = PlaybackStartPreferencesDefaults.scheduledHour,
    val scheduledMinute: Int = PlaybackStartPreferencesDefaults.scheduledMinute,
    val armed: PlaybackArmedState? = null,
    val isPlaying: Boolean = false,
    val playingStartedAtEpochMs: Long? = null,
) {
    val isArmed: Boolean = armed != null

    companion object {
        fun idle(
            mode: PlaybackStartMode,
            countdownMinutes: Int,
            scheduledHour: Int,
            scheduledMinute: Int,
        ) = PlaybackStartUiState(
            mode = mode,
            countdownMinutes = countdownMinutes,
            scheduledHour = scheduledHour,
            scheduledMinute = scheduledMinute,
        )
    }
}

object PlaybackStartPreferencesDefaults {
    const val countdownMinutes = 15
    const val scheduledHour = 23
    const val scheduledMinute = 0
}
