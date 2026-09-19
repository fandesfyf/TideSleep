package com.tidesleep.app.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tidesleep.app.data.SafetyConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContinuousPinkNoisePlayerTest {

    private lateinit var context: Context
    private lateinit var scope: TestScope
    private lateinit var player: ContinuousPinkNoisePlayer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scope = TestScope()
        player = ContinuousPinkNoisePlayer(context, scope)
    }

    @After
    fun tearDown() {
        player.stop()
    }

    @Test
    fun start_setsPlayingTrue() = runTest {
        player.start(SafetyConfig.demo())
        advanceUntilIdle()
        assertTrue(player.playing)
    }

    @Test
    fun stop_setsPlayingFalse() = runTest {
        player.start(SafetyConfig.demo())
        advanceUntilIdle()
        player.stop()
        assertFalse(player.playing)
    }

    @Test
    fun startWhilePlaying_isIdempotent() = runTest {
        player.start(SafetyConfig.demo())
        advanceUntilIdle()
        player.start(SafetyConfig.demo())
        advanceUntilIdle()
        assertTrue(player.playing)
        player.stop()
        assertFalse(player.playing)
    }

    @Test
    fun updateVolume_whilePlaying() = runTest {
        player.start(SafetyConfig.demo())
        advanceUntilIdle()
        player.updateVolume(15)
        assertTrue(player.playing)
        player.stop()
    }
}
