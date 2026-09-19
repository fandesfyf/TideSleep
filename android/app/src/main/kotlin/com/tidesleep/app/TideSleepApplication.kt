package com.tidesleep.app

import android.app.Application
import com.tidesleep.app.audio.ContinuousPinkNoisePlayer
import com.tidesleep.app.audio.PinkNoisePulsePlayer
import com.tidesleep.app.data.PlaybackStartPreferences
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.data.TideSleepRepository
import com.tidesleep.app.playback.PinkNoiseScheduler
import com.tidesleep.app.playback.PlaybackStartManager
import com.tidesleep.app.playback.PlaybackStartUiState
import com.tidesleep.app.session.SessionEngine
import com.tidesleep.app.wearable.FakeSleepMonitor
import com.tidesleep.app.wearable.MiJiaSleepBridgeMonitor
import com.tidesleep.app.wearable.SleepStateSink
import com.tidesleep.app.wearable.WearableDeviceInfo
import com.tidesleep.app.wearable.WearableSleepMonitor
import com.tidesleep.app.wearable.WearableSleepState
import com.tidesleep.app.wearable.XiaomiWearSleepMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TideSleepApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var repository: TideSleepRepository
        private set

    private val _safetyConfig = MutableStateFlow(SafetyConfig.demo())
    val safetyConfigFlow: StateFlow<SafetyConfig> = _safetyConfig.asStateFlow()

    private val _monitorSource = MutableStateFlow(SleepMonitorSource.FAKE)
    val monitorSourceFlow: StateFlow<SleepMonitorSource> = _monitorSource.asStateFlow()

    private val _disclaimerAccepted = MutableStateFlow(false)
    val disclaimerAcceptedFlow: StateFlow<Boolean> = _disclaimerAccepted.asStateFlow()

    lateinit var sleepMonitor: WearableSleepMonitor
        private set

    lateinit var sessionEngine: SessionEngine
        private set

    lateinit var continuousPlayer: ContinuousPinkNoisePlayer
        private set

    lateinit var playbackStartManager: PlaybackStartManager
        private set

    private val _continuousPlaying = MutableStateFlow(false)
    val continuousPlayingFlow: StateFlow<Boolean> = _continuousPlaying.asStateFlow()

    private val _devices = MutableStateFlow<List<WearableDeviceInfo>>(emptyList())
    val devicesFlow: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    private val _sleepState = MutableStateFlow(WearableSleepState.Unknown)
    val sleepStateFlow: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private var sleepStateSink: SleepStateSink? = null
    private var monitorBindingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = TideSleepRepository(this)

        val prefs = runBlocking { repository.loadOnce() }
        _safetyConfig.value = prefs.safetyConfig
        _monitorSource.value = prefs.monitorSource
        _disclaimerAccepted.value = prefs.disclaimerAccepted

        continuousPlayer = ContinuousPinkNoisePlayer(this, applicationScope)
        playbackStartManager = PlaybackStartManager(
            scope = applicationScope,
            scheduler = PinkNoiseScheduler(this),
            onStartPlayback = { startContinuousPinkNoise() },
            onStopPlayback = { stopContinuousPinkNoise() },
            onPreferencesChanged = { savePlaybackStartPreferences(it) },
            isActuallyPlaying = { continuousPlayer.playing },
        )
        playbackStartManager.restore(prefs.playbackStart)

        sleepMonitor = createMonitor(prefs.monitorSource)
        bindSleepMonitor(sleepMonitor)
        sessionEngine = createSessionEngine()
    }

    fun onScheduledPinkNoiseStart() {
        playbackStartManager.onScheduledTrigger()
        com.tidesleep.app.session.TideSleepSessionService.start(this)
    }

    val playbackStartState: StateFlow<PlaybackStartUiState>
        get() = playbackStartManager.state

    fun startContinuousPinkNoise() {
        continuousPlayer.start(_safetyConfig.value)
        _continuousPlaying.value = true
    }

    fun stopContinuousPinkNoise() {
        continuousPlayer.stop()
        _continuousPlaying.value = false
    }

    private fun savePlaybackStartPreferences(prefs: PlaybackStartPreferences) {
        applicationScope.launch {
            repository.savePlaybackStartPreferences(prefs)
        }
    }

    fun updateSafetyConfig(config: SafetyConfig) {
        _safetyConfig.value = config
        sessionEngine.updateConfig(config)
        if (_continuousPlaying.value) {
            continuousPlayer.updateVolume(config.volumePercent)
        }
        applicationScope.launch {
            repository.saveSafetyConfig(config)
        }
    }

    fun switchMonitorSource(source: SleepMonitorSource) {
        if (_monitorSource.value == source) return
        _monitorSource.value = source
        sessionEngine.stopTonight()
        sleepMonitor = createMonitor(source)
        bindSleepMonitor(sleepMonitor)
        sessionEngine = createSessionEngine()
        applicationScope.launch {
            repository.saveMonitorSource(source)
        }
    }

    /** 米家深链 / 广播投递睡眠状态（Path A）。 */
    fun deliverExternalSleepState(state: WearableSleepState) {
        sleepStateSink?.onExternalSleepState(state)
    }

    private fun bindSleepMonitor(monitor: WearableSleepMonitor) {
        monitorBindingJob?.cancel()
        sleepStateSink = monitor as? SleepStateSink
        monitorBindingJob = applicationScope.launch {
            launch { monitor.devices.collect { _devices.value = it } }
            launch { monitor.sleepState.collect { _sleepState.value = it } }
        }
    }

    fun acceptDisclaimer() {
        _disclaimerAccepted.value = true
        applicationScope.launch {
            repository.setDisclaimerAccepted(true)
        }
    }

    private fun createSessionEngine(): SessionEngine {
        return SessionEngine(
            scope = applicationScope,
            sleepMonitor = sleepMonitor,
            config = _safetyConfig.value,
            createPulsePlayer = { PinkNoisePulsePlayer(it) },
            onNightArchived = { summary ->
                applicationScope.launch {
                    repository.appendNightSummary(summary)
                }
            },
        )
    }

    private fun createMonitor(source: SleepMonitorSource): WearableSleepMonitor {
        return when (source) {
            SleepMonitorSource.FAKE -> FakeSleepMonitor()
            SleepMonitorSource.MIJIA_BRIDGE -> MiJiaSleepBridgeMonitor()
            SleepMonitorSource.XIAOMI_WEAR -> XiaomiWearSleepMonitor(this)
        }
    }
}
