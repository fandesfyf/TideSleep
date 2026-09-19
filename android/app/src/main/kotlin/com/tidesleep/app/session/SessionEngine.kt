package com.tidesleep.app.session

import com.tidesleep.app.audio.PinkNoisePulsePlayer
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.wearable.WearableSleepMonitor
import com.tidesleep.app.wearable.WearableSleepState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SessionEngine(
    private val scope: CoroutineScope,
    private val sleepMonitor: WearableSleepMonitor,
    private var config: SafetyConfig,
) {
    private val _snapshot = MutableStateFlow(SessionSnapshot())
    val snapshot: StateFlow<SessionSnapshot> = _snapshot.asStateFlow()

    private var pulsePlayer: PinkNoisePulsePlayer? = null
    private var stimulationJob: Job? = null
    private var sleepWatchJob: Job? = null
    private var delayJob: Job? = null

    fun updateConfig(newConfig: SafetyConfig) {
        config = newConfig
        pulsePlayer?.release()
        pulsePlayer = null
    }

    fun startTonight() {
        if (_snapshot.value.phase != SessionPhase.Idle &&
            _snapshot.value.phase != SessionPhase.Stopped
        ) {
            return
        }

        transition(
            phase = SessionPhase.Arming,
            message = "正在准备监听…",
            event = "开启今晚就寝",
        )

        scope.launch {
            sleepMonitor.start()
            transition(
                phase = SessionPhase.WaitingSleep,
                message = "等待入睡确认，暂不播放",
                event = "开始等待入睡",
            )
            watchSleepState()
        }
    }

    fun stopTonight(reason: StopReason = StopReason.Manual) {
        stimulationJob?.cancel()
        delayJob?.cancel()
        sleepWatchJob?.cancel()
        pulsePlayer?.release()
        pulsePlayer = null

        scope.launch {
            sleepMonitor.stop()
        }

        val reasonLabel = when (reason) {
            StopReason.Manual -> "手动停止"
            StopReason.WakeDetected -> "检测到醒来"
            StopReason.Timeout -> "达到单晚上限"
            StopReason.Error -> "异常停止"
        }

        transition(
            phase = SessionPhase.Stopped,
            message = "今晚会话已结束：$reasonLabel",
            event = reasonLabel,
            stopReason = reason,
        )
    }

    private fun watchSleepState() {
        sleepWatchJob?.cancel()
        sleepWatchJob = scope.launch {
            sleepMonitor.sleepState
                .distinctUntilChanged()
                .collect { state ->
                    when (state) {
                        WearableSleepState.Asleep -> onSleepOnset()
                        WearableSleepState.Awake -> onWakeDetected()
                        WearableSleepState.Unknown -> Unit
                    }
                }
        }
    }

    private fun onSleepOnset() {
        val current = _snapshot.value.phase
        if (current != SessionPhase.WaitingSleep) return

        appendTimeline("检测到入睡")
        transition(
            phase = SessionPhase.WaitingSleep,
            message = "已入睡，${config.postSleepDelay.inWholeMinutes} 分钟后开始稀疏脉冲",
        )

        delayJob?.cancel()
        delayJob = scope.launch {
            delay(config.postSleepDelay)
            if (_snapshot.value.phase == SessionPhase.WaitingSleep) {
                beginStimulation()
            }
        }
    }

    private fun onWakeDetected() {
        if (_snapshot.value.phase == SessionPhase.Stimulating ||
            _snapshot.value.phase == SessionPhase.WaitingSleep
        ) {
            stopTonight(StopReason.WakeDetected)
        }
    }

    private fun beginStimulation() {
        appendTimeline("开始稀疏粉红噪声脉冲")
        val maxMs = config.maxSessionDuration.inWholeMilliseconds
        _snapshot.value = _snapshot.value.copy(
            phase = SessionPhase.Stimulating,
            remainingStimulationMs = maxMs,
            statusMessage = "刺激进行中（开放环）",
            pulseCount = 0,
        )

        val player = PinkNoisePulsePlayer(config)
        pulsePlayer = player

        stimulationJob?.cancel()
        stimulationJob = scope.launch {
            val startedAt = System.currentTimeMillis()
            var pulses = 0

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = maxMs - elapsed
                if (remaining <= 0) {
                    stopTonight(StopReason.Timeout)
                    break
                }

                player.playPulse()
                pulses++
                _snapshot.value = _snapshot.value.copy(
                    pulseCount = pulses,
                    remainingStimulationMs = remaining,
                )

                val intervalSeconds = Random.nextDouble(
                    config.pulseIntervalRange.start,
                    config.pulseIntervalRange.endInclusive,
                )
                delay(intervalSeconds.seconds.inWholeMilliseconds)
            }
        }
    }

    private fun transition(
        phase: SessionPhase,
        message: String,
        event: String? = null,
        stopReason: StopReason? = null,
    ) {
        val timeline = if (event != null) {
            _snapshot.value.timeline + SessionTimelineEvent(event, Instant.now())
        } else {
            _snapshot.value.timeline
        }
        _snapshot.value = _snapshot.value.copy(
            phase = phase,
            statusMessage = message,
            timeline = timeline,
            stopReason = stopReason ?: _snapshot.value.stopReason,
        )
    }

    private fun appendTimeline(label: String) {
        _snapshot.value = _snapshot.value.copy(
            timeline = _snapshot.value.timeline + SessionTimelineEvent(label, Instant.now())
        )
    }

    private val Double.seconds
        get() = (this * 1000).milliseconds
}
