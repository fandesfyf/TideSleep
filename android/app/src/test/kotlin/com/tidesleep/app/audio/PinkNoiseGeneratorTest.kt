package com.tidesleep.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinkNoiseGeneratorTest {

    @Test
    fun fillBuffer_producesNonSilentSamples() {
        val generator = PinkNoiseGenerator()
        val buffer = ShortArray(1024)
        generator.fillBuffer(buffer)
        val nonZero = buffer.count { it != 0.toShort() }
        assertTrue("Expected pink noise samples", nonZero > buffer.size / 2)
    }

    @Test
    fun generatePulse_hasExpectedLength() {
        val generator = PinkNoiseGenerator()
        val pulse = generator.generatePulse(441)
        assertEquals(441, pulse.size)
    }

    @Test
    fun consecutiveBuffers_areNotIdentical() {
        val generator = PinkNoiseGenerator()
        val first = ShortArray(256)
        val second = ShortArray(256)
        generator.fillBuffer(first)
        generator.fillBuffer(second)
        assertNotEquals(first.toList(), second.toList())
    }
}
