package com.tidesleep.app.audio

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 1/f 粉红噪声生成器（Paul Kellet 算法），听起来类似收音机无信号嘶嘶声。
 */
class PinkNoiseGenerator {

    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0

    fun fillBuffer(output: ShortArray, amplitudeScale: Double = 0.35) {
        val amplitude = Short.MAX_VALUE * amplitudeScale
        for (i in output.indices) {
            val white = Random.nextDouble(-1.0, 1.0)
            b0 = 0.99886 * b0 + white * 0.0555179
            b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520
            b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522
            b5 = -0.7616 * b5 - white * 0.0168980
            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
            b6 = white * 0.115926

            val sample = (pink / 5.0 * amplitude).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i] = sample.toShort()
        }
    }

    fun generatePulse(numSamples: Int, amplitudeScale: Double = 0.35): ShortArray {
        val output = ShortArray(numSamples)
        fillBuffer(output, amplitudeScale)
        applyFadeEnvelope(output)
        return output
    }

    private fun applyFadeEnvelope(output: ShortArray) {
        val total = output.size
        val fadeSamples = (total * 0.15).toInt().coerceAtLeast(1)
        for (i in output.indices) {
            val envelope = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > total - fadeSamples -> (total - i).toDouble() / fadeSamples
                else -> 1.0
            }
            val scaled = (output[i] * sqrt(envelope)).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i] = scaled.toShort()
        }
    }
}
