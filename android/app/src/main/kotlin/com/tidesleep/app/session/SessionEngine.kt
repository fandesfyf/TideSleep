package com.tidesleep.app.session

import com.tidesleep.app.audio.PulsePlayer
import com.tidesleep.app.data.NightSummary
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.wearable.WearableSleepMonitor
import com.tidesleep.app.wearable.WearableSleepState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SessionEngine(
    private val scope: CoroutineScope,
    private val sleepMonitor: WearableSleepMonitor,
    private var config: SafetyConfig,
    private val createPulsePlayer: (SafetyConfig) -> PulsePlayer,
    private val onNightArchived: (NightSummary) -> Unit = {},
) {
    private val _snapshot = MutableStateFlow(SessionSnapshot())
    val snapshot: StateFlow<SessionSnapshot> = _snapshot.asStateFlow()

    private var pulsePlayer: PulsePlayer? = null
    private var stimulationJob: Job? = null
    private var sleepWatchJob: Job? = null
    private var delayJob: Job? = null
    private var armingJob: Job? = null
    private var archivedCurrentSession = false

    /** 测试用：取消所有后台任务，不归档会话 */
    fun cancelAllJobs() {
        stimulationJob?.cancel()
        delayJob?.cancel()
        sleepWatchJob?.cancel()
        armingJob?.cancel()
        pulsePlayer?.release()
        pulsePlayer = null
    }

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

        // 上一晚已在 stopTonight 时归档；此处仅重置会话
        archivedCurrentSession = false
        _snapshot.value = SessionSnapshot(
            phase = SessionPhase.Arming,
            statusMessage = "正在准备监听…",
            timeline = listOf(SessionTimelineEvent("开启今晚就寝", Instant.now())),
        )

        armingJob?.cancel()
        armingJob = scope.launch {
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
        val phase = _snapshot.value.phase
        if (phase == SessionPhase.Stopped || phase == SessionPhase.Idle) {
            return
        }

        stimulationJob?.cancel()
        stimulationJob = null
        delayJob?.cancel()
        delayJob = null
        sleepWatchJob?.cancel()
        sleepWatchJob = null
        armingJob?.cancel()
        armingJob = null
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

        archiveSessionIfNeeded()
    }

    private fun watchSleepState() {
        sleepWatchJob?.cancel()
        sleepWatchJob = scope.launch {
            sleepMonitor.sleepState.collect { state ->
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

        val onsetAt = Instant.now()
        appendTimeline("检测到入睡")
        _snapshot.value = _snapshot.value.copy(
            sleepOnsetAt = onsetAt,
            statusMessage = "已入睡，${config.formatPostSleepDelay()}后开始稀疏脉冲",
        )

        transition(
            phase = SessionPhase.WaitingDelay,
            message = "入睡延迟中，${config.formatPostSleepDelay()}后开播",
        )

        delayJob?.cancel()
        delayJob = scope.launch {
            delay(config.postSleepDelay)
            if (_snapshot.value.phase == SessionPhase.WaitingDelay) {
                beginStimulation()
            }
        }
    }

    private fun onWakeDetected() {
        val phase = _snapshot.value.phase
        when (phase) {
            SessionPhase.Stimulating, SessionPhase.WaitingDelay -> {
                if (phase == SessionPhase.WaitingDelay) {
                    delayJob?.cancel()
                    delayJob = null
                }
                stopTonight(StopReason.WakeDetected)
            }
            // WaitingSleep 时忽略初始 Awake 状态（StateFlow 首帧），避免误停
            else -> Unit
        }
    }

    private fun beginStimulation() {
        appendTimeline("开始稀疏粉红噪声脉冲")
        val maxMs = config.maxSessionDuration.inWholeMilliseconds
        val startedAt = Instant.now()
        _snapshot.value = _snapshot.value.copy(
            phase = SessionPhase.Stimulating,
            remainingStimulationMs = maxMs,
            statusMessage = "刺激进行中（开放环）",
            pulseCount = 0,
            stimulationStartedAt = startedAt,
        )

        val player = createPulsePlayer(config)
        pulsePlayer = player

        stimulationJob?.cancel()
        stimulationJob = scope.launch {
            var elapsedMs = 0L
            var pulses = 0

            while (isActive) {
                val remaining = maxMs - elapsedMs
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

                val range = config.pulseIntervalRange
                val intervalSeconds = if (range.start >= range.endInclusive) {
                    range.start
                } else {
                    Random.nextDouble(range.start, range.endInclusive)
                }
                val intervalMs = (intervalSeconds * 1000).toLong().coerceAtMost(remaining)
                delay(intervalMs)
                elapsedMs += intervalMs
            }
        }
    }

    private fun archiveSessionIfNeeded() {
        if (archivedCurrentSession) return
        val snap = _snapshot.value
        if (snap.timeline.isEmpty() || snap.phase == SessionPhase.Idle) return

        val endedAt = snap.timeline.lastOrNull()?.timestamp ?: Instant.now()
        onNightArchived(
            NightSummary(
                date = LocalDate.ofInstant(endedAt, ZoneId.systemDefault()),
                timeline = snap.timeline,
                pulseCount = snap.pulseCount,
                stopReason = snap.stopReason,
                endedAt = endedAt,
            )
        )
        archivedCurrentSession = true
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
            timeline = _snapshot.value.timeline + SessionTimelineEvent(label, Instant.now()),
        )
    }
}
