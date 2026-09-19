package com.tidesleep.app.session

import com.tidesleep.app.audio.NoOpPulsePlayer
import com.tidesleep.app.data.NightSummary
import com.tidesleep.app.data.SafetyConfig
import com.tidesleep.app.wearable.FakeSleepMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEngineTest {

    private val demoConfig = SafetyConfig.demo()

    @Test
    fun startTonight_transitionsToWaitingSleep() = runTest {
        withEngine { engine, _ ->
            engine.startTonight()
            advanceUntilIdle()
            assertEquals(SessionPhase.WaitingSleep, engine.snapshot.value.phase)
        }
    }

    @Test
    fun sleepOnset_entersWaitingDelay_thenStimulating() = runTest {
        withEngine { engine, monitor ->
            engine.startTonight()
            advanceUntilIdle()
            monitor.simulateSleepOnset()
            runCurrent()
            assertEquals(SessionPhase.WaitingDelay, engine.snapshot.value.phase)
            advanceTimeBy(demoConfig.postSleepDelay.inWholeMilliseconds + 100)
            runCurrent()
            assertEquals(SessionPhase.Stimulating, engine.snapshot.value.phase)
        }
    }

    @Test
    fun wakeDuringDelay_stopsSession() = runTest {
        withEngine { engine, monitor ->
            engine.startTonight()
            advanceUntilIdle()
            monitor.simulateSleepOnset()
            runCurrent()
            assertEquals(SessionPhase.WaitingDelay, engine.snapshot.value.phase)
            monitor.simulateWake()
            runCurrent()
            assertEquals(SessionPhase.Stopped, engine.snapshot.value.phase)
            assertEquals(StopReason.WakeDetected, engine.snapshot.value.stopReason)
        }
    }

    @Test
    fun stopTonight_isIdempotent() = runTest {
        val archived = mutableListOf<NightSummary>()
        withEngine(onArchive = { archived.add(it) }) { engine, _ ->
            engine.startTonight()
            advanceUntilIdle()
            engine.stopTonight(StopReason.Manual)
            advanceUntilIdle()
            engine.stopTonight(StopReason.Manual)
            advanceUntilIdle()
            assertEquals(1, archived.size)
            assertEquals(SessionPhase.Stopped, engine.snapshot.value.phase)
        }
    }

    @Test
    fun newNight_archivesPreviousSession() = runTest {
        val archived = mutableListOf<NightSummary>()
        withEngine(onArchive = { archived.add(it) }) { engine, _ ->
            engine.startTonight()
            advanceUntilIdle()
            engine.stopTonight(StopReason.Manual)
            advanceUntilIdle()
            engine.startTonight()
            advanceUntilIdle()
            assertTrue(archived.isNotEmpty())
            assertEquals("开启今晚就寝", engine.snapshot.value.timeline.first().label)
        }
    }

    @Test
    fun wakeDuringStimulation_stopsSession() = runTest {
        withEngine { engine, monitor ->
            engine.startTonight()
            advanceUntilIdle()
            monitor.simulateSleepOnset()
            advanceTimeBy(demoConfig.postSleepDelay.inWholeMilliseconds + 100)
            runCurrent()
            assertEquals(SessionPhase.Stimulating, engine.snapshot.value.phase)
            monitor.simulateWake()
            runCurrent()
            assertEquals(SessionPhase.Stopped, engine.snapshot.value.phase)
            assertEquals(StopReason.WakeDetected, engine.snapshot.value.stopReason)
        }
    }

    @Test
    fun timeout_stopsSession() = runTest {
        val shortConfig = demoConfig.copy(
            maxSessionDuration = kotlin.time.Duration.parse("2s"),
            pulseIntervalMeanSeconds = 1,
            pulseIntervalJitterSeconds = 1,
        )
        withEngine(config = shortConfig) { engine, monitor ->
            engine.startTonight()
            advanceUntilIdle()
            monitor.simulateSleepOnset()
            advanceTimeBy(shortConfig.postSleepDelay.inWholeMilliseconds + 100)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(SessionPhase.Stopped, engine.snapshot.value.phase)
            assertEquals(StopReason.Timeout, engine.snapshot.value.stopReason)
        }
    }

    private suspend fun TestScope.withEngine(
        config: SafetyConfig = demoConfig,
        onArchive: (NightSummary) -> Unit = {},
        block: suspend (SessionEngine, FakeSleepMonitor) -> Unit,
    ) {
        val monitor = FakeSleepMonitor()
        val engine = SessionEngine(
            scope = this,
            sleepMonitor = monitor,
            config = config,
            createPulsePlayer = { NoOpPulsePlayer() },
            onNightArchived = onArchive,
        )
        try {
            block(engine, monitor)
        } finally {
            engine.cancelAllJobs()
        }
    }
}
