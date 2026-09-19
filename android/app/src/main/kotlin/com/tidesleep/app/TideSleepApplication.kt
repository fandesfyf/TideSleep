package com.tidesleep.app

import android.app.Application
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.data.SleepMonitorSource
import com.tidesleep.app.session.SessionEngine
import com.tidesleep.app.wearable.FakeSleepMonitor
import com.tidesleep.app.wearable.WearableSleepMonitor
import com.tidesleep.app.wearable.XiaomiWearSleepMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TideSleepApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    var safetyConfig: SafetyConfig = SafetyConfig()
        private set

    var monitorSource: SleepMonitorSource = SleepMonitorSource.FAKE
        private set

    lateinit var sleepMonitor: WearableSleepMonitor
        private set

    lateinit var sessionEngine: SessionEngine
        private set

    override fun onCreate() {
        super.onCreate()
        sleepMonitor = createMonitor(monitorSource)
        sessionEngine = SessionEngine(applicationScope, sleepMonitor, safetyConfig)
    }

    fun updateSafetyConfig(config: SafetyConfig) {
        safetyConfig = config
        sessionEngine.updateConfig(config)
    }

    fun switchMonitorSource(source: SleepMonitorSource) {
        if (monitorSource == source) return
        monitorSource = source
        sessionEngine.stopTonight()
        sleepMonitor = createMonitor(source)
        sessionEngine = SessionEngine(applicationScope, sleepMonitor, safetyConfig)
    }

    private fun createMonitor(source: SleepMonitorSource): WearableSleepMonitor {
        return when (source) {
            SleepMonitorSource.FAKE -> FakeSleepMonitor()
            SleepMonitorSource.XIAOMI_WEAR -> XiaomiWearSleepMonitor()
        }
    }
}
