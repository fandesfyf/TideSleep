package com.tidesleep.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidesleep.app.TideSleepApplication
import com.tidesleep.app.audio.PinkNoisePulsePlayer
import com.tidesleep.app.data.NightSummary
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SafetyPreset
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.session.SessionSnapshot
import com.tidesleep.app.session.StopReason
import com.tidesleep.app.session.TideSleepSessionService
import com.tidesleep.app.wearable.WearableDeviceInfo
import com.tidesleep.app.wearable.WearableSleepState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

class TideSleepViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TideSleepApplication

    val sessionSnapshot: StateFlow<SessionSnapshot> = app.sessionEngine.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSnapshot())

    val devices: StateFlow<List<WearableDeviceInfo>> = app.devicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sleepState: StateFlow<WearableSleepState> = app.sleepStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearableSleepState.Unknown)

    val safetyConfig: StateFlow<SafetyConfig> = app.safetyConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SafetyConfig.demo())

    val monitorSource: StateFlow<SleepMonitorSource> = app.monitorSourceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepMonitorSource.FAKE)

    val disclaimerAccepted: StateFlow<Boolean> = app.disclaimerAcceptedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val nightHistory: StateFlow<List<NightSummary>> = app.repository.preferences
        .map { it.nightHistory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startTonight() {
        if (!disclaimerAccepted.value) return
        val context = getApplication<Application>()
        TideSleepSessionService.start(context)
        app.sessionEngine.startTonight()
    }

    fun stopTonight() {
        app.sessionEngine.stopTonight(StopReason.Manual)
        TideSleepSessionService.stop(getApplication())
    }

    fun simulateSleepOnset() {
        app.sleepMonitor.simulateSleepOnset()
    }

    fun simulateWake() {
        app.sleepMonitor.simulateWake()
    }

    fun applyPreset(preset: SafetyPreset) {
        val config = when (preset) {
            SafetyPreset.DEMO -> SafetyConfig.demo()
            SafetyPreset.SCIENTIFIC -> SafetyConfig.scientific()
            SafetyPreset.CUSTOM -> safetyConfig.value
        }
        updateSafetyConfig(config)
    }

    fun updateSafetyConfig(config: SafetyConfig) {
        app.updateSafetyConfig(config)
    }

    fun updateSafetyConfigCustom(
        postSleepDelay: Duration,
        maxSessionDuration: Duration,
        volumePercent: Int,
        pulseIntervalMeanSeconds: Int = safetyConfig.value.pulseIntervalMeanSeconds,
    ) {
        updateSafetyConfig(
            safetyConfig.value.copy(
                preset = SafetyPreset.CUSTOM,
                postSleepDelay = postSleepDelay,
                maxSessionDuration = maxSessionDuration,
                volumePercent = volumePercent,
                pulseIntervalMeanSeconds = pulseIntervalMeanSeconds,
            )
        )
    }

    fun switchMonitorSource(source: SleepMonitorSource) {
        app.switchMonitorSource(source)
    }

    fun acceptDisclaimer() {
        app.acceptDisclaimer()
    }

    fun playCalibrationPulse() {
        viewModelScope.launch {
            val player = PinkNoisePulsePlayer(safetyConfig.value)
            player.playPulse()
            kotlinx.coroutines.delay(200)
            player.release()
        }
    }

    fun isSessionActive(): Boolean {
        val phase = sessionSnapshot.value.phase
        return phase != SessionPhase.Idle && phase != SessionPhase.Stopped
    }
}
