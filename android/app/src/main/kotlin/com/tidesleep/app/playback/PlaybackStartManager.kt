package com.tidesleep.app.playback

import com.tidesleep.app.data.PlaybackStartMode
import com.tidesleep.app.data.PlaybackStartPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackStartManager(
    private val scope: CoroutineScope,
    private val scheduler: PinkNoiseScheduler,
    private val onStartPlayback: () -> Unit,
    private val onStopPlayback: () -> Unit,
    private val onPreferencesChanged: (PlaybackStartPreferences) -> Unit,
    private val isActuallyPlaying: () -> Boolean,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val controller = PlaybackStartController(nowMs = nowMs)
    private val _state = MutableStateFlow(controller.snapshot())
    val state: StateFlow<PlaybackStartUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var elapsedJob: Job? = null

    fun restore(prefs: PlaybackStartPreferences) {
        val effectivePrefs = prefs.armedTriggerAtEpochMs?.let { triggerAt ->
            if (triggerAt <= nowMs()) prefs.copy(armedTriggerAtEpochMs = null) else prefs
        } ?: prefs
        if (effectivePrefs != prefs) {
            onPreferencesChanged(effectivePrefs)
        }
        controller.restoreFromPreferences(effectivePrefs, isActuallyPlaying())
        val snapshot = controller.snapshot()
        _state.value = snapshot
        snapshot.armed?.let { armed ->
            val triggerAt = when (armed) {
                is PlaybackArmedState.Countdown -> armed.triggerAtEpochMs
                is PlaybackArmedState.Scheduled -> armed.triggerAtEpochMs
            }
            scheduler.schedulePlaybackAt(triggerAt)
            if (armed is PlaybackArmedState.Countdown) {
                startCountdownTicker(triggerAt)
            }
        }
        if (snapshot.isPlaying) {
            startElapsedTicker()
        }
    }

    fun setMode(mode: PlaybackStartMode) {
        update(controller.setMode(mode))
        persist()
    }

    fun setCountdownMinutes(minutes: Int) {
        update(controller.setCountdownMinutes(minutes))
        persist()
    }

    fun setScheduledTime(hour: Int, minute: Int) {
        update(controller.setScheduledTime(hour, minute))
        persist()
    }

    fun onPrimaryAction() {
        val snapshot = controller.snapshot()
        when {
            snapshot.isPlaying -> stopPlayback()
            snapshot.isArmed -> Unit
            snapshot.mode == PlaybackStartMode.IMMEDIATE -> startImmediate()
            snapshot.mode == PlaybackStartMode.COUNTDOWN -> armCountdown()
            snapshot.mode == PlaybackStartMode.SCHEDULED -> armSchedule()
        }
    }

    fun cancelArmed() {
        countdownJob?.cancel()
        countdownJob = null
        scheduler.cancel()
        update(controller.cancelArmed())
        persist()
    }

    fun onScheduledTrigger() {
        countdownJob?.cancel()
        countdownJob = null
        scheduler.cancel()
        update(controller.onPlaybackTriggered())
        onStartPlayback()
        startElapsedTicker()
        persist()
    }

    private fun startImmediate() {
        update(controller.startImmediate())
        onStartPlayback()
        startElapsedTicker()
        persist()
    }

    private fun armCountdown() {
        when (val result = controller.armCountdown()) {
            is PlaybackStartController.ArmResult.Armed -> {
                update(controller.snapshot())
                scheduler.schedulePlaybackAt(result.triggerAtEpochMs)
                startCountdownTicker(result.triggerAtEpochMs)
                persist()
            }
            PlaybackStartController.ArmResult.Rejected -> Unit
        }
    }

    private fun armSchedule() {
        when (val result = controller.armSchedule()) {
            is PlaybackStartController.ArmResult.Armed -> {
                update(controller.snapshot())
                scheduler.schedulePlaybackAt(result.triggerAtEpochMs)
                persist()
            }
            PlaybackStartController.ArmResult.Rejected -> Unit
        }
    }

    private fun stopPlayback() {
        countdownJob?.cancel()
        countdownJob = null
        elapsedJob?.cancel()
        elapsedJob = null
        onStopPlayback()
        update(controller.stopPlaying())
        persist()
    }

    private fun startCountdownTicker(triggerAtEpochMs: Long) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                val remaining = triggerAtEpochMs - nowMs()
                if (remaining <= 0L) {
                    onScheduledTrigger()
                    break
                }
                update(controller.onCountdownTick(remaining))
                delay(1_000L)
            }
        }
    }

    private fun startElapsedTicker() {
        elapsedJob?.cancel()
        val startedAt = controller.snapshot().playingStartedAtEpochMs ?: return
        elapsedJob = scope.launch {
            while (isActive && controller.snapshot().isPlaying) {
                delay(1_000L)
                _state.value = controller.snapshot()
            }
        }
    }

    private fun update(newState: PlaybackStartUiState) {
        _state.value = newState
    }

    private fun persist() {
        onPreferencesChanged(controller.toPreferences())
    }
}
