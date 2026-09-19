package com.tidesleep.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidesleep.app.TideSleepApplication
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.session.SessionSnapshot
import com.tidesleep.app.session.StopReason
import com.tidesleep.app.session.TideSleepSessionService
import com.tidesleep.app.wearable.WearableDeviceInfo
import com.tidesleep.app.wearable.WearableSleepState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TideSleepViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TideSleepApplication

    val sessionSnapshot: StateFlow<SessionSnapshot> = app.sessionEngine.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSnapshot())

    val devices: StateFlow<List<WearableDeviceInfo>> = app.sleepMonitor.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sleepState: StateFlow<WearableSleepState> = app.sleepMonitor.sleepState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearableSleepState.Unknown)

    val safetyConfig: SafetyConfig
        get() = app.safetyConfig

    val monitorSource: SleepMonitorSource
        get() = app.monitorSource

    fun startTonight() {
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

    fun updateSafetyConfig(config: SafetyConfig) {
        app.updateSafetyConfig(config)
    }

    fun switchMonitorSource(source: SleepMonitorSource) {
        viewModelScope.launch {
            app.switchMonitorSource(source)
        }
    }

    fun isSessionActive(): Boolean {
        val phase = sessionSnapshot.value.phase
        return phase != SessionPhase.Idle && phase != SessionPhase.Stopped
    }
}
