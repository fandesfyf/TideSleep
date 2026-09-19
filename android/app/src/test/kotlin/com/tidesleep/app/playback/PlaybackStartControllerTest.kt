package com.tidesleep.app.playback

import com.tidesleep.app.data.PlaybackStartMode
import com.tidesleep.app.data.PlaybackStartPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PlaybackStartControllerTest {

    private var now = 1_700_000_000_000L

    private fun controller() = PlaybackStartController(nowMs = { now })

    @Test
    fun immediateStart_setsPlayingAndClearsArmed() {
        val controller = controller()
        controller.startImmediate()

        val state = controller.snapshot()
        assertTrue(state.isPlaying)
        assertNull(state.armed)
        assertNotNull(state.playingStartedAtEpochMs)
    }

    @Test
    fun armCountdown_schedulesTriggerAndTicksDown() {
        val controller = controller()
        controller.setCountdownMinutes(15)

        val result = controller.armCountdown()
        assertTrue(result is PlaybackStartController.ArmResult.Armed)
        val armed = result as PlaybackStartController.ArmResult.Armed
        assertEquals(now + 15 * 60_000L, armed.triggerAtEpochMs)

        now += 30_000L
        controller.onCountdownTick(armed.triggerAtEpochMs - now)
        val countdown = controller.snapshot().armed as PlaybackArmedState.Countdown
        assertEquals(14 * 60_000L + 30_000L, countdown.remainingMs)
    }

    @Test
    fun countdownComplete_startsPlaying() {
        val controller = controller()
        controller.setCountdownMinutes(5)
        controller.armCountdown()

        controller.onCountdownTick(0L)

        val state = controller.snapshot()
        assertTrue(state.isPlaying)
        assertNull(state.armed)
    }

    @Test
    fun armSchedule_usesNextOccurrenceTodayOrTomorrow() {
        val controller = controller()
        controller.setScheduledTime(23, 0)

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val result = controller.armSchedule() as PlaybackStartController.ArmResult.Armed

        val trigger = Calendar.getInstance().apply { timeInMillis = result.triggerAtEpochMs }
        assertEquals(23, trigger.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, trigger.get(Calendar.MINUTE))
        if (currentHour < 23) {
            assertTrue(result.triggerAtEpochMs > now)
        }
    }

    @Test
    fun cancelArmed_clearsArmedState() {
        val controller = controller()
        controller.armCountdown()
        assertTrue(controller.snapshot().isArmed)

        controller.cancelArmed()

        assertFalse(controller.snapshot().isArmed)
    }

    @Test
    fun restoreFromPreferences_rebuildsCountdownArmedState() {
        val controller = controller()
        val triggerAt = now + 10 * 60_000L
        controller.restoreFromPreferences(
            PlaybackStartPreferences(
                mode = PlaybackStartMode.COUNTDOWN,
                countdownMinutes = 10,
                armedTriggerAtEpochMs = triggerAt,
            ),
            isPlaying = false,
        )

        val armed = controller.snapshot().armed as PlaybackArmedState.Countdown
        assertEquals(10 * 60_000L, armed.remainingMs)
        assertEquals(triggerAt, armed.triggerAtEpochMs)
    }

    @Test
    fun computeNextTriggerEpochMs_rollsToTomorrowWhenPast() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 19, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = calendar.timeInMillis
        val trigger = PlaybackStartController.computeNextTriggerEpochMs(22, 0, nowMs)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(20, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(22, triggerCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun toPreferences_persistsArmedTrigger() {
        val controller = controller()
        controller.armSchedule()
        val prefs = controller.toPreferences()
        assertEquals(PlaybackStartMode.SCHEDULED, prefs.mode)
        assertNotNull(prefs.armedTriggerAtEpochMs)
    }

    @Test
    fun setMode_blockedWhileArmedOrPlaying() {
        val controller = controller()
        controller.armCountdown()
        controller.setMode(PlaybackStartMode.IMMEDIATE)
        assertEquals(PlaybackStartMode.COUNTDOWN, controller.snapshot().mode)
    }
}
