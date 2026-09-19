package com.tidesleep.app.playback

import com.tidesleep.app.data.PlaybackStartMode
import com.tidesleep.app.data.PlaybackStartPreferences
import java.util.Calendar

/**
 * Pure state machine for immediate / countdown / scheduled pink-noise start.
 * Android scheduling is delegated via callbacks.
 */
class PlaybackStartController(
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private var state: PlaybackStartUiState = PlaybackStartUiState.idle(
        mode = PlaybackStartMode.IMMEDIATE,
        countdownMinutes = PlaybackStartPreferences.DEFAULT_COUNTDOWN_MINUTES,
        scheduledHour = PlaybackStartPreferences.DEFAULT_SCHEDULED_HOUR,
        scheduledMinute = PlaybackStartPreferences.DEFAULT_SCHEDULED_MINUTE,
    )

    fun snapshot(): PlaybackStartUiState = state

    fun restoreFromPreferences(prefs: PlaybackStartPreferences, isPlaying: Boolean) {
        val armed = prefs.armedTriggerAtEpochMs?.let { triggerAt ->
            val now = nowMs()
            when {
                triggerAt <= now -> null
                prefs.mode == PlaybackStartMode.COUNTDOWN -> {
                    val totalMs = prefs.countdownMinutes * 60_000L
                    PlaybackArmedState.Countdown(
                        remainingMs = triggerAt - now,
                        totalMs = totalMs,
                        triggerAtEpochMs = triggerAt,
                    )
                }
                prefs.mode == PlaybackStartMode.SCHEDULED -> PlaybackArmedState.Scheduled(triggerAt)
                else -> null
            }
        }
        state = PlaybackStartUiState(
            mode = prefs.mode,
            countdownMinutes = prefs.countdownMinutes,
            scheduledHour = prefs.scheduledHour,
            scheduledMinute = prefs.scheduledMinute,
            armed = armed,
            isPlaying = isPlaying,
            playingStartedAtEpochMs = if (isPlaying) nowMs() else null,
        )
    }

    fun setMode(mode: PlaybackStartMode): PlaybackStartUiState {
        if (state.isPlaying || state.isArmed) return state
        state = state.copy(mode = mode)
        return state
    }

    fun setCountdownMinutes(minutes: Int): PlaybackStartUiState {
        if (state.isPlaying || state.isArmed) return state
        state = state.copy(countdownMinutes = minutes.coerceIn(1, 180))
        return state
    }

    fun setScheduledTime(hour: Int, minute: Int): PlaybackStartUiState {
        if (state.isPlaying || state.isArmed) return state
        state = state.copy(
            scheduledHour = hour.coerceIn(0, 23),
            scheduledMinute = minute.coerceIn(0, 59),
        )
        return state
    }

    fun armCountdown(): ArmResult {
        if (state.isPlaying || state.isArmed) return ArmResult.Rejected
        val totalMs = state.countdownMinutes * 60_000L
        val triggerAt = nowMs() + totalMs
        state = state.copy(
            mode = PlaybackStartMode.COUNTDOWN,
            armed = PlaybackArmedState.Countdown(
                remainingMs = totalMs,
                totalMs = totalMs,
                triggerAtEpochMs = triggerAt,
            ),
        )
        return ArmResult.Armed(triggerAtEpochMs = triggerAt)
    }

    fun armSchedule(): ArmResult {
        if (state.isPlaying || state.isArmed) return ArmResult.Rejected
        val triggerAt = computeNextTriggerEpochMs(
            hour = state.scheduledHour,
            minute = state.scheduledMinute,
            nowMs = nowMs(),
        )
        state = state.copy(
            mode = PlaybackStartMode.SCHEDULED,
            armed = PlaybackArmedState.Scheduled(triggerAtEpochMs = triggerAt),
        )
        return ArmResult.Armed(triggerAtEpochMs = triggerAt)
    }

    fun onCountdownTick(remainingMs: Long): PlaybackStartUiState {
        val armed = state.armed as? PlaybackArmedState.Countdown ?: return state
        if (remainingMs <= 0L) return onPlaybackTriggered()
        state = state.copy(
            armed = armed.copy(remainingMs = remainingMs.coerceAtLeast(0L)),
        )
        return state
    }

    fun onPlaybackTriggered(): PlaybackStartUiState {
        val startedAt = nowMs()
        state = state.copy(
            armed = null,
            isPlaying = true,
            playingStartedAtEpochMs = startedAt,
        )
        return state
    }

    fun startImmediate(): PlaybackStartUiState {
        if (state.isPlaying) return state
        state = state.copy(armed = null)
        return onPlaybackTriggered()
    }

    fun cancelArmed(): PlaybackStartUiState {
        if (!state.isArmed) return state
        state = state.copy(armed = null)
        return state
    }

    fun stopPlaying(): PlaybackStartUiState {
        if (!state.isPlaying) return state
        state = state.copy(isPlaying = false, playingStartedAtEpochMs = null)
        return state
    }

    fun toPreferences(): PlaybackStartPreferences {
        val triggerAt = when (val armed = state.armed) {
            is PlaybackArmedState.Countdown -> armed.triggerAtEpochMs
            is PlaybackArmedState.Scheduled -> armed.triggerAtEpochMs
            null -> null
        }
        return PlaybackStartPreferences(
            mode = state.mode,
            countdownMinutes = state.countdownMinutes,
            scheduledHour = state.scheduledHour,
            scheduledMinute = state.scheduledMinute,
            armedTriggerAtEpochMs = triggerAt,
        )
    }

    sealed class ArmResult {
        data object Rejected : ArmResult()
        data class Armed(val triggerAtEpochMs: Long) : ArmResult()
    }

    companion object {
        fun computeNextTriggerEpochMs(
            hour: Int,
            minute: Int,
            nowMs: Long = System.currentTimeMillis(),
        ): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            if (calendar.timeInMillis <= nowMs) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        fun formatRemainingMmSs(remainingMs: Long): String {
            val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        fun formatElapsedMmSs(elapsedMs: Long): String {
            val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        fun formatClockTime(hour: Int, minute: Int): String =
            "%02d:%02d".format(hour, minute)
    }
}
