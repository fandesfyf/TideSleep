package com.tidesleep.app

import android.app.Application
import com.tidesleep.app.audio.PinkNoisePulsePlayer
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.data.TideSleepRepository
import com.tidesleep.app.session.SessionEngine
import com.tidesleep.app.wearable.FakeSleepMonitor
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

    private val _devices = MutableStateFlow<List<WearableDeviceInfo>>(emptyList())
    val devicesFlow: StateFlow<List<WearableDeviceInfo>> = _devices.asStateFlow()

    private val _sleepState = MutableStateFlow(WearableSleepState.Unknown)
    val sleepStateFlow: StateFlow<WearableSleepState> = _sleepState.asStateFlow()

    private var monitorBindingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = TideSleepRepository(this)

        val prefs = runBlocking { repository.loadOnce() }
        _safetyConfig.value = prefs.safetyConfig
        _monitorSource.value = prefs.monitorSource
        _disclaimerAccepted.value = prefs.disclaimerAccepted

        sleepMonitor = createMonitor(prefs.monitorSource)
        bindSleepMonitor(sleepMonitor)
        sessionEngine = createSessionEngine()
    }

    fun updateSafetyConfig(config: SafetyConfig) {
        _safetyConfig.value = config
        sessionEngine.updateConfig(config)
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

    private fun bindSleepMonitor(monitor: WearableSleepMonitor) {
        monitorBindingJob?.cancel()
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
            SleepMonitorSource.XIAOMI_WEAR -> XiaomiWearSleepMonitor()
        }
    }
}
